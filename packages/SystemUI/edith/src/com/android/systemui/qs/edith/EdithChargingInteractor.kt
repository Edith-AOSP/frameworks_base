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

package com.android.systemui.qs.edith

import android.content.Context
import android.os.RemoteException
import android.text.format.Formatter
import com.android.internal.app.IBatteryStats
import com.android.keyguard.KeyguardUpdateMonitor
import com.android.keyguard.KeyguardUpdateMonitorCallback
import com.android.settingslib.fuelgauge.BatteryStatus
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.res.R
import java.text.NumberFormat
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * The charging state the lock screen chip shows.
 *
 * The wording is not new copy: the chip shows the same state the lock screen indication already
 * uses for the same battery state, only split into a title (the state) and a subtitle (the
 * percentage and, when known, the time to full). The state is picked from the same stock resources
 * and in the same order as `KeyguardIndicationController.computePowerIndication()`, so the chip and
 * the indication area can never disagree about how charging is described.
 */
@SysUISingleton
class EdithChargingInteractor
@Inject
constructor(
    @Application private val context: Context,
    private val keyguardUpdateMonitor: KeyguardUpdateMonitor,
    private val batteryStats: IBatteryStats,
) {

    /**
     * The current charging state, `null` while the device is not plugged in.
     *
     * Registered while collected, so the `KeyguardUpdateMonitor` callback only exists while the
     * lock screen chip can be shown.
     */
    val status: Flow<ChargingChipStatus?> =
        callbackFlow {
                val callback =
                    object : KeyguardUpdateMonitorCallback() {
                        override fun onRefreshBatteryInfo(status: BatteryStatus) {
                            trySend(status.toChipStatus())
                        }
                    }
                keyguardUpdateMonitor.registerCallback(callback)
                awaitClose { keyguardUpdateMonitor.removeCallback(callback) }
            }
            .flowOn(Dispatchers.Main.immediate)

    private fun BatteryStatus.toChipStatus(): ChargingChipStatus? {
        if (!isPluggedIn()) {
            return null
        }

        // Same precedence as KeyguardIndicationController.computePowerIndication(): the defender
        // and the incompatible accessory are more important than how fast the battery is filling.
        val tier =
            when {
                isBatteryDefender() -> ChargingTier.ON_HOLD
                incompatibleCharger.orElse(false) -> ChargingTier.INCOMPATIBLE
                isCharged() -> ChargingTier.CHARGED
                isDead() -> ChargingTier.REGULAR
                isPluggedInWireless() || isPluggedInDock() -> ChargingTier.REGULAR
                else ->
                    when (getChargingSpeed(context)) {
                        BatteryStatus.CHARGING_FAST -> ChargingTier.FAST
                        BatteryStatus.CHARGING_SLOWLY -> ChargingTier.SLOWLY
                        else -> ChargingTier.REGULAR
                    }
            }

        return ChargingChipStatus(
            stateText = stateTextFor(tier, this),
            percentage = NumberFormat.getPercentInstance().format(level / 100f),
            timeRemaining = if (tier == ChargingTier.CHARGED) null else chargeTimeRemaining(),
            tier = tier,
        )
    }

    private fun stateTextFor(tier: ChargingTier, status: BatteryStatus): String =
        when (tier) {
            ChargingTier.CHARGED -> context.getString(R.string.keyguard_charged)
            ChargingTier.ON_HOLD ->
                context.stateTextOf(R.string.keyguard_plugged_in_charging_limited)
            ChargingTier.INCOMPATIBLE ->
                context.stateTextOf(R.string.keyguard_plugged_in_incompatible_charger)
            ChargingTier.FAST -> context.stateTextOf(R.string.keyguard_plugged_in_charging_fast)
            ChargingTier.SLOWLY -> context.stateTextOf(R.string.keyguard_plugged_in_charging_slowly)
            ChargingTier.REGULAR ->
                when {
                    status.isPluggedInWireless() ->
                        context.stateTextOf(R.string.keyguard_plugged_in_wireless)
                    status.isPluggedInDock() ->
                        context.stateTextOf(R.string.keyguard_plugged_in_dock)
                    else -> context.stateTextOf(R.string.keyguard_plugged_in)
                }
        }

    private fun chargeTimeRemaining(): String? {
        val millis =
            try {
                batteryStats.computeChargeTimeRemaining()
            } catch (e: RemoteException) {
                return null
            }
        return if (millis > 0) {
            Formatter.formatShortElapsedTimeRoundingUpToMinutes(context, millis)
        } else {
            null
        }
    }

    /**
     * The stock charging strings are `"%s • Charging rapidly"`: the percentage is part of the same
     * string as the state. The chip shows the state on its own line and the percentage underneath,
     * so format the string with a sentinel and remove exactly what we inserted. Unlike splitting on
     * the bullet, that also works for locales that put the percentage after the state.
     */
    private fun Context.stateTextOf(resId: Int): String =
        getString(resId, PERCENTAGE_SENTINEL).replace(PERCENTAGE_SENTINEL, "").trim { char ->
            char.isWhitespace() ||
                char == '•' ||
                char == '·' ||
                char == ':' ||
                char == '：' ||
                char == ',' ||
                char == '，'
        }

    private companion object {
        /** Never part of a translated string, so removing it can only remove what we inserted. */
        const val PERCENTAGE_SENTINEL = "\u0000"
    }
}

/** What the charging chip draws: the state wording, the numbers and the badge icon tier. */
data class ChargingChipStatus(
    val stateText: String,
    val percentage: String,
    val timeRemaining: String?,
    val tier: ChargingTier,
)

/** The battery states that get a distinct badge icon. */
enum class ChargingTier {
    FAST,
    REGULAR,
    SLOWLY,
    CHARGED,
    ON_HOLD,
    INCOMPATIBLE,
}
