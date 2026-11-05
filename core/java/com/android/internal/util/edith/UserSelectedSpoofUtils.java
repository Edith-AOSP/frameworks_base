/*
 * Copyright (C) 2021-2025 Halcyon Project
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
package com.android.internal.util.edith;

import android.content.ContentResolver;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility to manage user-selected app spoofing.
 */
public class UserSelectedSpoofUtils {

    private static final String ENTRY_SEPARATOR = ",";
    private static final String VALUE_SEPARATOR = ":";

    /**
     * Return true if this app should be spoofed dynamically by the user.
     */
    public static boolean shouldSpoofApp(Context context, String packageName) {
        if (context == null || packageName == null) return false;
        Set<String> apps = getUserSelectedApps(context);
        return apps.contains(packageName);
    }

    /**
     * Return the spoof profile for the given app if it exists.
     * Example: "PixelXL", "ROG6", etc.
     */
    public static String getSpoofProfile(Context context, String packageName) {
        if (context == null || packageName == null) return null;
        Map<String, String> map = getUserSelectedAppsMap(context);
        return map.get(packageName);
    }

    /**
     * Get the full map of user-selected apps to their spoof profile.
     */
    public static Map<String, String> getUserSelectedAppsMap(Context context) {
        return getUserSelectedAppsMap(context, UserHandle.myUserId());
    }

    private static Map<String, String> getUserSelectedAppsMap(Context context, int userId) {
        Map<String, String> map = new HashMap<>();
        if (context == null || userId < 0) return map;

        ContentResolver cr = context.getContentResolver();
        String raw = Settings.Secure.getStringForUser(cr, Settings.Secure.EDITH_SPOOFED_APPS, userId);
        if (raw == null || raw.isEmpty()) return map;

        for (String entry : raw.split(ENTRY_SEPARATOR)) {
            int separator = entry.indexOf(VALUE_SEPARATOR);
            if (separator > 0 && separator < entry.length() - 1) {
                map.put(entry.substring(0, separator), entry.substring(separator + 1));
            }
        }

        return map;
    }

    /**
     * Return the set of all user-selected packages.
     */
    public static Set<String> getUserSelectedApps(Context context) {
        return getUserSelectedAppsMap(context).keySet();
    }

    private enum Action {
        ADD, REMOVE, SET
    }

    private static void putAppForUser(Context context, String packageName, String profile, int userId, Action action) {
        if (context == null || userId < 0) return;

        Map<String, String> apps = getUserSelectedAppsMap(context, userId);

        switch (action) {
            case ADD:
                if (packageName != null && profile != null) {
                    apps.put(packageName, profile);
                }
                break;
            case REMOVE:
                if (packageName != null) {
                    apps.remove(packageName);
                }
                break;
            case SET:
                apps.clear();
                break;
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : apps.entrySet()) {
            if (sb.length() > 0) sb.append(ENTRY_SEPARATOR);
            sb.append(entry.getKey()).append(VALUE_SEPARATOR).append(entry.getValue());
        }
        Settings.Secure.putStringForUser(context.getContentResolver(),
                Settings.Secure.EDITH_SPOOFED_APPS, sb.toString(), userId);
    }

    /** Add an app to user-selected spoof list */
    public static void addApp(Context context, String packageName, String profile, int userId) {
        putAppForUser(context, packageName, profile, userId, Action.ADD);
    }

    /** Remove an app from user-selected spoof list */
    public static void removeApp(Context context, String packageName, int userId) {
        putAppForUser(context, packageName, null, userId, Action.REMOVE);
    }

    /** Reset all user-selected spoof apps for a user */
    public static void setApps(Context context, int userId) {
        putAppForUser(context, null, null, userId, Action.SET);
    }
}