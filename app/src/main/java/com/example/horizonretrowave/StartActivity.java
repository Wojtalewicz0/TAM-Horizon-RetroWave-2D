package com.example.horizonretrowave;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.graphics.SurfaceTexture;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.content.Intent;
import android.hardware.camera2.CameraCharacteristics;
import android.media.MediaPlayer;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class StartActivity extends AppCompatActivity {

    private static final String TAG = "StartActivity";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor uiExecutor = command -> mainHandler.post(command);

    private TextureView profileVideoBackground;
    private MediaPlayer profileVideoPlayer;
    private Surface profileVideoSurface;
    private View profileContainer;
    private View playButton;
    private View epilepsyWarning;
    private View blackFadeOverlay;
    private TextView profileTitle;
    private EditText profileNick;
    private ImageView profileImage;
    private Button changeProfileButton;
    private Button playProfileButton;
    private TextView epilepsyTitle;
    private TextView epilepsyBody;
    private CompletableFuture<Void> startSequence;
    private Runnable restoreNickBackground;
    private boolean profileTransitionStarted;
    private Bitmap pendingProfileBitmap;
    private Uri pendingCameraPhotoUri;
    private File pendingCameraPhotoFile;

    private final ActivityResultLauncher<Intent> frontCameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::handleCameraResult
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Shared.Initialize(getApplicationContext());

        Shared.LoadSharedSave(getApplicationContext());

        if (Shared.sharedSave.devMode) {
            openMainActivityAndFinish();
            return;
        }

        EdgeToEdge.enable(this);
        enableImmersiveLandscapeWindow();
        setContentView(R.layout.activity_start);

        profileVideoBackground = findViewById(R.id.profile_video_background);
        profileContainer = findViewById(R.id.profile_container);
        playButton = findViewById(R.id.play_button);
        profileTitle = findViewById(R.id.profile_title);
        profileNick = findViewById(R.id.profile_nick);
        profileImage = findViewById(R.id.profile_image);
        changeProfileButton = findViewById(R.id.change_profile_button);
        playProfileButton = findViewById(R.id.play_button);
        epilepsyWarning = findViewById(R.id.epilepsy_warning);
        blackFadeOverlay = findViewById(R.id.black_fade_overlay);
        epilepsyTitle = findViewById(R.id.epilepsy_title);
        epilepsyBody = findViewById(R.id.epilepsy_body);

        loadSavedProfile();

        prepareProfileVideoSurface();

        Typeface titleFont = ResourcesCompat.getFont(this, R.font.epilepsy_title_bold);
        Typeface bodyFont = ResourcesCompat.getFont(this, R.font.epilepsy_text);
        if (titleFont != null) {
            epilepsyTitle.setTypeface(titleFont, Typeface.NORMAL);
        }
        if (bodyFont != null) {
            epilepsyBody.setTypeface(bodyFont, Typeface.NORMAL);
        }

        applyProfileNeonStyle();

        changeProfileButton.setOnClickListener(view -> openFrontCamera());
        playButton.setOnClickListener(view -> handlePlayButtonClick());

        startAppSequence();
    }

    private void applyProfileNeonStyle() {
        int neonPink = ContextCompat.getColor(this, R.color.profile_neon_pink_glow);
        int buttonFill = ContextCompat.getColor(this, R.color.profile_button_magenta);
        int inputFill = ContextCompat.getColor(this, R.color.profile_input_fill);

        profileTitle.setShadowLayer(24f, 0f, 0f, neonPink);
        profileNick.setShadowLayer(22f, 0f, 0f, neonPink);
        changeProfileButton.setShadowLayer(22f, 0f, 0f, neonPink);
        playProfileButton.setShadowLayer(26f, 0f, 0f, neonPink);

        changeProfileButton.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        playProfileButton.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        profileNick.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        changeProfileButton.setBackground(
                new NeonBackgroundDrawable(buttonFill, neonPink, 10f, 22f)
        );
        playProfileButton.setBackground(
                new NeonBackgroundDrawable(buttonFill, neonPink, 10f, 24f)
        );
        profileNick.setBackground(
                new NeonBackgroundDrawable(inputFill, neonPink, 10f, 22f)
        );
    }

    private void loadSavedProfile() {
        String savedName = Shared.sharedSave.playerName;
        if (savedName != null
                && !savedName.trim().isEmpty()
                && !"null".equalsIgnoreCase(savedName.trim())) {
            profileNick.setText(savedName);
            profileNick.setSelection(profileNick.length());
        }

        Bitmap savedPhoto = Shared.LoadProfilePhoto(this);
        if (savedPhoto != null) {
            profileImage.setImageBitmap(savedPhoto);
        }
    }

    private void openFrontCamera() {
        if (profileTransitionStarted) {
            return;
        }

        File photoFile = new File(getCacheDir(), "profile_photo_camera.jpg");
        if (photoFile.exists() && !photoFile.delete()) {
            Toast.makeText(this, "Nie można przygotować aparatu.", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        Uri photoUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                photoFile
        );

        pendingCameraPhotoFile = photoFile;
        pendingCameraPhotoUri = photoUri;

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

        cameraIntent.putExtra(
                "android.intent.extras.CAMERA_FACING",
                CameraCharacteristics.LENS_FACING_FRONT
        );
        cameraIntent.putExtra(
                "android.intent.extras.LENS_FACING",
                CameraCharacteristics.LENS_FACING_FRONT
        );
        cameraIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
        cameraIntent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        );

        try {
            frontCameraLauncher.launch(cameraIntent);
        } catch (ActivityNotFoundException exception) {
            pendingCameraPhotoFile = null;
            pendingCameraPhotoUri = null;
            Log.e(TAG, "No camera application is available.", exception);
            Toast.makeText(this, "Brak aplikacji aparatu na urządzeniu.", Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void handleCameraResult(ActivityResult result) {
        File photoFile = pendingCameraPhotoFile;
        pendingCameraPhotoFile = null;
        pendingCameraPhotoUri = null;

        if (result.getResultCode() != Activity.RESULT_OK || photoFile == null) {
            if (photoFile != null) {
                photoFile.delete();
            }
            return;
        }

        Bitmap capturedPhoto = decodeCapturedPhoto(photoFile);
        photoFile.delete();

        if (capturedPhoto == null) {
            Toast.makeText(this, "Nie udało się odczytać zdjęcia.", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        pendingProfileBitmap = capturedPhoto;
        profileImage.setImageBitmap(capturedPhoto);
    }

    private Bitmap decodeCapturedPhoto(File photoFile) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoFile.getAbsolutePath(), bounds);

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateSampleSize(
                bounds.outWidth,
                bounds.outHeight,
                1024
        );
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath(), options);
        if (bitmap == null) {
            return null;
        }

        try {
            ExifInterface exif = new ExifInterface(photoFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            Matrix rotation = new Matrix();
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                rotation.postRotate(90f);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                rotation.postRotate(180f);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                rotation.postRotate(270f);
            }

            if (!rotation.isIdentity()) {
                Bitmap rotated = Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.getWidth(),
                        bitmap.getHeight(),
                        rotation,
                        true
                );
                if (rotated != bitmap) {
                    bitmap.recycle();
                }
                return rotated;
            }
        } catch (IOException exception) {
            Log.w(TAG, "Could not read profile photo orientation.", exception);
        }

        return bitmap;
    }

    private int calculateSampleSize(int width, int height, int maxDimension) {
        int sampleSize = 1;
        while (width / sampleSize > maxDimension
                || height / sampleSize > maxDimension) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    private void handlePlayButtonClick() {
        if (profileTransitionStarted) {
            return;
        }

        String nickname = profileNick.getText().toString();
        if (nickname.trim().isEmpty()) {
            showEmptyNickError();
            return;
        }

        if (pendingProfileBitmap != null
                && !Shared.SaveProfilePhoto(this, pendingProfileBitmap)) {
            Toast.makeText(this, "Nie udało się zapisać zdjęcia profilu.", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        Shared.sharedSave.playerName = nickname.trim();
        Shared.SaveSharedSave(this);

        profileTransitionStarted = true;
        playButton.setEnabled(false);
        profileNick.setEnabled(false);

        Shared.OpacityTo(profileContainer, profileContainer.getAlpha(), 0.0f, 1000L);
        Shared.OpacityTo(
                profileVideoBackground,
                profileVideoBackground.getAlpha(),
                0.0f,
                1000L
        );

        mainHandler.postDelayed(() -> {
            openMainActivityAndFinish();
        }, 1000L);
    }

    private void openMainActivityAndFinish() {
        Intent intent = new Intent(StartActivity.this, MainActivity.class);
        startActivity(intent);

        finish();
    }

    private void showEmptyNickError() {
        if (restoreNickBackground != null) {
            mainHandler.removeCallbacks(restoreNickBackground);
        }

        profileNick.setBackgroundColor(Color.RED);
        restoreNickBackground = () -> {
            restoreProfileNickBackground();
            restoreNickBackground = null;
        };
        mainHandler.postDelayed(restoreNickBackground, 1000L);
    }

    private void restoreProfileNickBackground() {
        int neonPink = ContextCompat.getColor(this, R.color.profile_neon_pink_glow);
        int inputFill = ContextCompat.getColor(this, R.color.profile_input_fill);
        profileNick.setBackground(
                new NeonBackgroundDrawable(inputFill, neonPink, 10f, 22f)
        );
    }

    private void prepareProfileVideoSurface() {
        profileVideoBackground.setSurfaceTextureListener(
                new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(
                            SurfaceTexture surfaceTexture,
                            int width,
                            int height
                    ) {
                        profileVideoSurface = new Surface(surfaceTexture);
                        prepareProfileVideo();
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(
                            SurfaceTexture surfaceTexture,
                            int width,
                            int height
                    ) {
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                        releaseProfileVideo();
                        if (profileVideoSurface != null) {
                            profileVideoSurface.release();
                            profileVideoSurface = null;
                        }
                        return true;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                    }
                }
        );
    }

    private void prepareProfileVideo() {
        if (profileVideoSurface == null) {
            return;
        }

        releaseProfileVideo();

        MediaPlayer player = new MediaPlayer();
        profileVideoPlayer = player;

        try {
            player.setDataSource(
                    this,
                    Uri.parse("android.resource://" + getPackageName()
                            + "/" + R.raw.animation)
            );
            player.setSurface(profileVideoSurface);
            player.setVolume(0.0f, 0.0f);
            player.setLooping(true);
            player.setOnPreparedListener(preparedPlayer -> {
                if (profileVideoPlayer != preparedPlayer) {
                    preparedPlayer.release();
                    return;
                }
                preparedPlayer.setVolume(0.0f, 0.0f);
                preparedPlayer.start();
            });
            player.setOnErrorListener((failedPlayer, what, extra) -> {
                Log.e(TAG, "Could not play profile background video. what="
                        + what + ", extra=" + extra);
                return true;
            });
            player.prepareAsync();
        } catch (IOException | RuntimeException exception) {
            Log.e(TAG, "Could not prepare res/raw/animation.mp4", exception);
            releaseProfileVideo();
        }
    }

    private void releaseProfileVideo() {
        if (profileVideoPlayer != null) {
            profileVideoPlayer.release();
            profileVideoPlayer = null;
        }
    }

    private void startAppSequence() {
        startSequence = delaySeconds(5.0)
                .thenCompose(ignore -> runOnUi(() -> {
                    epilepsyWarning.setVisibility(View.VISIBLE);
                    Shared.OpacityTo(epilepsyWarning, 0.0f, 1.0f, 2000L);
                }))
                .thenCompose(ignore -> delaySeconds(2.0))
                .thenCompose(ignore -> delaySeconds(6.0))
                .thenCompose(ignore -> runOnUi(() ->
                        Shared.OpacityTo(epilepsyWarning, 1.0f, 0.0f, 2000L)
                ))
                .thenCompose(ignore -> delaySeconds(2.0))
                .thenCompose(ignore -> runOnUi(() -> {
                    epilepsyWarning.setVisibility(View.GONE);
                    epilepsyWarning.setAlpha(0.0f);
                }))
                .thenCompose(ignore -> delaySeconds(5.0))
                .thenCompose(ignore -> runOnUi(() -> {
                    if (!"null".equals(Shared.sharedSave.playerName)) {
                        openMainActivityAndFinish();
                        return;
                    }

                    profileVideoBackground.setVisibility(View.VISIBLE);
                    profileContainer.setVisibility(View.VISIBLE);
                    profileVideoBackground.setAlpha(0.0f);
                    profileContainer.setAlpha(0.0f);

                    Shared.OpacityTo(profileVideoBackground, 0.0f, 0.15f, 3000L);
                    Shared.OpacityTo(profileContainer, 0.0f, 1.0f, 3000L);
                }));
    }

    private CompletableFuture<Void> delaySeconds(double seconds) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        long milliseconds = Math.round(seconds * 1000.0);
        mainHandler.postDelayed(() -> future.complete(null), milliseconds);

        return future;
    }

    private CompletableFuture<Void> runOnUi(Runnable action) {
        return CompletableFuture.runAsync(action, uiExecutor);
    }

    private void enableImmersiveLandscapeWindow() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        );
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersiveLandscapeWindow();
        }
    }

    @Override
    protected void onDestroy() {
        if (startSequence != null) {
            startSequence.cancel(true);
        }
        mainHandler.removeCallbacksAndMessages(null);
        releaseProfileVideo();
        if (profileVideoSurface != null) {
            profileVideoSurface.release();
            profileVideoSurface = null;
        }
        super.onDestroy();
    }
}
