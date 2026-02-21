package net.kdt.pojavlaunch.customization;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;

public class BackgroundManager {
    private static final String TAG = "BackgroundManager";

    public static void applyBackground(View rootView, ImageView imageView, VideoView videoView) {
        String bgType = LauncherPreferences.PREF_BACKGROUND_TYPE;

        // Reset views
        imageView.setVisibility(View.GONE);
        imageView.setImageDrawable(null);
        videoView.setVisibility(View.GONE);
        videoView.stopPlayback();
        rootView.setBackground(null);

        switch (bgType) {
            case "color":
                applyColorBackground(rootView);
                break;
            case "image":
                applyImageBackground(imageView);
                break;
            case "video":
                applyVideoBackground(videoView);
                break;
            default:
                break;
        }
    }

    private static void applyColorBackground(View rootView) {
        try {
            int color = Color.parseColor(LauncherPreferences.PREF_BACKGROUND_COLOR);
            rootView.setBackground(new ColorDrawable(color));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid background color: " + LauncherPreferences.PREF_BACKGROUND_COLOR, e);
        }
    }

    private static void applyImageBackground(ImageView imageView) {
        String path = LauncherPreferences.PREF_BACKGROUND_IMAGE_PATH;
        if (path.isEmpty()) return;
        try {
            Uri uri = Uri.parse(path);
            imageView.setImageURI(uri);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.w(TAG, "Failed to load background image", e);
        }
    }

    private static void applyVideoBackground(VideoView videoView) {
        String path = LauncherPreferences.PREF_BACKGROUND_VIDEO_PATH;
        if (path.isEmpty()) return;
        try {
            Uri uri = Uri.parse(path);
            videoView.setVideoURI(uri);
            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.setVolume(0f, 0f);
            });
            videoView.setVisibility(View.VISIBLE);
            videoView.start();
        } catch (Exception e) {
            Log.w(TAG, "Failed to load background video", e);
        }
    }
}
