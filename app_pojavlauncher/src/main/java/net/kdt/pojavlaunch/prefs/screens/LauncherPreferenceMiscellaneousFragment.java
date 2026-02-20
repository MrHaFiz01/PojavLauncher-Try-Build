package net.kdt.pojavlaunch.prefs.screens;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.GLInfoUtils;

public class LauncherPreferenceMiscellaneousFragment extends LauncherPreferenceFragment {

    private final ActivityResultLauncher<String[]> mBackgroundPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}

                String mime = requireContext().getContentResolver().getType(uri);
                boolean isVideo = mime != null && mime.startsWith("video/");

                LauncherPreferences.DEFAULT_PREF.edit()
                        .putString(LauncherPreferences.PREF_KEY_BACKGROUND_URI, uri.toString())
                        .putBoolean(LauncherPreferences.PREF_KEY_BACKGROUND_IS_VIDEO, isVideo)
                        .apply();
            });

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_misc);

        Preference driverPreference = requirePreference("zinkPreferSystemDriver");
        PackageManager packageManager = driverPreference.getContext().getPackageManager();
        boolean supportsTurnip = Tools.checkVulkanSupport(packageManager) && GLInfoUtils.getGlInfo().isAdreno();
        driverPreference.setVisible(supportsTurnip);

        requirePreference("backgroundChoose").setOnPreferenceClickListener(p -> {
            mBackgroundPickerLauncher.launch(new String[]{"image/*", "video/*"});
            return true;
        });

        requirePreference("backgroundClear").setOnPreferenceClickListener(p -> {
            LauncherPreferences.DEFAULT_PREF.edit()
                    .remove(LauncherPreferences.PREF_KEY_BACKGROUND_URI)
                    .remove(LauncherPreferences.PREF_KEY_BACKGROUND_IS_VIDEO)
                    .apply();
            return true;
        });
    }
}
