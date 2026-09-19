#!/bin/bash
# Rebuild, install and (re-)enable the Edith Clock Fraunces plugin.
#
# If the clock ever disappears from the ThemePicker after a crash, SystemUI
# auto-disables the plugin component (PluginEnablerImpl). This script
# re-enables it so the clock shows up again.
set -euo pipefail

PKG="com.android.systemui.clocks.edithclockfraunces"
COMPONENT="$PKG/com.android.systemui.clocks.edithclockfraunces.EdithClockFrauncesProvider"
APK="/system_ext/priv-app/SystemUIClocks-EdithClockFraunces/SystemUIClocks-EdithClockFraunces.apk"

m SystemUIClocks-EdithClockFraunces

adb root
adb remount
adb sync

# Reinstall the apk (uninstall first so component state is reset).
adb shell pm uninstall --user 0 "$PKG" || true
adb shell pm install "$APK"

# Clear any auto-disabled state and re-enable the provider component.
adb shell pm enable "$COMPONENT"

if [[ "${1:-}" == "-r" ]]; then
    adb reboot
else
    adb shell stop
    adb shell start
fi

echo "Installed and enabled $COMPONENT"
