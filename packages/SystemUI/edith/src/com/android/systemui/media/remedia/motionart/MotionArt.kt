/*
 * Copyright (C) 2026 The PenguinOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.media.remedia.motionart

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Dynamic ("motion") album art: the short looped video Apple Music ships for an album, used by the
 * expanded lock screen media player.
 *
 * The video is resolved from the public iTunes search + album page, so no account, token or extra
 * app is involved: search by artist/title, walk the matching album pages (a song is often listed as
 * both a single and an album track, and only one of them may be animated), pull the `motionDetail*`
 * HLS master out of the page's structured data, pick the AVC variant closest to the width we draw
 * at, and cache the file the playlist's `#EXT-X-MAP` points at as a local MP4. The loop is that
 * whole file — the segments after the map are byteranges into it — so the `BYTERANGE` on the map is
 * not used, and the media playlist is only read for the URI.
 *
 * What is cached is the segment with its timeline moved to zero by [MotionArtTimeline], so the
 * player loops a file whose first sample sits at zero rather than wherever the album's playlist
 * happened to start. The samples themselves are left untouched.
 *
 * Everything is best-effort and cached on disk: [find] returns `null` when the track has no motion
 * art, the network is down or anything is unexpected, and the caller just keeps the static cover.
 */
object MotionArt {
    /** Which cover shape to fetch. The tall one is only used by the backdrop. */
    enum class Kind(val key: String, val targetWidth: Int, val suffix: String) {
        SQUARE("motionDetailSquare", 768, ""),
        TALL("motionDetailTall", 830, "-tall"),
    }

    private const val TAG = "LockscreenMotionArt"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/138.0 Mobile Safari/537.36"
    private const val MAX_BYTES = 24L * 1024 * 1024
    private const val MAX_CACHED = 40

    /** How many matching album pages to try before giving up on a track. */
    private const val MAX_ALBUM_CANDIDATES = 4

    /** Bumped when the cached file changes shape, so older files are not played as they are. */
    private const val CACHE_VERSION = 3

    /** Remembered album id (or [NONE]) per query, so a miss is not re-searched every track change. */
    private val albums = ConcurrentHashMap<String, String>()
    private const val NONE = ""

    suspend fun find(context: Context, artist: String, title: String, kind: Kind): File? =
        withContext(Dispatchers.IO) {
            if (artist.isBlank() || title.isBlank()) return@withContext null
            val dir = cacheDir(context)
            val key = "$artist\u0000$title\u0000$kind"
            val known = albums[key]
            if (known != null) {
                return@withContext known
                    .takeIf { it != NONE }
                    ?.let { cacheFile(dir, it, kind) }
                    ?.takeIf { it.exists() }
            }
            try {
                val candidates = findAlbums(artist, title)
                for ((id, page) in candidates) {
                    val cached = cacheFile(dir, id, kind)
                    if (!cached.exists() && !download(page, kind, cached)) continue
                    albums[key] = id
                    cached.setLastModified(System.currentTimeMillis())
                    trim(dir)
                    return@withContext cached
                }
                albums[key] = NONE
                return@withContext null
            } catch (e: IOException) {
                Log.w(TAG, "Motion art lookup failed: $e")
                null
            }
        }

    /**
     * Albums matching [artist] and [title], in search order.
     *
     * A song is often listed more than once — its single and the album it appears on — and only one
     * of them may carry motion art, so the caller tries each page instead of trusting the first hit.
     */
    private fun findAlbums(artist: String, title: String): List<Pair<String, String>> {
        val country = Locale.getDefault().country.lowercase().ifEmpty { "us" }
        val term = URLEncoder.encode("$artist $title", "UTF-8")
        val json =
            JSONObject(
                fetch(
                    "https://itunes.apple.com/search?term=$term&entity=song&limit=10" +
                        "&country=$country"
                )
            )
        val results = json.optJSONArray("results") ?: return emptyList()
        val wantedArtist = normalise(artist)
        val wantedTitle = normalise(title)
        val seen = HashSet<Long>()
        val candidates = ArrayList<Pair<String, String>>()
        for (i in 0 until results.length()) {
            val song = results.getJSONObject(i)
            val songArtist = normalise(song.optString("artistName"))
            val songTitle = normalise(song.optString("trackName"))
            val artistMatches =
                songArtist.contains(wantedArtist) || wantedArtist.contains(songArtist)
            val titleMatches = songTitle.startsWith(wantedTitle) || wantedTitle.startsWith(songTitle)
            if (!artistMatches || !titleMatches) continue
            val id = song.optLong("collectionId").takeIf { it > 0 } ?: continue
            if (!seen.add(id)) continue
            val page = song.optString("collectionViewUrl").substringBefore('?')
            if (page.isEmpty()) continue
            candidates += id.toString() to page
            if (candidates.size == MAX_ALBUM_CANDIDATES) break
        }
        return candidates
    }

