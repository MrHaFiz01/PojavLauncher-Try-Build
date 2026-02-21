package net.kdt.pojavlaunch.prefs.screens;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

public class LauncherPreferenceBackgroundFragment extends LauncherPreferenceFragment {

    private ActivityResultLauncher<Intent> mImagePickerLauncher;
    private ActivityResultLauncher<Intent> mVideoPickerLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                requireContext().getContentResolver().takePersistableUriPermission(uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (SecurityException ignored) {}
                            LauncherPreferences.DEFAULT_PREF.edit()
                                    .putString("backgroundImagePath", uri.toString())
                                    .apply();
                            Preference pref = findPreference("backgroundImagePath");
                            if (pref != null) pref.setSummary(uri.getLastPathSegment());
                        }
                    }
                });

        mVideoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                requireContext().getContentResolver().takePersistableUriPermission(uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (SecurityException ignored) {}
                            LauncherPreferences.DEFAULT_PREF.edit()
                                    .putString("backgroundVideoPath", uri.toString())
                                    .apply();
                            Preference pref = findPreference("backgroundVideoPath");
                            if (pref != null) pref.setSummary(uri.getLastPathSegment());
                        }
                    }
                });

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_background);

        Preference imagePref = requirePreference("backgroundImagePath");
        imagePref.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            mImagePickerLauncher.launch(intent);
            return true;
        });

        Preference videoPref = requirePreference("backgroundVideoPath");
        videoPref.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            mVideoPickerLauncher.launch(intent);
            return true;
        });

        String imgPath = LauncherPreferences.DEFAULT_PREF.getString("backgroundImagePath", "");
        if (!imgPath.isEmpty()) {
            imagePref.setSummary(Uri.parse(imgPath).getLastPathSegment());
        }
        String vidPath = LauncherPreferences.DEFAULT_PREF.getString("backgroundVideoPath", "");
        if (!vidPath.isEmpty()) {
            videoPref.setSummary(Uri.parse(vidPath).getLastPathSegment());
        }

        computeVisibility();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        super.onSharedPreferenceChanged(p, s);
        computeVisibility();
    }

    private void computeVisibility() {
        String bgType = LauncherPreferences.DEFAULT_PREF.getString("backgroundType", "default");
        requirePreference("backgroundColor").setVisible("color".equals(bgType));
        requirePreference("backgroundImagePath").setVisible("image".equals(bgType));
        requirePreference("backgroundVideoPath").setVisible("video".equals(bgType));
    }
}
