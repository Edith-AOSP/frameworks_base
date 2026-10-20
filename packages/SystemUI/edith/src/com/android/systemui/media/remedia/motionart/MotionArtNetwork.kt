/*
 * Copyright (C) 2026 The Android Open Source Project
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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Tracks whether the default network is unmetered, for the "Wi-Fi only" motion art mode.
 *
 * Unmetered covers Wi-Fi and Ethernet; a metered connection is treated the same whether it is
 * cellular or a metered hotspot. Only fetching is gated by this — art that is already cached keeps
 * playing on any network.
 */
@SysUISingleton
class MotionArtNetwork @Inject constructor(@Application private val context: Context) {

    val isUnmetered: Flow<Boolean> =
        callbackFlow {
                val manager = context.getSystemService(ConnectivityManager::class.java)
                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onCapabilitiesChanged(
                            network: Network,
                            capabilities: NetworkCapabilities,
                        ) {
                            trySend(capabilities.isUnmetered())
                        }

                        override fun onLost(network: Network) {
                            trySend(manager?.let { unmetered(it) } ?: false)
                        }
                    }
                trySend(manager?.let { unmetered(it) } ?: false)
                manager?.registerDefaultNetworkCallback(callback)
                awaitClose { manager?.unregisterNetworkCallback(callback) }
            }
            .distinctUntilChanged()

    private fun unmetered(manager: ConnectivityManager): Boolean =
        manager.activeNetwork?.let { manager.isUnmetered(it) } ?: false

    private fun ConnectivityManager.isUnmetered(network: Network): Boolean {
        val capabilities = getNetworkCapabilities(network) ?: return false
        return capabilities.isUnmetered()
    }

    private fun NetworkCapabilities.isUnmetered(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
