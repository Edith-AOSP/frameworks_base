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

package com.android.systemui.keyguard.ui.composable.elements

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.android.systemui.media.remedia.ui.compose.Media
import com.android.systemui.qs.edith.ChargingChipStatus
import com.android.systemui.qs.edith.ChargingTier
import com.android.systemui.res.R

/**
 * The lock screen charging chip, shown beside the notification chip while the expanded media player
 * is up and a charger is connected.
 *
 * The wording is the lock screen's existing charging copy: the state on the first line (for example
 * "Charging rapidly") and the percentage with the time to full underneath. When the state needs the
 * whole chip, the numbers are dropped rather than ellipsised — the badge and the state are the
 * point of the chip.
 *
 * @param expanded whether the text has grown out of the compact badge-only form.
 * @param besideNotification whether the compact notification pill shares the row, in which case
 *   its width and the gap are kept free so neither pill is squeezed.
 * @param connected how joined this chip is to the notification chip beside it, `0f` = apart, `1f` =
 *   the facing corners squared off and the gap closed.
 * @param contentAlpha opacity of the badge and text only, so a close can fade them out while the
 *   pill's own background is still folding away.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LockscreenChargingChip(
    status: ChargingChipStatus,
    containerColor: Color,
    contentColor: Color,
    expanded: Boolean,
    besideNotification: Boolean,
    connected: Float,
    dimAmount: Float = 0f,
    rotationEnabled: Boolean = true,
    contentAlpha: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val pillRadius = ChipHeight / 2
    // The chip sits before the notification chip in the row, so its facing side is always its end
    // side — in RTL the row order and the start/end corners both mirror together.
    val facingRadius = lerp(pillRadius, JoinedRadius, connected.coerceIn(0f, 1f))
    val shape =
        RoundedCornerShape(
            topStart = pillRadius,
            bottomStart = pillRadius,
            topEnd = facingRadius,
            bottomEnd = facingRadius,
        )

    val titleStyle = MaterialTheme.typography.labelLarge
    val titleBudget = titleBudget(besideNotification)
    // The numbers are a bonus; the state text is the point of the chip. Drop them when the state
    // would need the room, and never let either line push the pill past the row.
    val showSubtitle = expanded && titleFits(status.stateText, titleStyle, titleBudget)
    val subtitle =
        status.timeRemaining?.let {
            stringResource(R.string.edith_charging_chip_subtitle, status.percentage, it)
        } ?: status.percentage

    val description = buildString {
        append(status.stateText)
        append(", ")
        append(status.percentage)
        status.timeRemaining?.let {
            append(", ")
            append(it)
        }
    }

    Row(
        modifier =
            modifier
                .requiredHeight(ChipHeight)
                .clip(shape)
                .background(containerColor)
                // The background stays fully opaque; only the badge and text fade, so the pill
                // itself can fold away while its contents have already gone.
                .graphicsLayer { alpha = contentAlpha.coerceIn(0f, 1f) }
                // Inside the clip and the background, so the pill itself grows with the animation
                // instead of snapping to the new width while only the space around it moves.
                .animateContentSize(ContentSizeSpring)
                .padding(
                    start = ChipStartPadding,
                    end = if (expanded) ChipEndPadding else ChipStartPadding,
                )
                .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChargingBadge(status.tier, dimAmount, rotationEnabled)

        if (expanded) {
            Spacer(Modifier.width(BadgeGap))
            Column(Modifier.widthIn(max = titleBudget)) {
                Text(
                    text = status.stateText,
                    color = contentColor,
                    style = titleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showSubtitle) {
                    Text(
                        text = subtitle,
                        color = contentColor.copy(alpha = SubtitleAlpha),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * The M3 cookie badge. It spins once when the chip opens, then holds still.
 *
 * While it spins, a face says how the charge is going — a smile when it is fast, a frown when it is
 * slow or limited. When the spin finishes the face animates out and the condition glyph takes its
 * place: a full battery when charged, a battery alert when the defender is holding the charge, and
 * the bolt otherwise. The badge only "acts" for the opening beat.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChargingBadge(
    tier: ChargingTier,
    dimAmount: Float,
    rotationEnabled: Boolean,
) {
    val cookie = MaterialShapes.Cookie12Sided.toShape()
    val dim = dimAmount.coerceIn(0f, 1f)
    val badgeColor = lerpColor(MaterialTheme.colorScheme.primary, Color.Black, dim)
    val badgeContentColor = lerpColor(MaterialTheme.colorScheme.onPrimary, Color.Black, dim)

    val spin = remember { Animatable(0f) }
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(rotationEnabled) {
        if (!rotationEnabled) {
            // Entering AOD: stop the opening spin too and settle straight on the steady glyph.
            triggered = true
            return@LaunchedEffect
        }
        if (triggered) return@LaunchedEffect
        // Let the chip become visible before the spin starts; in the pop entrance the badge
        // spends its first frames behind the notification pill.
        kotlinx.coroutines.delay(BADGE_SPIN_DELAY_MS)
        spin.animateTo(360f, tween(BADGE_SPIN_MS, easing = FastOutSlowInEasing))
        triggered = true
    }
    // After the opening spin the cookie keeps turning only while the battery is actually
    // charging and the screen is awake; AOD must not hold a permanent animation.
    LaunchedEffect(tier, triggered, rotationEnabled) {
        if (!rotationEnabled || !triggered || !tier.loopsWhileCharging) return@LaunchedEffect
        val period = tier.loopPeriodMillis()
        while (true) {
            spin.animateTo(
                targetValue = spin.value + 360f,
                animationSpec = tween(period, easing = LinearEasing),
            )
        }
    }

    val triggerGlyph =
        when (tier) {
            ChargingTier.FAST -> BadgeGlyph.SMILE
            ChargingTier.SLOWLY,
            ChargingTier.INCOMPATIBLE,
            ChargingTier.ON_HOLD -> BadgeGlyph.SAD
            ChargingTier.REGULAR,
            ChargingTier.CHARGED -> BadgeGlyph.BOLT
        }
    // Once the opening spin is done the badge settles on the glyph for the condition: a full
    // battery when charged, an alert when the defender is holding the charge, otherwise the bolt.
    val steady =
        when (tier) {
            ChargingTier.CHARGED -> BadgeGlyph.BATTERY_FULL
            ChargingTier.ON_HOLD -> BadgeGlyph.BATTERY_FRAME_ALERT
            else -> BadgeGlyph.BOLT
        }

    Box(Modifier.size(BadgeSize), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { rotationZ = spin.value }
                .background(badgeColor, cookie)
        )
        AnimatedContent(
            targetState = if (triggered) steady else triggerGlyph,
            // Deliberately not sized here: a fixed 18dp modifier passes min = 18dp down to the
            // content, which forced the smaller face back to the full icon size and made every
            // inset change a no-op. The badge Box centres whatever the glyph measures.
            transitionSpec = {
                (fadeIn(tween(160)) + scaleIn(tween(180), initialScale = 0.72f))
                    .togetherWith(
                        fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.72f)
                    )
                    // The face and the line glyphs measure differently; never clip the swap.
                    .using(SizeTransform(clip = false))
            },
            contentAlignment = Alignment.Center,
            label = "chargingBadgeIcon",
        ) { glyph ->
            BadgeGlyphIcon(glyph, badgeContentColor)
        }
    }
}

/** What the badge is showing: a face during the opening spin, the condition glyph afterwards. */
private enum class BadgeGlyph {
    SMILE,
    SAD,
    BOLT,
    BATTERY_FULL,
    BATTERY_FRAME_ALERT,
}