    private fun normalise(text: String) =
        text.lowercase().replace(Regex("\\(.*?\\)|\\[.*?]"), "").replace(Regex("[^\\p{L}\\p{N}]"), "")

    private fun download(page: String, kind: Kind, target: File): Boolean {
        val html = fetch(page)
        val start = html.indexOf("\"${kind.key}\"")
        if (start < 0) return false
        val video =
            Regex("\"video\":\"(https://[^\"]+?\\.m3u8)\"")
                .find(html.substring(start, minOf(html.length, start + 4000)))
                ?.groupValues
                ?.get(1) ?: return false

        val master = fetch(video).lines()
        var best: String? = null
        var bestDistance = Int.MAX_VALUE
        for (i in master.indices) {
            val line = master[i]
            if (!line.startsWith("#EXT-X-STREAM-INF") || !line.contains("avc1")) continue
            val width =
                Regex("RESOLUTION=(\\d+)x").find(line)?.groupValues?.get(1)?.toInt() ?: continue
            val uri = master.getOrNull(i + 1)?.trim().orEmpty()
            if (uri.isEmpty() || uri.startsWith("#")) continue
            val distance = abs(width - kind.targetWidth)
            if (distance < bestDistance) {
                best = URL(URL(video), uri).toString()
                bestDistance = distance
            }
        }
        best ?: return false

        val map =
            Regex("#EXT-X-MAP:URI=\"([^\"]+)\"").find(fetch(best))?.groupValues?.get(1)
                ?: return false
        target.parentFile?.mkdirs()
        val partial = File(target.path + ".part")
        val connection = open(URL(URL(best), map).toString())
        try {
            if (connection.contentLengthLong > MAX_BYTES) return false
            connection.inputStream.use { input ->
                partial.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
        if (partial.length() !in 1..MAX_BYTES) {
            partial.delete()
            return false
        }
        val rewritten = File(target.path + ".rewrite")
        if (MotionArtTimeline.rewrite(partial, rewritten) && rewritten.renameTo(target)) {
            partial.delete()
            return true
        }
        rewritten.delete()
        // A segment this cannot parse may still play as it is, so it is kept rather than dropping
        // the art for this track.
        return partial.renameTo(target)
    }

    private fun cacheFile(dir: File, albumId: String, kind: Kind) =
        File(dir, "$albumId${kind.suffix}.v$CACHE_VERSION.mp4")

    private fun trim(dir: File) {
        val suffix = ".v$CACHE_VERSION.mp4"
        dir.listFiles { file -> file.name.endsWith(".mp4") }.orEmpty().forEach { file ->
            if (!file.name.endsWith(suffix)) file.delete()
        }
        val files = dir.listFiles { file -> file.name.endsWith(suffix) } ?: return
        files.sortedByDescending { it.lastModified() }.drop(MAX_CACHED).forEach { it.delete() }
    }

    /** Directory holding the cached loops. Shared with the cache provider used by Settings. */
    fun cacheDir(context: Context): File =
        File(context.createDeviceProtectedStorageContext().cacheDir, "motion_art")

    /** Total size of the cache, including any download that is still in progress. */
    fun cacheBytes(context: Context): Long =
        cacheDir(context).listFiles()?.sumOf { it.length() } ?: 0L

    /** Number of loops currently cached. */
    fun cacheFiles(context: Context): Int =
        cacheDir(context).listFiles()?.count { it.name.endsWith(".mp4") } ?: 0

    /**
     * Deletes every cached loop. The remembered albums are dropped with them: their files are gone,
     * so a later lookup has to search and download again instead of resolving to a missing file.
     */
    fun clearCache(context: Context) {
        cacheDir(context).listFiles()?.forEach { it.delete() }
        albums.clear()
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", USER_AGENT)
        }

    private fun fetch(url: String): String {
        val connection = open(url)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${connection.responseCode} for $url")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
