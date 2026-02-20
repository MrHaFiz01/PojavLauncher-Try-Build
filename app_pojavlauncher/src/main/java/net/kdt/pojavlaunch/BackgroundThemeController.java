package net.kdt.pojavlaunch;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.core.view.WindowInsetsControllerCompat;
import androidx.palette.graphics.Palette;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.InputStream;

public class BackgroundThemeController {
    private final Activity mActivity;
    private final ImageView mBackgroundImage;
    private final VideoView mBackgroundVideo;
    private int mAccentColor = 0;

    public BackgroundThemeController(Activity activity, ImageView backgroundImage, VideoView backgroundVideo) {
        mActivity = activity;
        mBackgroundImage = backgroundImage;
        mBackgroundVideo = backgroundVideo;
    }

    public void applyFromPreferences() {
        String uriString = LauncherPreferences.DEFAULT_PREF.getString(
                LauncherPreferences.PREF_KEY_BACKGROUND_URI, null);
        boolean isVideo = LauncherPreferences.DEFAULT_PREF.getBoolean(
                LauncherPreferences.PREF_KEY_BACKGROUND_IS_VIDEO, false);

        if (uriString == null || uriString.isEmpty()) {
            clearBackground();
            return;
        }

        Uri uri = Uri.parse(uriString);
        if (isVideo) {
            applyVideoBackground(uri);
        } else {
            applyImageBackground(uri);
        }
    }

    private void applyImageBackground(Uri uri) {
        mBackgroundVideo.setVisibility(View.GONE);
        mBackgroundVideo.stopPlayback();
        try {
            Bitmap bitmap = decodeSampledBitmap(uri);
            if (bitmap != null) {
                mBackgroundImage.setImageBitmap(bitmap);
                mBackgroundImage.setVisibility(View.VISIBLE);
                extractAndApplyColor(bitmap);
            } else {
                clearBackground();
            }
        } catch (Exception e) {
            clearBackground();
        }
    }

    private void applyVideoBackground(Uri uri) {
        mBackgroundImage.setVisibility(View.GONE);
        try {
            mBackgroundVideo.setVisibility(View.VISIBLE);
            mBackgroundVideo.setVideoURI(uri);
            mBackgroundVideo.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.setVolume(0f, 0f);
                mp.start();
            });
            mBackgroundVideo.setOnErrorListener((mp, what, extra) -> {
                clearBackground();
                return true;
            });
            mBackgroundVideo.start();

            // Extract color from video frame
            PojavApplication.sExecutorService.execute(() -> {
                try {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(mActivity, uri);
                    Bitmap frame = retriever.getFrameAtTime(1000000);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        retriever.close();
                    } else {
                        retriever.release();
                    }
                    if (frame != null) {
                        Bitmap scaled = Bitmap.createScaledBitmap(frame, 128,
                                (int) (128f * frame.getHeight() / frame.getWidth()), true);
                        frame.recycle();
                        Tools.runOnUiThread(() -> extractAndApplyColor(scaled));
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            clearBackground();
        }
    }

    private void clearBackground() {
        mBackgroundImage.setVisibility(View.GONE);
        mBackgroundImage.setImageBitmap(null);
        mBackgroundVideo.setVisibility(View.GONE);
        mBackgroundVideo.stopPlayback();
        mAccentColor = 0;
        applyDefaultThemeColors();
    }

    private Bitmap decodeSampledBitmap(Uri uri) {
        try {
            // First pass: get dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream is = mActivity.getContentResolver().openInputStream(uri);
            if (is == null) return null;
            BitmapFactory.decodeStream(is, null, options);
            is.close();

            // Calculate sample size
            int targetSize = Math.max(
                    mActivity.getResources().getDisplayMetrics().widthPixels,
                    mActivity.getResources().getDisplayMetrics().heightPixels);
            int inSampleSize = 1;
            if (options.outHeight > targetSize || options.outWidth > targetSize) {
                int halfHeight = options.outHeight / 2;
                int halfWidth = options.outWidth / 2;
                while ((halfHeight / inSampleSize) >= targetSize
                        && (halfWidth / inSampleSize) >= targetSize) {
                    inSampleSize *= 2;
                }
            }

            // Second pass: decode
            options = new BitmapFactory.Options();
            options.inSampleSize = inSampleSize;
            is = mActivity.getContentResolver().openInputStream(uri);
            if (is == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            is.close();
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private void extractAndApplyColor(Bitmap bitmap) {
        Bitmap small = Bitmap.createScaledBitmap(bitmap, 128,
                (int) (128f * bitmap.getHeight() / bitmap.getWidth()), true);
        Palette.from(small).maximumColorCount(16).generate(palette -> {
            if (palette == null) return;
            int defaultColor = mActivity.getResources().getColor(R.color.minebutton_color);
            mAccentColor = palette.getVibrantColor(
                    palette.getDominantColor(defaultColor));
            applyAccentColor(mAccentColor);
        });
    }

    private void applyAccentColor(int color) {
        Window window = mActivity.getWindow();
        // Darken the color for status bar
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.6f; // darken
        int statusBarColor = Color.HSVToColor(hsv);
        window.setStatusBarColor(statusBarColor);
        window.setNavigationBarColor(statusBarColor);

        // Set light/dark status bar icons based on luminance
        double luminance = (0.299 * Color.red(statusBarColor)
                + 0.587 * Color.green(statusBarColor)
                + 0.114 * Color.blue(statusBarColor)) / 255;
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(
                window, window.getDecorView());
        controller.setAppearanceLightStatusBars(luminance > 0.5);
    }

    private void applyDefaultThemeColors() {
        Window window = mActivity.getWindow();
        window.setStatusBarColor(mActivity.getResources().getColor(R.color.background_status_bar));
        window.setNavigationBarColor(mActivity.getResources().getColor(R.color.background_app));
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(
                window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
    }

    public int getAccentColor() {
        return mAccentColor;
    }

    public void pauseVideo() {
        if (mBackgroundVideo.isPlaying()) {
            mBackgroundVideo.pause();
        }
    }

    public void resumeVideo() {
        boolean isVideo = LauncherPreferences.DEFAULT_PREF.getBoolean(
                LauncherPreferences.PREF_KEY_BACKGROUND_IS_VIDEO, false);
        if (isVideo && mBackgroundVideo.getVisibility() == View.VISIBLE) {
            mBackgroundVideo.start();
        }
    }
}