@Composable
private fun BadgeGlyphIcon(glyph: BadgeGlyph, tint: Color) {
    when (glyph) {
        BadgeGlyph.SMILE ->
            Icon(
                painter = painterResource(R.drawable.edith_charging_face_smile),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(FaceIconSize),
            )
        BadgeGlyph.SAD ->
            Icon(
                painter = painterResource(R.drawable.edith_charging_face_sad),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(FaceIconSize),
            )
        BadgeGlyph.BOLT ->
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(BadgeIconSize),
            )
        BadgeGlyph.BATTERY_FULL ->
            Icon(
                imageVector = BatteryAndroidFull,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(BadgeIconSize),
            )
        BadgeGlyph.BATTERY_FRAME_ALERT ->
            Icon(
                imageVector = BatteryAndroidFrameAlert,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(BadgeIconSize),
            )
    }
}

/**
 * The width the state text may use on one line. When the notification pill is beside this chip,
 * its compact width and the gap are kept free; when the charging chip is alone, the whole row is
 * available, so the numbers stay for states that would otherwise not fit.
 *
 * The budget is derived from the screen width rather than measured from the row, because the
 * charging chip has already been laid out before the notification chip beside it is known, and a
 * feedback loop would show the numbers for a frame before hiding them.
 */
@Composable
private fun titleBudget(besideNotification: Boolean): Dp {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val reserved = if (besideNotification) NotificationChipWidth + ChipGap else 0.dp
    return (screenWidth -
            ScreenSidePadding * 2 -
            reserved -
            BadgeSize -
            BadgeGap -
            ChipStartPadding -
            ChipEndPadding)
        .coerceAtLeast(0.dp)
}

