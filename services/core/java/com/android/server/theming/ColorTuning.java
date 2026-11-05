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

package com.android.server.theming;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Relative intensity adjustments for generated color palettes.
 *
 * <p>Values are percentages where {@link #NEUTRAL} leaves the palette unchanged. They are stored
 * alongside the other theme settings so the theme service can apply them whenever it regenerates
 * an overlay.
 *
 * @hide
 */
record ColorTuning(int accentSaturation, int backgroundSaturation, int backgroundLightness) {

    static final int NEUTRAL = 100;
    static final int MIN = 0;
    static final int MAX = 200;

    static final String KEY_ACCENT_SATURATION =
            ThemeSettingsManager.KEY_PREFIX + "accent_saturation";
    static final String KEY_BACKGROUND_SATURATION =
            ThemeSettingsManager.KEY_PREFIX + "background_saturation";
    static final String KEY_BACKGROUND_LIGHTNESS =
            ThemeSettingsManager.KEY_PREFIX + "background_lightness";

    static final ColorTuning DEFAULT = new ColorTuning(NEUTRAL, NEUTRAL, NEUTRAL);

    @NonNull
    static ColorTuning fromJson(@Nullable String jsonString) {
        if (TextUtils.isEmpty(jsonString)) {
            return DEFAULT;
        }
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException e) {
            return DEFAULT;
        }
    }

    @NonNull
    static ColorTuning fromJson(@NonNull JSONObject json) {
        return new ColorTuning(
                clamp(json.optInt(KEY_ACCENT_SATURATION, NEUTRAL)),
                clamp(json.optInt(KEY_BACKGROUND_SATURATION, NEUTRAL)),
                clamp(json.optInt(KEY_BACKGROUND_LIGHTNESS, NEUTRAL)));
    }

    static void putInto(@NonNull JSONObject json, @NonNull ColorTuning tuning) throws JSONException {
        json.put(KEY_ACCENT_SATURATION, clamp(tuning.accentSaturation()));
        json.put(KEY_BACKGROUND_SATURATION, clamp(tuning.backgroundSaturation()));
        json.put(KEY_BACKGROUND_LIGHTNESS, clamp(tuning.backgroundLightness()));
    }

    private static int clamp(int value) {
        return Math.max(MIN, Math.min(MAX, value));
    }

    boolean isNeutral() {
        return accentSaturation == NEUTRAL
                && backgroundSaturation == NEUTRAL
                && backgroundLightness == NEUTRAL;
    }
}