/** Whether the state text fits on one line in [budget]; if not, the numbers are dropped. */
@Composable
private fun titleFits(title: String, style: TextStyle, budget: Dp): Boolean {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val titleWidth =
        remember(title, style) { textMeasurer.measure(title, style, maxLines = 1).size.width }
    return titleWidth <= with(density) { budget.toPx() }
}

/** Same height as the notification chip and the quick affordances either side of the row. */
private val ChipHeight = 48.dp

private val BadgeSize = 32.dp
private val BadgeIconSize = 18.dp

/**
 * Material glyphs inset their artwork by 2dp on a 24dp grid. The smile and frown SVGs are
 * edge-to-edge and optically heavier than a line glyph, so even the matching 20/24 live area still
 * read larger than the bolt. Shrink them inside the same 18dp slot instead of growing the slot.
 */
private val FaceIconSize = BadgeIconSize * (2f / 3f)
private val BadgeGap = 10.dp

private val ChipStartPadding = 8.dp
private val ChipEndPadding = 14.dp

/** Facing corner radius while the two chips are joined, M3's "small" corner. */
private val JoinedRadius = 8.dp

/**
 * Width of the pill while it shows only the badge. The lower region grows the pill out of zero
 * width on first entrance, so it needs the target width rather than a measured one.
 */
internal val ChargingChipCompactWidth = ChipStartPadding * 2 + BadgeSize

/** The row's side inset, matching `keyguard_affordance_horizontal_offset`. */
private val ScreenSidePadding = 16.dp

/**
 * A compact notification chip: the first icon, the count and the two paddings. Used only to decide
 * how much width the charging chip's state text can have.
 */
private val NotificationChipWidth = 80.dp

private const val SubtitleAlpha = 0.7f

/** One turn when the badge opens; a single deliberate spin, not a spinner. */
private const val BADGE_SPIN_MS = 780

/** Let the chip get out from behind the notification pill before the spin starts. */
private const val BADGE_SPIN_DELAY_MS = 120L

/** The continuing turn while charging: fast fills spin quicker than slow ones. */
private const val BADGE_LOOP_FAST_MS = 3_000
private const val BADGE_LOOP_REGULAR_MS = 6_000
private const val BADGE_LOOP_SLOW_MS = 12_000

/** Whether the battery is actively filling, so the badge should keep turning. */
private val ChargingTier.loopsWhileCharging: Boolean
    get() =
        when (this) {
            ChargingTier.FAST,
            ChargingTier.REGULAR,
            ChargingTier.SLOWLY -> true
            ChargingTier.CHARGED,
            ChargingTier.ON_HOLD,
            ChargingTier.INCOMPATIBLE -> false
        }

private fun ChargingTier.loopPeriodMillis(): Int =
    when (this) {
        ChargingTier.FAST -> BADGE_LOOP_FAST_MS
        ChargingTier.SLOWLY -> BADGE_LOOP_SLOW_MS
        else -> BADGE_LOOP_REGULAR_MS
    }

private val ContentSizeSpring =
    spring<IntSize>(
        dampingRatio = Media.MORPH_SPRING_DAMPING,
        stiffness = Media.MORPH_SPRING_STIFFNESS,
    )
