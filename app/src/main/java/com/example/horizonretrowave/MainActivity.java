package com.example.horizonretrowave;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERFORMANCE_NEON_COLOR = 0xFFFF2DBB;
    private static final int GAME_ENTRY_GLOW_COLOR = 0xFFFFDD4A;
    private static final int GAME_TIMER_BONUS_COLOR = 0xFF54FF78;
    private static ArrayList<RankingEntry> cachedRankingJsonEntries;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor uiExecutor = command -> mainHandler.post(command);
    private final Random gameplayRandom = new Random();
    private TextureView videoBackground;
    private MediaPlayer backgroundPlayer;
    private Surface videoSurface;
    private int currentVideoResourceId;
    private String currentVideoResourceName;
    private long backgroundPrepareRequest;
    private View blackFadeOverlay;
    private TextView gameTimerText;
    private TextView gamePointsText;
    private TextView gamePointsEarnedText;
    private LinearLayout gameRangeRow;
    private TextView gameEntryText;
    private TextView gameRangeLowText;
    private TextView gameRangeHighText;
    private FrameLayout gameCarDisplay;
    private ImageView gameCarImage;
    private ImageView gameCarShadow;
    private TextView gameCarPlate;
    private FrameLayout gameNumpadPanel;
    private final View[] gameNumpadButtons = new View[11];
    private TextView startText;
    private FrameLayout gameResultsPanel;
    private TextView gameResultsText;
    private NeonGlowDrawable gameResultsOutline;
    private ValueAnimator gameCarBounceAnimator;
    private Runnable gameTimerRunnable;
    private Runnable gameTimerBonusRestoreRunnable;
    private Runnable correctGuessHideRunnable;
    private Runnable earnedPointsFadeOutRunnable;
    private Runnable earnedPointsRestoreRunnable;
    private Runnable carShadowFadeOutRunnable;
    private Runnable carShadowHideRunnable;
    private final StringBuilder gameNumberInput = new StringBuilder();
    private boolean gameTimerBonusHighlightActive;
    private View creditsTouchOverlay;
    private View creditsContent;
    private View menuContainer;
    private ImageView startupPolwojLogo;
    private ImageView startupZseLogo;
    private TextView menuPlay;
    private TextView menuGarage;
    private TextView menuRankings;
    private TextView menuSettings;
    private TextView menuExit;
    private FrameLayout exitConfirmationOverlay;
    private FrameLayout exitConfirmationDialog;
    private TextView exitConfirmationText;
    private TextView exitConfirmationYes;
    private TextView exitConfirmationNo;
    private FrameLayout settingsOverlay;
    private FrameLayout settingsPanel;
    private TextView settingsTitle;
    private NeonBackButtonView settingsBack;
    private TextView settingsSound;
    private TextView settingsPerformance;
    private TextView settingsReset;
    private TextView settingsAuthors;
    private FrameLayout rankingsOverlay;
    private FrameLayout rankingsPanel;
    private NeonBackButtonView rankingsBack;
    private LinearLayout rankingsRowsContainer;
    private TextView rankingsHeaderNumber;
    private TextView rankingsHeaderName;
    private TextView rankingsHeaderPoints;
    private FrameLayout garageOverlay;
    private FrameLayout garagePanel;
    private NeonBackButtonView garageBack;
    private NeonArrowButtonView garagePreviousCar;
    private NeonArrowButtonView garageNextCar;
    private TextView garageCarName;
    private TextView garageCarMode;
    private ImageView garageCarImage;
    private EditText garagePlate;
    private final ArrayList<TextView> menuNeonTextViews = new ArrayList<>();
    private final ArrayList<NeonGlowDrawable> menuNeonDrawables = new ArrayList<>();
    private final ArrayList<TextView> exitConfirmationNeonTextViews = new ArrayList<>();
    private final ArrayList<NeonGlowDrawable> exitConfirmationNeonDrawables = new ArrayList<>();
    private final ArrayList<TextView> settingsNeonTextViews = new ArrayList<>();
    private final ArrayList<NeonGlowDrawable> settingsNeonDrawables = new ArrayList<>();
    private final ArrayList<TextView> rankingsNeonTextViews = new ArrayList<>();
    private final ArrayList<NeonGlowDrawable> rankingsNeonDrawables = new ArrayList<>();
    private final ArrayList<TextView> garageNeonTextViews = new ArrayList<>();
    private final ArrayList<NeonGlowDrawable> garageNeonDrawables = new ArrayList<>();
    private final ArrayList<NeonGlowDrawable> gameNumpadNeonDrawables = new ArrayList<>();
    private int activeNeonColor = PERFORMANCE_NEON_COLOR;
    private float activeNeonGlowRadius = 36f;
    private boolean activeNeonShadowEnabled = true;
    private ValueAnimator menuNeonAnimator;
    private ValueAnimator gameNumpadNeonAnimator;
    private CompletableFuture<Void> introSequence;
    private CompletableFuture<Void> gameSequence;
    private CompletableFuture<Void> finishGameReturnSequence;
    private CompletableFuture<Void> creditsSequence;
    private boolean gameTransitionStarted;
    private boolean gameFinishedShown;
    public boolean menuReadyFlag = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Shared.Initialize(getApplicationContext());
        EdgeToEdge.enable(this);
        enableImmersiveLandscapeWindow();
        setContentView(R.layout.activity_main);
        videoBackground = findViewById(R.id.video_background);
        videoBackground.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(
                    SurfaceTexture surfaceTexture,
                    int width,
                    int height
            ) {
                videoSurface = new Surface(surfaceTexture);
                prepareBackgroundVideo();
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
                releaseBackgroundPlayer();
                if (videoSurface != null) {
                    videoSurface.release();
                    videoSurface = null;
                }
                return true;
            }
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            }
        });
        menuContainer = findViewById(R.id.menu_container);
        startupPolwojLogo = findViewById(R.id.startup_polwoj_logo);
        startupZseLogo = findViewById(R.id.startup_zse_logo);
        menuPlay = findViewById(R.id.menu_play);
        menuGarage = findViewById(R.id.menu_garage);
        menuRankings = findViewById(R.id.menu_rankings);
        menuSettings = findViewById(R.id.menu_settings);
        menuExit = findViewById(R.id.menu_exit);
        exitConfirmationOverlay = findViewById(R.id.exit_confirmation_overlay);
        exitConfirmationDialog = findViewById(R.id.exit_confirmation_dialog);
        exitConfirmationText = findViewById(R.id.exit_confirmation_text);
        exitConfirmationYes = findViewById(R.id.exit_confirmation_yes);
        exitConfirmationNo = findViewById(R.id.exit_confirmation_no);
        settingsOverlay = findViewById(R.id.settings_overlay);
        settingsPanel = findViewById(R.id.settings_panel);
        settingsTitle = findViewById(R.id.settings_title);
        settingsBack = findViewById(R.id.settings_back);
        settingsSound = findViewById(R.id.settings_sound);
        settingsPerformance = findViewById(R.id.settings_performance);
        settingsReset = findViewById(R.id.settings_reset);
        settingsAuthors = findViewById(R.id.settings_authors);
        rankingsOverlay = findViewById(R.id.rankings_overlay);
        rankingsPanel = findViewById(R.id.rankings_panel);
        rankingsBack = findViewById(R.id.rankings_back);
        rankingsRowsContainer = findViewById(R.id.rankings_rows_container);
        rankingsHeaderNumber = findViewById(R.id.rankings_header_number);
        rankingsHeaderName = findViewById(R.id.rankings_header_name);
        rankingsHeaderPoints = findViewById(R.id.rankings_header_points);
        garageOverlay = findViewById(R.id.garage_overlay);
        garagePanel = findViewById(R.id.garage_panel);
        garageBack = findViewById(R.id.garage_back);
        garagePreviousCar = findViewById(R.id.garage_previous_car);
        garageNextCar = findViewById(R.id.garage_next_car);
        garageCarName = findViewById(R.id.garage_car_name);
        garageCarMode = findViewById(R.id.garage_car_mode);
        garageCarImage = findViewById(R.id.garage_car_image);
        garagePlate = findViewById(R.id.garage_plate);
        gameTimerText = findViewById(R.id.game_timer_text);
        gamePointsText = findViewById(R.id.game_points_text);
        gamePointsEarnedText = findViewById(R.id.game_points_earned_text);
        gameRangeRow = findViewById(R.id.game_range_row);
        gameEntryText = findViewById(R.id.game_entry_text);
        gameRangeLowText = findViewById(R.id.game_range_low_text);
        gameRangeHighText = findViewById(R.id.game_range_high_text);
        gameCarDisplay = findViewById(R.id.game_car_display);
        gameCarImage = findViewById(R.id.game_car_image);
        gameCarShadow = findViewById(R.id.game_car_shadow);
        gameCarPlate = findViewById(R.id.game_car_plate);
        gameNumpadPanel = findViewById(R.id.game_numpad_panel);
        gameNumpadButtons[0] = findViewById(R.id.game_numpad_7);
        gameNumpadButtons[1] = findViewById(R.id.game_numpad_8);
        gameNumpadButtons[2] = findViewById(R.id.game_numpad_9);
        gameNumpadButtons[3] = findViewById(R.id.game_numpad_4);
        gameNumpadButtons[4] = findViewById(R.id.game_numpad_5);
        gameNumpadButtons[5] = findViewById(R.id.game_numpad_6);
        gameNumpadButtons[6] = findViewById(R.id.game_numpad_1);
        gameNumpadButtons[7] = findViewById(R.id.game_numpad_2);
        gameNumpadButtons[8] = findViewById(R.id.game_numpad_3);
        gameNumpadButtons[9] = findViewById(R.id.game_numpad_enter);
        gameNumpadButtons[10] = findViewById(R.id.game_numpad_0);
        startText = findViewById(R.id.start_text);
        gameResultsPanel = findViewById(R.id.game_results_panel);
        gameResultsText = findViewById(R.id.game_results_text);
        creditsTouchOverlay = findViewById(R.id.credits_touch_overlay);
        creditsContent = findViewById(R.id.credits_content);
        gameTimerText.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        gamePointsText.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        gamePointsEarnedText.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        gamePointsEarnedText.setTextColor(Color.WHITE);
        applyGameHudGlow(gamePointsEarnedText, Color.WHITE);
        gameEntryText.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        gameEntryText.setTextColor(GAME_ENTRY_GLOW_COLOR);
        applyGameHudGlow(gameEntryText, GAME_ENTRY_GLOW_COLOR);
        startText.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        gameResultsText.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        gameResultsOutline = new NeonGlowDrawable();
        gameResultsOutline.setGlowColor(Color.rgb(157, 48, 255));
        gameResultsOutline.setGlowRadius(48f);
        gameResultsOutline.setShadowEnabled(!Shared.sharedSave.performanceModeEnabled);
        gameResultsPanel.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        gameResultsPanel.setForeground(gameResultsOutline);
        prepareGameNumpad();
        menuPlay.setOnClickListener(view -> ClickedGame());
        menuGarage.setOnClickListener(view -> ClickedGarage());
        menuRankings.setOnClickListener(view -> ClickedRankTable());
        menuSettings.setOnClickListener(view -> ClickedSettings());
        menuExit.setOnClickListener(view -> ClickedExit());
        prepareMenuNeonEffect(menuContainer);
        prepareExitConfirmationNeonEffect();
        prepareSettingsNeonEffect();
        prepareRankingsNeonEffect();
        prepareGarageNeonEffect();
        exitConfirmationYes.setOnClickListener(view -> confirmExit());
        exitConfirmationNo.setOnClickListener(view -> hideExitConfirmation());
        settingsBack.setOnClickListener(view -> hideSettings());
        rankingsBack.setOnClickListener(view -> hideRankings());
        garageBack.setOnClickListener(view -> ClickedGarageBack());
        garagePreviousCar.setPointsRight(false);
        garageNextCar.setPointsRight(true);
        garagePreviousCar.setOnClickListener(view -> selectOtherGarageCar());
        garageNextCar.setOnClickListener(view -> selectOtherGarageCar());
        configureGaragePlateInput();
        settingsSound.setOnClickListener(view -> toggleSoundSetting());
        settingsPerformance.setOnClickListener(view -> togglePerformanceSetting());
        settingsReset.setOnClickListener(view -> showResetProfileConfirmation());
        settingsAuthors.setOnClickListener(view -> ShowCredits());
        updateSoundButtonLabel();
        updatePerformanceButtonLabel();
        blackFadeOverlay = findViewById(R.id.black_fade_overlay);
        startAppSequence();
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
    public void ChangeVideo(String fileName) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> ChangeVideo(fileName));
            return;
        }
        if (videoBackground == null) {
            throw new IllegalStateException(
                    "video_background must be initialized before ChangeVideo()."
            );
        }
        String resourceName = normalizeVideoResourceName(fileName);
        int resourceId = getResources().getIdentifier(
                resourceName,
                "raw",
                getPackageName()
        );
        if (resourceId == 0) {
            Log.e(TAG, "Video resource not found in res/raw: " + fileName);
            return;
        }
        currentVideoResourceName = resourceName;
        currentVideoResourceId = resourceId;
        prepareBackgroundVideo();
    }
    private void prepareBackgroundVideo() {
        if (videoSurface == null || currentVideoResourceId == 0) {
            return;
        }
        final long prepareRequest = ++backgroundPrepareRequest;
        MediaPlayer player = backgroundPlayer;
        try {
            if (player == null) {
                player = new MediaPlayer();
                backgroundPlayer = player;
            } else {
                player.reset();
            }
            final int resourceId = currentVideoResourceId;
            final String resourceName = currentVideoResourceName;
            player.setDataSource(
                    this,
                    Uri.parse("android.resource://" + getPackageName()
                            + "/" + resourceId)
            );
            player.setSurface(videoSurface);
            player.setVolume(0.0f, 0.0f);
            player.setLooping(shouldLoopVideo(resourceName));
            player.setOnPreparedListener(preparedPlayer -> {
                if (backgroundPlayer != preparedPlayer
                        || backgroundPrepareRequest != prepareRequest
                        || videoSurface == null) {
                    return;
                }
                preparedPlayer.setVolume(0.0f, 0.0f);
                preparedPlayer.start();
            });
            player.setOnCompletionListener(completedPlayer -> {
                if (backgroundPlayer == completedPlayer
                        && backgroundPrepareRequest == prepareRequest
                        && resourceName.equals(currentVideoResourceName)) {
                    WhenVideoFinished();
                }
            });
            player.setOnErrorListener((failedPlayer, what, extra) -> {
                Log.e(TAG, "Could not play background video. what=" + what
                        + ", extra=" + extra);
                return true;
            });
            player.prepareAsync();
        } catch (IOException | RuntimeException exception) {
            Log.e(TAG, "Could not prepare background video: "
                    + currentVideoResourceName, exception);
            if (backgroundPlayer == player) {
                releaseBackgroundPlayer();
            }
        }
    }
    private boolean shouldLoopVideo(String resourceName) {
        return !"intro".equals(resourceName);
    }
    private void releaseBackgroundPlayer() {
        backgroundPrepareRequest++;
        if (backgroundPlayer != null) {
            backgroundPlayer.setOnPreparedListener(null);
            backgroundPlayer.setOnCompletionListener(null);
            backgroundPlayer.setOnErrorListener(null);
            backgroundPlayer.release();
            backgroundPlayer = null;
        }
    }
    public void WhenVideoFinished() {
        if (currentVideoResourceName == null) {
            return;
        }
        if ("intro".equals(currentVideoResourceName)) {
            ChangeVideo("intro_ext.mp4");
        } else {
            ChangeVideo(currentVideoResourceName + ".mp4");
        }
    }
    private String normalizeVideoResourceName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Video file name cannot be empty.");
        }
        String resourceName = fileName.trim();
        int extensionStart = resourceName.lastIndexOf('.');
        if (extensionStart > 0) {
            resourceName = resourceName.substring(0, extensionStart);
        }
        return resourceName.toLowerCase(Locale.US);
    }
    private void startAppSequence() {
        Shared.OpacityTo(videoBackground, 1.0f, 0.0f, 0L);
        introSequence = runOnUi(() -> Shared.SoundPlay("intro_prev.mp3", 1.0f))
                .thenCompose(ignore -> delaySecondsUnconditionally(1.0))
                .thenCompose(ignore -> playStartupLogo(startupPolwojLogo, R.drawable.polwoj_white))
                .thenCompose(ignore -> delaySecondsUnconditionally(3.0))
                .thenCompose(ignore -> playStartupLogo(startupZseLogo, R.drawable.zse))
                .thenCompose(ignore -> delaySecondsUnconditionally(4.0))
                .thenCompose(ignore -> runOnUi(() -> {
                    ChangeVideo("intro.mp4");
                    Shared.SoundPlay(
                            "theme.mp3",
                            1.0f,
                            false,
                            () -> {
                                if (!gameTransitionStarted) {
                                    Shared.SoundPlay("theme_next.mp3", 1.0f, true);
                                }
                            }
                    );
                    Shared.OpacityTo(videoBackground, 0.0f, 1.0f, 600L);
                }))
                .thenCompose(ignore -> delaySeconds(17.0))
                .thenCompose(ignore -> runOnUi(() -> {
                    menuContainer.setVisibility(View.VISIBLE);
                    startMenuNeonPulse();
                    Shared.OpacityTo(menuContainer, 0.0f, 1.0f, 5000L);
                    if (Shared.sharedSave.points <= 1) {
                        Shared.OpacityTo(menuRankings, 0.0f, 0.5f, 3000L);
                    }
                }))
                .thenCompose(ignore -> delaySeconds(4.0))
                .thenCompose(ignore -> runOnUi(() -> {
                    menuReadyFlag = true;
                    Shared.sharedSave.devMode = false;
                }));
    }
    private CompletableFuture<Void> playStartupLogo(ImageView logo, int drawableResource) {
        return runOnUi(() -> {
                    logo.setImageResource(drawableResource);
                    logo.setVisibility(View.VISIBLE);
                    logo.setAlpha(0f);
                    Shared.OpacityTo(logo, 0f, 1f, 1000L);
                })
                .thenCompose(ignore -> delaySecondsUnconditionally(1.0))
                .thenCompose(ignore -> delaySecondsUnconditionally(1.0))
                .thenCompose(ignore -> runOnUi(() -> Shared.OpacityTo(logo, 1f, 0f, 1000L)))
                .thenCompose(ignore -> delaySecondsUnconditionally(1.0))
                .thenCompose(ignore -> runOnUi(() -> {
                    logo.setVisibility(View.GONE);
                    logo.setAlpha(0f);
                }));
    }
    private void ClickedGame() {
        if (gameTransitionStarted) {
            return;
        }
        stopGameCarBounce();
        gameCarDisplay.setTranslationY(0f);
        startText.setScaleX(1f);
        startText.setScaleY(1f);
        gameTransitionStarted = true;
        menuReadyFlag = false;
        if (menuNeonAnimator != null) {
            menuNeonAnimator.cancel();
            menuNeonAnimator = null;
        }
        Shared.sharedSave.lowRange = 0;
        Shared.sharedSave.highRange = 10;
        Shared.sharedSave.attemptsCounter = 0;
        Shared.sharedSave.inGameLowRange = Shared.sharedSave.lowRange;
        Shared.sharedSave.inGameHighRange = Shared.sharedSave.highRange;
        generateNumberToGuess();
        Shared.sharedSave.startGameSeconds = 60;
        if (Shared.sharedSave.carSelected == 2) {
            Shared.sharedSave.startGameSeconds = 45;
        }
        blackFadeOverlay.bringToFront();
        Shared.BlackFade(blackFadeOverlay, 0f, 1f, 1000L);
        blackFadeOverlay.setFocusableInTouchMode(true);
        blackFadeOverlay.requestFocus();
        Shared.SoundFade("theme.mp3", 0f, 1.0);
        Shared.SoundFade("theme_next.mp3", 0f, 1.0);
        gameSequence = delaySeconds(1.0)
                .thenCompose(ignore -> runOnUi(() -> {
                    Shared.SoundStop("theme.mp3");
                    Shared.SoundStop("theme_next.mp3");
                    menuContainer.setVisibility(View.GONE);
                    menuContainer.setAlpha(0f);
                    ChangeVideo("game.mp4");
                    Shared.SoundPlay("start.mp3", 1.0f);
                }))
                .thenCompose(ignore -> delaySeconds(3.0))
                .thenCompose(ignore -> runOnUi(() -> {
                    gameFinishedShown = false;
                    if (gameTimerRunnable != null) {
                        mainHandler.removeCallbacks(gameTimerRunnable);
                        gameTimerRunnable = null;
                    }
                    cancelGameTimerBonusHighlight();
                    cancelEarnedPointsPopup();
                    cancelGameCarShadowEffect();
                    clearCorrectGuessMessage();
                    gameCarShadow.setVisibility(View.GONE);
                    gameCarShadow.setAlpha(0f);
                    gamePointsEarnedText.setVisibility(View.GONE);
                    gamePointsEarnedText.setAlpha(0f);
                    gameNumberInput.setLength(0);
                    Shared.sharedSave.inGameSeconds = Math.max(
                            0,
                            Shared.sharedSave.startGameSeconds
                    );
                    Shared.sharedSave.inGamePoints = 0;
                    Shared.sharedSave.inGamePointsEarned = 0;
                    updateGameTimerDisplay();
                    updateGamePointsDisplay();
                    updateGameRangeDisplay();
                    gameEntryText.setText("");
                    updateGameCarDisplay();
                    gameTimerText.setVisibility(View.VISIBLE);
                    gameTimerText.setAlpha(0f);
                    gameTimerText.bringToFront();
                    Shared.OpacityTo(gameTimerText, 0f, 1f, 1000L);
                    gamePointsText.setVisibility(View.VISIBLE);
                    gamePointsText.setAlpha(0f);
                    gamePointsText.bringToFront();
                    Shared.OpacityTo(gamePointsText, 0f, 1f, 1000L);
                    gameRangeRow.setVisibility(View.VISIBLE);
                    gameRangeRow.setAlpha(0f);
                    gameRangeRow.bringToFront();
                    Shared.OpacityTo(gameRangeRow, 0f, 1f, 1000L);
                    gameCarDisplay.setVisibility(View.VISIBLE);
                    gameCarDisplay.setAlpha(0f);
                    gameCarDisplay.setTranslationY(0f);
                    gameCarDisplay.bringToFront();
                    startGameCarBounce();
                    Shared.OpacityTo(gameCarDisplay, 0f, 1f, 1000L);
                    gameNumpadPanel.setVisibility(View.VISIBLE);
                    gameNumpadPanel.setAlpha(0f);
                    gameNumpadPanel.bringToFront();
                    for (View button : gameNumpadButtons) {
                        button.setEnabled(true);
                        button.setClickable(true);
                    }
                    Shared.OpacityTo(gameNumpadPanel, 0f, 1f, 1000L);
                    startGameNumpadNeonPulse();
                    Shared.SoundPlay("game_music.mp3", 0f, true);
                    Shared.SoundFade("game_music.mp3", 1.0f, 1.0);
                    Shared.BlackFade(blackFadeOverlay, 1f, 0f, 1000L);
                }))
                .thenCompose(ignore -> delaySeconds(1.0))
                .thenCompose(ignore -> showStartCountdownValue("3"))
                .thenCompose(ignore -> delaySeconds(1.0))
                .thenCompose(ignore -> showStartCountdownValue("2"))
                .thenCompose(ignore -> delaySeconds(1.0))
                .thenCompose(ignore -> showStartCountdownValue("1"))
                .thenCompose(ignore -> delaySeconds(1.0))
                .thenCompose(ignore -> showStartCountdownValue("Start!"))
                .thenCompose(ignore -> delaySeconds(1.0))
                .thenCompose(ignore -> runOnUi(() -> {
                    Shared.OpacityTo(startText, 1f, 0f, 500L);
                }))
                .thenCompose(ignore -> delaySeconds(0.5))
                .thenCompose(ignore -> runOnUi(() -> {
                    startText.setVisibility(View.GONE);
                    startText.setAlpha(0f);
                    startText.setScaleX(0.6f);
                    startText.setScaleY(0.6f);
                    startGameTimer();
                }));
    }
    private CompletableFuture<Void> showStartCountdownValue(String value) {
        return runOnUi(() -> {
            startText.setText(value);
            startText.setTextColor(Color.WHITE);
            applyGameHudGlow(startText, Color.WHITE);
            startText.setVisibility(View.VISIBLE);
            startText.setAlpha(1f);
            startText.bringToFront();
        });
    }
    private void updateGameTimerDisplay() {
        int remainingSeconds = Math.max(0, Shared.sharedSave.inGameSeconds);
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        gameTimerText.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
        int color = remainingSeconds < 10
                ? Color.rgb(255, 125, 125)
                : Color.WHITE;
        if (gameTimerBonusHighlightActive) {
            color = GAME_TIMER_BONUS_COLOR;
        }
        gameTimerText.setTextColor(color);
        applyGameHudGlow(gameTimerText, color);
    }
    private void updateGamePointsDisplay() {
        gamePointsText.setText(String.valueOf(Math.min(99999, Math.max(0, Shared.sharedSave.inGamePoints))));
        gamePointsText.setTextColor(Color.WHITE);
        applyGameHudGlow(gamePointsText, Color.WHITE);
    }
    private void updateGameRangeDisplay() {
        gameRangeLowText.setText(Shared.sharedSave.inGameLowRange + "<");
        gameRangeHighText.setText("<" + Shared.sharedSave.inGameHighRange);
        for (TextView rangeText : new TextView[]{gameRangeLowText, gameRangeHighText}) {
            rangeText.setTextColor(GAME_ENTRY_GLOW_COLOR);
            applyGameHudGlow(rangeText, GAME_ENTRY_GLOW_COLOR);
        }
        gameEntryText.setTextColor(GAME_ENTRY_GLOW_COLOR);
        applyGameHudGlow(gameEntryText, GAME_ENTRY_GLOW_COLOR);
    }
    private void updateGameCarDisplay() {
        boolean secondCar = Shared.sharedSave.carSelected == 2;
        gameCarImage.setImageResource(secondCar ? R.drawable.car2 : R.drawable.car1);
        gameCarPlate.setText(Shared.sharedSave.getPlayerTableCode());
    }

    private void startGameCarBounce() {
        stopGameCarBounce();
        // Small, sharp jolts rather than a smooth wave-like suspension bounce.
        gameCarBounceAnimator = ValueAnimator.ofFloat(
                0f, -8f, -8f, 3f, 3f, -6f, -6f, 8f, 8f, -2f, -2f, 0f, 0f
        );
        gameCarBounceAnimator.setDuration(700L);
        gameCarBounceAnimator.setRepeatCount(ValueAnimator.INFINITE);
        gameCarBounceAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
        gameCarBounceAnimator.addUpdateListener(animator -> {
            gameCarDisplay.setTranslationY((Float) animator.getAnimatedValue());
        });
        gameCarBounceAnimator.start();
    }

    private void stopGameCarBounce() {
        if (gameCarBounceAnimator != null) {
            gameCarBounceAnimator.cancel();
            gameCarBounceAnimator = null;
        }
    }
    private void applyGameHudGlow(TextView textView, int color) {
        if (Shared.sharedSave.performanceModeEnabled) {
            textView.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
        } else {
            textView.setShadowLayer(34f, 0f, 0f, color);
        }
    }
    private void prepareGameNumpad() {
        gameNumpadPanel.setClipChildren(false);
        gameNumpadPanel.setClipToPadding(false);
        for (View button : gameNumpadButtons) {
            button.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            NeonGlowDrawable outline = new NeonGlowDrawable();
            outline.setGlowColor(Color.rgb(255, 45, 187));
            outline.setGlowRadius(36f);
            outline.setShadowEnabled(!Shared.sharedSave.performanceModeEnabled);
            button.setForeground(outline);
            gameNumpadNeonDrawables.add(outline);
        }
        gameNumpadButtons[0].setOnClickListener(view -> clicked7());
        gameNumpadButtons[1].setOnClickListener(view -> clicked8());
        gameNumpadButtons[2].setOnClickListener(view -> clicked9());
        gameNumpadButtons[3].setOnClickListener(view -> clicked4());
        gameNumpadButtons[4].setOnClickListener(view -> clicked5());
        gameNumpadButtons[5].setOnClickListener(view -> clicked6());
        gameNumpadButtons[6].setOnClickListener(view -> clicked1());
        gameNumpadButtons[7].setOnClickListener(view -> clicked2());
        gameNumpadButtons[8].setOnClickListener(view -> clicked3());
        gameNumpadButtons[9].setOnClickListener(view -> clickedenter());
        gameNumpadButtons[10].setOnClickListener(view -> clicked0());
    }
    private void startGameNumpadNeonPulse() {
        stopGameNumpadNeonPulse();
        int pink = Color.rgb(255, 45, 187);
        if (Shared.sharedSave.performanceModeEnabled) {
            for (NeonGlowDrawable drawable : gameNumpadNeonDrawables) {
                drawable.setGlowColor(pink);
                drawable.setGlowRadius(0f);
                drawable.setShadowEnabled(false);
            }
            return;
        }
        int violet = Color.rgb(157, 48, 255);
        int lightBlue = Color.rgb(78, 214, 255);
        gameNumpadNeonAnimator = ValueAnimator.ofObject(
                new ArgbEvaluator(),
                pink,
                violet,
                lightBlue,
                pink
        );
        gameNumpadNeonAnimator.setDuration(4000L);
        gameNumpadNeonAnimator.setRepeatCount(ValueAnimator.INFINITE);
        gameNumpadNeonAnimator.setInterpolator(
                new android.view.animation.LinearInterpolator()
        );
        gameNumpadNeonAnimator.addUpdateListener(animation -> {
            int color = (Integer) animation.getAnimatedValue();
            float progress = animation.getAnimatedFraction();
            float glowRadius = 36f + 12f * (float) Math.sin(progress * Math.PI * 2.0);
            for (NeonGlowDrawable drawable : gameNumpadNeonDrawables) {
                drawable.setGlowColor(color);
                drawable.setGlowRadius(glowRadius);
                drawable.setShadowEnabled(true);
            }
        });
        gameNumpadNeonAnimator.start();
    }
    private void stopGameNumpadNeonPulse() {
        if (gameNumpadNeonAnimator != null) {
            gameNumpadNeonAnimator.cancel();
            gameNumpadNeonAnimator = null;
        }
    }
    private void clicked7() { appendGameNumber('7'); }
    private void clicked8() { appendGameNumber('8'); }
    private void clicked9() { appendGameNumber('9'); }
    private void clicked4() { appendGameNumber('4'); }
    private void clicked5() { appendGameNumber('5'); }
    private void clicked6() { appendGameNumber('6'); }
    private void clicked1() { appendGameNumber('1'); }
    private void clicked2() { appendGameNumber('2'); }
    private void clicked3() { appendGameNumber('3'); }
    private void clicked0() { appendGameNumber('0'); }
    private void appendGameNumber(char digit) {
        if (gameFinishedShown
                || gameTimerRunnable == null
                || gameNumberInput.length() >= getGameInputMaxDigits()) {
            return;
        }
        gameNumberInput.append(digit);
        gameEntryText.setText(gameNumberInput);
        gameEntryText.setTextColor(GAME_ENTRY_GLOW_COLOR);
        applyGameHudGlow(gameEntryText, GAME_ENTRY_GLOW_COLOR);
    }
    private void clickedenter() {
        if (gameFinishedShown || gameTimerRunnable == null) {
            return;
        }
        if (gameNumberInput.length() == 0) {
            gameEntryText.setText("");
            Shared.sharedSave.inGamePointsEarned = 0;
            return;
        }
        long enteredNumber = Long.parseLong(
                gameNumberInput.toString()
        );
        gameNumberInput.setLength(0);
        gameEntryText.setText("");
        if (Shared.sharedSave.attemptsCounter < Integer.MAX_VALUE) {
            Shared.sharedSave.attemptsCounter++;
        }
        if (enteredNumber != Shared.sharedSave.numberToGuess) {
            Shared.sharedSave.inGamePointsEarned = 0;
            if (enteredNumber > Shared.sharedSave.numberToGuess) {
                int guessedUpperBound = (int) Math.min(
                        Integer.MAX_VALUE,
                        enteredNumber
                );
                Shared.sharedSave.inGameHighRange = Math.min(
                        Shared.sharedSave.inGameHighRange,
                        guessedUpperBound
                );
            } else {
                Shared.sharedSave.inGameLowRange = Math.max(
                        Shared.sharedSave.inGameLowRange,
                        (int) enteredNumber
                );
            }
            updateGameRangeDisplay();
            return;
        }
        long halfRange = Shared.sharedSave.highRange / 2L;
        long earnedPoints = (halfRange*2)
                * (halfRange - Shared.sharedSave.attemptsCounter);
        Shared.sharedSave.inGamePointsEarned = (int) Math.max(
                Integer.MIN_VALUE,
                Math.min(Integer.MAX_VALUE, earnedPoints)
        );
        increaseGameHighRange();
        generateNumberToGuess();
        if (Shared.sharedSave.inGamePointsEarned > 0) {
            long bonusSeconds = (long) Shared.sharedSave.inGameSeconds + 5L;
            Shared.sharedSave.inGameSeconds = (int) Math.min(
                    Integer.MAX_VALUE,
                    bonusSeconds
            );
            restartGameTimerBonusHighlight();
        }
        long updatedPoints = (long) Math.max(0, Shared.sharedSave.inGamePoints)
                + Shared.sharedSave.inGamePointsEarned;
        Shared.sharedSave.inGamePoints = (int) Math.max(
                0L,
                Math.min(Integer.MAX_VALUE, updatedPoints)
        );
        showCorrectGuessMessage(enteredNumber);
        showEarnedPointsPopup();
    }
    private void increaseGameHighRange() {
        if (Shared.sharedSave.carSelected == 2) {
            Shared.sharedSave.highRange = Shared.sharedSave.highRange
                    > Integer.MAX_VALUE / 2
                    ? Integer.MAX_VALUE
                    : Shared.sharedSave.highRange * 2;
        } else {
            Shared.sharedSave.highRange = Shared.sharedSave.highRange
                    > Integer.MAX_VALUE - 10
                    ? Integer.MAX_VALUE
                    : Shared.sharedSave.highRange + 10;
        }
        Shared.sharedSave.inGameLowRange = Shared.sharedSave.lowRange;
        Shared.sharedSave.inGameHighRange = Shared.sharedSave.highRange;
        updateGameRangeDisplay();
    }
    private void generateNumberToGuess() {
        Shared.sharedSave.attemptsCounter = 0;
        long minimum = (long) Shared.sharedSave.lowRange + 1L;
        long rangeSize = (long) Shared.sharedSave.highRange - minimum;
        if (rangeSize <= 0L) {
            Shared.sharedSave.numberToGuess = Shared.sharedSave.lowRange;
            return;
        }
        long offset = rangeSize <= Integer.MAX_VALUE
                ? gameplayRandom.nextInt((int) rangeSize)
                : (long) (gameplayRandom.nextDouble() * rangeSize);
        Shared.sharedSave.numberToGuess = (int) (minimum + offset);
    }
    private void showCorrectGuessMessage(long guessedNumber) {
        clearCorrectGuessMessage();
        startText.setText("Zgadłeś: " + guessedNumber);
        startText.setTextColor(Color.WHITE);
        applyGameHudGlow(startText, Color.WHITE);
        startText.setVisibility(View.VISIBLE);
        startText.setAlpha(1f);
        startText.bringToFront();
        correctGuessHideRunnable = () -> {
            correctGuessHideRunnable = null;
            startText.setVisibility(View.GONE);
            startText.setAlpha(0f);
        };
        mainHandler.postDelayed(correctGuessHideRunnable, 1000L);
    }
    private void clearCorrectGuessMessage() {
        if (correctGuessHideRunnable != null) {
            mainHandler.removeCallbacks(correctGuessHideRunnable);
            correctGuessHideRunnable = null;
        }
        startText.setVisibility(View.GONE);
        startText.setAlpha(0f);
    }
    private int getGameInputMaxDigits() {
        long absoluteHighRange = Math.abs((long) Shared.sharedSave.highRange);
        return Math.max(1, Long.toString(absoluteHighRange).length());
    }
    private void restartGameTimerBonusHighlight() {
        cancelGameTimerBonusHighlight();
        gameTimerBonusHighlightActive = true;
        updateGameTimerDisplay();
        gameTimerBonusRestoreRunnable = () -> {
            gameTimerBonusRestoreRunnable = null;
            gameTimerBonusHighlightActive = false;
            updateGameTimerDisplay();
        };
        mainHandler.postDelayed(gameTimerBonusRestoreRunnable, 1000L);
    }
    private void cancelGameTimerBonusHighlight() {
        gameTimerBonusHighlightActive = false;
        if (gameTimerBonusRestoreRunnable != null) {
            mainHandler.removeCallbacks(gameTimerBonusRestoreRunnable);
            gameTimerBonusRestoreRunnable = null;
        }
    }
    private void showEarnedPointsPopup() {
        cancelEarnedPointsPopup();
        int earnedPoints = Shared.sharedSave.inGamePointsEarned;
        gamePointsEarnedText.setText(earnedPoints >= 0 ? "+" + earnedPoints : String.valueOf(earnedPoints));
        gamePointsEarnedText.setTextColor(Color.WHITE);
        applyGameHudGlow(gamePointsEarnedText, Color.WHITE);
        gamePointsEarnedText.setVisibility(View.VISIBLE);
        Shared.OpacityTo(gamePointsEarnedText, 0f, 1f, 500L);
        if (earnedPoints > 0) {
            showGameCarShadowEffect();
        }
        earnedPointsFadeOutRunnable = () -> {
            earnedPointsFadeOutRunnable = null;
            Shared.OpacityTo(
                    gamePointsEarnedText,
                    gamePointsEarnedText.getAlpha(),
                    0f,
                    500L
            );
            earnedPointsRestoreRunnable = () -> {
                earnedPointsRestoreRunnable = null;
                gamePointsEarnedText.setVisibility(View.GONE);
                gamePointsEarnedText.setAlpha(0f);
            };
            mainHandler.postDelayed(earnedPointsRestoreRunnable, 500L);
        };
        mainHandler.postDelayed(earnedPointsFadeOutRunnable, 1500L);
    }
    private void cancelEarnedPointsPopup() {
        if (earnedPointsFadeOutRunnable != null) {
            mainHandler.removeCallbacks(earnedPointsFadeOutRunnable);
            earnedPointsFadeOutRunnable = null;
        }
        if (earnedPointsRestoreRunnable != null) {
            mainHandler.removeCallbacks(earnedPointsRestoreRunnable);
            earnedPointsRestoreRunnable = null;
        }
    }
    private void showGameCarShadowEffect() {
        cancelGameCarShadowEffect();
        gameCarShadow.setVisibility(View.VISIBLE);
        Shared.OpacityTo(gameCarShadow, 0f, 0.7f, 500L);
        carShadowFadeOutRunnable = () -> {
            carShadowFadeOutRunnable = null;
            Shared.OpacityTo(gameCarShadow, gameCarShadow.getAlpha(), 0f, 1000L);
            carShadowHideRunnable = () -> {
                carShadowHideRunnable = null;
                gameCarShadow.setVisibility(View.GONE);
                gameCarShadow.setAlpha(0f);
            };
            mainHandler.postDelayed(carShadowHideRunnable, 1000L);
        };
        mainHandler.postDelayed(carShadowFadeOutRunnable, 2500L);
    }
    private void cancelGameCarShadowEffect() {
        if (carShadowFadeOutRunnable != null) {
            mainHandler.removeCallbacks(carShadowFadeOutRunnable);
            carShadowFadeOutRunnable = null;
        }
        if (carShadowHideRunnable != null) {
            mainHandler.removeCallbacks(carShadowHideRunnable);
            carShadowHideRunnable = null;
        }
    }
    private void startGameTimer() {
        if (Shared.sharedSave.inGameSeconds <= 0) {
            GameFinished();
            return;
        }
        gameTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Shared.sharedSave.inGameSeconds = Math.max(
                        0,
                        Shared.sharedSave.inGameSeconds - 1
                );
                updateGameTimerDisplay();
                updateGamePointsDisplay();
                if (Shared.sharedSave.inGameSeconds == 0) {
                    GameFinished();
                } else {
                    mainHandler.postDelayed(this, 1000L);
                }
            }
        };
        mainHandler.postDelayed(gameTimerRunnable, 1000L);
    }
    private void GameFinished() {
        if (gameFinishedShown) {
            return;
        }
        gameFinishedShown = true;
        stopGameCarBounce();
        clearCorrectGuessMessage();
        cancelGameTimerBonusHighlight();
        updateGameTimerDisplay();
        if (gameTimerRunnable != null) {
            mainHandler.removeCallbacks(gameTimerRunnable);
            gameTimerRunnable = null;
        }
        cancelEarnedPointsPopup();
        cancelGameCarShadowEffect();
        Shared.OpacityTo(gameCarShadow, gameCarShadow.getAlpha(), 0f, 500L);
        Shared.OpacityTo(
                gamePointsEarnedText,
                gamePointsEarnedText.getAlpha(),
                0f,
                500L
        );
        int finalPoints = Math.max(0, Shared.sharedSave.inGamePoints);
        boolean newRecord = finalPoints > Shared.sharedSave.points;
        if (newRecord) {
            Shared.sharedSave.points = finalPoints;
            Shared.SaveSharedSave(this);
        }
        String resultMessage = newRecord
                ? "Nowy Rekord: " + finalPoints
                : "Punkty: " + finalPoints;
        int resultGlowColor = Color.rgb(255, 221, 74);
        gameResultsText.setText(resultMessage);
        gameResultsText.setTextColor(resultGlowColor);
        applyGameHudGlow(gameResultsText, resultGlowColor);
        gameResultsOutline.setShadowEnabled(!Shared.sharedSave.performanceModeEnabled);
        gameResultsPanel.setVisibility(View.VISIBLE);
        gameResultsPanel.setAlpha(0f);
        gameResultsPanel.bringToFront();
        stopGameNumpadNeonPulse();
        for (View button : gameNumpadButtons) {
            button.setEnabled(false);
            button.setClickable(false);
        }
        Shared.OpacityTo(gameTimerText, 1f, 0f, 500L);
        Shared.OpacityTo(gamePointsText, 1f, 0f, 500L);
        Shared.OpacityTo(gameRangeRow, gameRangeRow.getAlpha(), 0f, 500L);
        Shared.OpacityTo(gameCarDisplay, 1f, 0f, 500L);
        Shared.OpacityTo(gameNumpadPanel, 1f, 0f, 500L);
        Shared.OpacityTo(gameResultsPanel, 0f, 1f, 500L);
        finishGameSequence();
    }
    private void finishGameSequence() {
        if (finishGameReturnSequence != null) {
            finishGameReturnSequence.cancel(true);
        }
        finishGameReturnSequence = delaySeconds(5.5)
                .thenCompose(ignore -> runOnUi(() -> {
                    menuReadyFlag = false;
                    if (menuNeonAnimator != null) {
                        menuNeonAnimator.cancel();
                        menuNeonAnimator = null;
                    }
                    float density = getResources().getDisplayMetrics().density;
                    blackFadeOverlay.setElevation(1600f * density);
                    blackFadeOverlay.bringToFront();
                    Shared.BlackFade(blackFadeOverlay, 0f, 1f, 1000L);
                    blackFadeOverlay.setFocusableInTouchMode(true);
                    blackFadeOverlay.requestFocus();
                    Shared.SoundFade("game_music.mp3", 0f, 1.0);
                }))
                .thenCompose(ignore -> delaySeconds(1.0))
                .thenCompose(ignore -> runOnUi(() -> {
                    Shared.SoundStop("game_music.mp3");
                    Shared.SoundStop("theme_next.mp3");
                    gameTimerText.setVisibility(View.GONE);
                    gamePointsText.setVisibility(View.GONE);
                    gamePointsEarnedText.setVisibility(View.GONE);
                    gamePointsEarnedText.setAlpha(0f);
                    gameRangeRow.setVisibility(View.GONE);
                    gameEntryText.setText("");
                    gameCarDisplay.setVisibility(View.GONE);
                    gameCarDisplay.setTranslationY(0f);
                    gameCarShadow.setVisibility(View.GONE);
                    gameCarShadow.setAlpha(0f);
                    gameNumpadPanel.setVisibility(View.GONE);
                    startText.setVisibility(View.GONE);
                    gameResultsPanel.setVisibility(View.GONE);
                    ChangeVideo("intro_ext.mp4");
                    menuContainer.setVisibility(View.VISIBLE);
                    menuContainer.setAlpha(0f);
                    if (Shared.sharedSave.points > 1) {
                        menuRankings.setAlpha(1f);
                    }
                    Shared.SoundPlay("theme_next.mp3", 0f, true);
                    Shared.SoundFade("theme_next.mp3", 1f, 1.0);
                    Shared.OpacityTo(menuContainer, 0f, 1f, 1000L);
                    Shared.BlackFade(blackFadeOverlay, 1f, 0f, 1000L);
                    startMenuNeonPulse();
                }))
                .thenCompose(ignore -> delaySeconds(1.0))
                .thenCompose(ignore -> runOnUi(() -> {
                    blackFadeOverlay.setElevation(
                            1300f * getResources().getDisplayMetrics().density
                    );
                    gameTransitionStarted = false;
                    menuReadyFlag = true;
                }));
    }
    private void ClickedGarage() {
        updateGarageCar();
        garagePlate.setText(Shared.sharedSave.getPlayerTableCode());
        garageOverlay.setVisibility(View.VISIBLE);
        garageOverlay.setClickable(true);
        garageOverlay.setFocusable(true);
        garageOverlay.bringToFront();
        garageBack.requestFocus();
    }
    private void ClickedGarageBack() {
        hideGaragePlateKeyboard();
        Shared.SaveSharedSave(this);
        garageOverlay.setVisibility(View.GONE);
        garageOverlay.setClickable(false);
        garageOverlay.setFocusable(false);
    }
    private void selectOtherGarageCar() {
        hideGaragePlateKeyboard();
        Shared.sharedSave.carSelected = Shared.sharedSave.carSelected == 1 ? 2 : 1;
        updateGarageCar();
    }
    private void configureGaragePlateInput() {
        garagePlate.setTypeface(getResources().getFont(R.font.plate));
        garagePlate.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
            int remainingCharacters = 5 - (dest.length() - (dend - dstart));
            StringBuilder accepted = new StringBuilder();
            for (int i = start; i < end && accepted.length() < remainingCharacters; i++) {
                char character = Character.toUpperCase(source.charAt(i));
                if ((character >= 'A' && character <= 'Z')
                        || (character >= '0' && character <= '9')) {
                    accepted.append(character);
                }
            }
            String original = source.subSequence(start, end).toString();
            return original.contentEquals(accepted) ? null : accepted.toString();
        }});
        garagePlate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                Shared.sharedSave.playerTable = text.toString();
            }
            @Override
            public void afterTextChanged(Editable text) {
            }
        });
        garagePlate.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideGaragePlateKeyboard();
                return true;
            }
            return false;
        });
    }
    private void hideGaragePlateKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && garagePlate.getWindowToken() != null) {
            inputMethodManager.hideSoftInputFromWindow(garagePlate.getWindowToken(), 0);
        }
        garagePlate.clearFocus();
    }
    private void updateGarageCar() {
        boolean firstCar = Shared.sharedSave.carSelected != 2;
        Shared.sharedSave.carSelected = firstCar ? 1 : 2;
        garageCarName.setText(
                firstCar ? Shared.sharedSave.car1Name : Shared.sharedSave.car2Name
        );
        garageCarMode.setText(
                firstCar ? R.string.garage_standard_mode : R.string.garage_hard_mode
        );
        garageCarImage.setImageResource(firstCar ? R.drawable.car1 : R.drawable.car2);
    }
    private void ClickedRankTable() {
        if (Shared.sharedSave.points <= 1) {
            return;
        }
        showRankings();
    }
    private void ClickedSettings() {
        showSettings();
    }
    private void showSettings() {
        updateSoundButtonLabel();
        updatePerformanceButtonLabel();
        settingsOverlay.setVisibility(View.VISIBLE);
        settingsOverlay.setClickable(true);
        settingsOverlay.setFocusable(true);
        settingsOverlay.bringToFront();
        settingsBack.requestFocus();
    }
    private void hideSettings() {
        settingsOverlay.setVisibility(View.GONE);
        settingsOverlay.setClickable(false);
        settingsOverlay.setFocusable(false);
    }
    private void showRankings() {
        populateRankings();
        rankingsOverlay.setVisibility(View.VISIBLE);
        rankingsOverlay.setClickable(true);
        rankingsOverlay.setFocusable(true);
        rankingsOverlay.bringToFront();
        rankingsBack.requestFocus();
    }
    private void hideRankings() {
        rankingsOverlay.setVisibility(View.GONE);
        rankingsOverlay.setClickable(false);
        rankingsOverlay.setFocusable(false);
    }
    private void populateRankings() {
        ArrayList<RankingEntry> rankingEntries = getCachedRankingJsonEntries();
        String playerName = Shared.sharedSave.playerName;
        if (playerName == null
                || playerName.trim().isEmpty()
                || "null".equalsIgnoreCase(playerName.trim())) {
            playerName = getString(R.string.rankings_default_player);
        } else {
            playerName = playerName.trim();
        }
        rankingEntries.add(new RankingEntry(
                playerName,
                Shared.sharedSave.getPlayerTableCode(),
                Shared.sharedSave.points,
                "main_profile",
                Shared.LoadProfilePhoto(this),
                true
        ));
        Collections.sort(
                rankingEntries,
                (first, second) -> Integer.compare(second.points, first.points)
        );
        rankingsRowsContainer.removeAllViews();
        rankingsNeonTextViews.clear();
        rankingsNeonDrawables.clear();
        prepareRankingsNeonEffect();
        for (int i = 0; i < rankingEntries.size(); i++) {
            RankingEntry entry = rankingEntries.get(i);
            View row = getLayoutInflater().inflate(
                    R.layout.item_ranking_row,
                    rankingsRowsContainer,
                    false
            );
            TextView number = row.findViewById(R.id.ranking_row_number);
            CircleImageView image = row.findViewById(R.id.ranking_row_image);
            TextView name = row.findViewById(R.id.ranking_row_name);
            TextView divider = row.findViewById(R.id.ranking_row_divider);
            TextView points = row.findViewById(R.id.ranking_row_points);
            if (entry.isCurrentPlayer) {
                row.setBackgroundResource(R.drawable.ranking_player_row_highlight);
            }
            number.setText(String.valueOf(i + 1));
            name.setText(formatRankingName(entry.name, entry.table));
            points.setText(String.valueOf(entry.points));
            if (entry.profilePhoto != null) {
                image.setImageBitmap(entry.profilePhoto);
            } else {
                image.setImageResource(resolveRankingIcon(entry.icon));
            }
            addRankingNeonText(number);
            addRankingNeonText(name);
            addRankingNeonText(divider);
            addRankingNeonText(points);
            attachRankingsNeonOutline(row);
            rankingsRowsContainer.addView(row);
        }
        updateNeonAppearance(
                activeNeonColor,
                activeNeonGlowRadius,
                activeNeonShadowEnabled
        );
    }
    private ArrayList<RankingEntry> getCachedRankingJsonEntries() {
        synchronized (MainActivity.class) {
            if (cachedRankingJsonEntries == null) {
                cachedRankingJsonEntries = loadRankingEntries();
            }
            return new ArrayList<>(cachedRankingJsonEntries);
        }
    }
    private ArrayList<RankingEntry> loadRankingEntries() {
        ArrayList<RankingEntry> entries = new ArrayList<>();
        StringBuilder jsonText = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getResources().openRawResource(R.raw.ranking),
                StandardCharsets.UTF_8
        ))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonText.append(line);
            }
            JSONArray jsonEntries = new JSONArray(jsonText.toString());
            for (int i = 0; i < jsonEntries.length(); i++) {
                JSONObject jsonEntry = jsonEntries.optJSONObject(i);
                if (jsonEntry == null) {
                    continue;
                }
                String name = jsonEntry.optString(
                        "name",
                        getString(R.string.rankings_default_player)
                ).trim();
                if (name.isEmpty()) {
                    name = getString(R.string.rankings_default_player);
                }
                entries.add(new RankingEntry(
                        name,
                        normalizeRankingTable(jsonEntry.optString("table", "")),
                        jsonEntry.optInt("points", 0),
                        jsonEntry.optString("icon", "main_profile"),
                        null,
                        false
                ));
            }
        } catch (IOException | JSONException exception) {
            Log.e(TAG, "Could not load res/raw/ranking.json", exception);
        }
        return entries;
    }
    private String formatRankingName(String name, String table) {
        return table == null || table.isEmpty()
                ? name
                : name + " (" + table + ")";
    }
    private String normalizeRankingTable(String table) {
        String normalized = table == null ? "" : table.trim().toUpperCase(Locale.US);
        return normalized.length() > 5 ? normalized.substring(0, 5) : normalized;
    }
    private int resolveRankingIcon(String iconFileName) {
        String resourceName = iconFileName == null ? "" : iconFileName.trim();
        int extensionIndex = resourceName.lastIndexOf('.');
        if (extensionIndex > 0) {
            resourceName = resourceName.substring(0, extensionIndex);
        }
        int resourceId = getResources().getIdentifier(
                resourceName.toLowerCase(Locale.US),
                "drawable",
                getPackageName()
        );
        return resourceId == 0 ? R.drawable.main_profile : resourceId;
    }
    private static final class RankingEntry {
        final String name;
        final String table;
        final int points;
        final String icon;
        final android.graphics.Bitmap profilePhoto;
        final boolean isCurrentPlayer;
        RankingEntry(
                String name,
                String table,
                int points,
                String icon,
                android.graphics.Bitmap profilePhoto,
                boolean isCurrentPlayer
        ) {
            this.name = name;
            this.table = table;
            this.points = points;
            this.icon = icon;
            this.profilePhoto = profilePhoto;
            this.isCurrentPlayer = isCurrentPlayer;
        }
    }
    private void toggleSoundSetting() {
        boolean soundEnabled = !Shared.sharedSave.soundEffectsEnabled;
        Shared.SetSoundEffectsEnabled(soundEnabled);
        Shared.SaveSharedSave(this);
        updateSoundButtonLabel();
    }
    private void togglePerformanceSetting() {
        Shared.sharedSave.performanceModeEnabled =
                !Shared.sharedSave.performanceModeEnabled;
        Shared.SaveSharedSave(this);
        updatePerformanceButtonLabel();
        applyPerformanceNeonMode();
    }
    private void updateSoundButtonLabel() {
        settingsSound.setText(
                Shared.sharedSave.soundEffectsEnabled
                        ? R.string.settings_sound_enabled
                        : R.string.settings_sound_disabled
        );
    }
    private void updatePerformanceButtonLabel() {
        settingsPerformance.setText(
                Shared.sharedSave.performanceModeEnabled
                        ? R.string.settings_performance_enabled
                        : R.string.settings_performance_disabled
        );
    }
    private void showResetProfileConfirmation() {
        hideSettings();
        exitConfirmationText.setText(R.string.reset_profile_confirmation);
        exitConfirmationYes.setOnClickListener(view -> confirmProfileReset());
        exitConfirmationNo.setOnClickListener(view -> cancelProfileReset());
        showConfirmationOverlay();
    }
    private void cancelProfileReset() {
        hideExitConfirmation();
        showSettings();
    }
    private void ShowCredits() {
        if (creditsSequence != null) {
            creditsSequence.cancel(true);
        }
        if (gameTimerRunnable != null) {
            mainHandler.removeCallbacks(gameTimerRunnable);
            gameTimerRunnable = null;
        }
        creditsTouchOverlay.setVisibility(View.VISIBLE);
        creditsContent.setVisibility(View.VISIBLE);
        creditsContent.setClickable(true);
        Shared.PositionTo(creditsContent, 960f, 1650f, 0L);
        Shared.OpacityTo(creditsContent, 0f, 0f, 0L);
        Shared.BlackFade(blackFadeOverlay, 0f, 0.65f, 1000L);
        creditsSequence = delaySeconds(1.0)
                .thenCompose(ignore -> runOnUi(() -> {
                    Shared.OpacityTo(creditsContent, 0f, 1f, 1000L);
                    Shared.PositionTo(creditsContent, 960f, -570f, 15300L);
                }))
                .thenCompose(ignore -> delaySeconds(15.3))
                .thenCompose(ignore -> runOnUi(this::finishCredits));
    }
    private void finishCredits() {
        Shared.BlackFade(blackFadeOverlay, 0.65f, 0f, 1000L);
        Shared.OpacityTo(creditsContent, 1f, 0f, 1000L);
        mainHandler.postDelayed(() -> {
            creditsContent.setVisibility(View.GONE);
            creditsTouchOverlay.setVisibility(View.GONE);
            creditsContent.setClickable(false);
            settingsOverlay.setVisibility(View.VISIBLE);
        }, 1000L);
    }
    private void prepareSettingsNeonEffect() {
        addSettingsNeonText(settingsTitle);
        addSettingsNeonText(settingsSound);
        addSettingsNeonText(settingsPerformance);
        addSettingsNeonText(settingsReset);
        addSettingsNeonText(settingsAuthors);
        attachSettingsNeonOutline(settingsPanel);
        attachSettingsNeonOutline(settingsSound);
        attachSettingsNeonOutline(settingsPerformance);
        attachSettingsNeonOutline(settingsReset);
        attachSettingsNeonOutline(settingsAuthors);
        settingsBack.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    private void prepareRankingsNeonEffect() {
        rankingsNeonTextViews.add(rankingsHeaderNumber);
        rankingsNeonTextViews.add(rankingsHeaderName);
        rankingsNeonTextViews.add(rankingsHeaderPoints);
        for (TextView textView : rankingsNeonTextViews) {
            textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        attachRankingsNeonOutline(rankingsPanel);
        rankingsBack.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    private void addRankingNeonText(TextView textView) {
        textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        rankingsNeonTextViews.add(textView);
    }
    private void attachRankingsNeonOutline(View view) {
        NeonGlowDrawable glowDrawable = new NeonGlowDrawable();
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        view.setForeground(glowDrawable);
        rankingsNeonDrawables.add(glowDrawable);
    }
    private void prepareGarageNeonEffect() {
        garageCarName.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        garageCarMode.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        garageNeonTextViews.add(garageCarName);
        garageNeonTextViews.add(garageCarMode);
        NeonGlowDrawable panelOutline = new NeonGlowDrawable();
        garagePanel.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        garagePanel.setForeground(panelOutline);
        garageNeonDrawables.add(panelOutline);
        garageBack.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        garagePreviousCar.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        garageNextCar.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    private void addSettingsNeonText(TextView textView) {
        textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        settingsNeonTextViews.add(textView);
    }
    private void attachSettingsNeonOutline(View view) {
        NeonGlowDrawable glowDrawable = new NeonGlowDrawable();
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        view.setForeground(glowDrawable);
        settingsNeonDrawables.add(glowDrawable);
    }
    private void ClickedExit() {
        Shared.SaveSharedSave(this);
        showExitConfirmation();
    }
    private void showExitConfirmation() {
        exitConfirmationText.setText(R.string.exit_confirmation);
        exitConfirmationYes.setOnClickListener(view -> confirmExit());
        exitConfirmationNo.setOnClickListener(view -> hideExitConfirmation());
        showConfirmationOverlay();
    }
    private void showConfirmationOverlay() {
        exitConfirmationOverlay.setVisibility(View.VISIBLE);
        exitConfirmationOverlay.setClickable(true);
        exitConfirmationOverlay.setFocusable(true);
        exitConfirmationOverlay.bringToFront();
        exitConfirmationYes.requestFocus();
    }
    private void hideExitConfirmation() {
        exitConfirmationOverlay.setVisibility(View.GONE);
        exitConfirmationOverlay.setClickable(false);
        exitConfirmationOverlay.setFocusable(false);
    }
    private void confirmExit() {
        Shared.SoundStopAll();
        finishAffinity();
    }
    private void confirmProfileReset() {
        Shared.sharedSave.resetToDefaults();
        Shared.SaveSharedSave(this);
        Shared.SoundStopAll();
        Intent restartIntent = getPackageManager().getLaunchIntentForPackage(
                getPackageName()
        );
        if (restartIntent != null) {
            restartIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );
            startActivity(restartIntent);
        } else {
            finishAffinity();
        }
    }
    private void prepareExitConfirmationNeonEffect() {
        exitConfirmationText.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        exitConfirmationText.setShadowLayer(30f, 0f, 0f, Color.rgb(255, 45, 187));
        exitConfirmationNeonTextViews.add(exitConfirmationText);
        attachExitConfirmationNeonOutline(exitConfirmationDialog);
        attachExitConfirmationNeonOutline(exitConfirmationYes);
        attachExitConfirmationNeonOutline(exitConfirmationNo);
    }
    private void attachExitConfirmationNeonOutline(View view) {
        NeonGlowDrawable glowDrawable = new NeonGlowDrawable();
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        view.setForeground(glowDrawable);
        exitConfirmationNeonDrawables.add(glowDrawable);
    }
    private void prepareMenuNeonEffect(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            menuNeonTextViews.add(textView);
            if (view instanceof android.widget.Button) {
                attachMenuNeonOutline(view);
            }
        } else if (!(view instanceof ImageView)
                && view != menuContainer
                && view.getId() != R.id.menu_actions) {
            attachMenuNeonOutline(view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                prepareMenuNeonEffect(group.getChildAt(index));
            }
        }
    }
    private void attachMenuNeonOutline(View view) {
        NeonGlowDrawable glowDrawable = new NeonGlowDrawable();
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        view.setForeground(glowDrawable);
        menuNeonDrawables.add(glowDrawable);
    }
    private void applyPerformanceNeonMode() {
        if (menuNeonAnimator != null) {
            menuNeonAnimator.cancel();
            menuNeonAnimator = null;
        }
        if (Shared.sharedSave.performanceModeEnabled) {
            updateNeonAppearance(PERFORMANCE_NEON_COLOR, 0f, false);
        } else {
            updateNeonAppearance(PERFORMANCE_NEON_COLOR, 36f, true);
            startMenuNeonPulse();
        }
    }
    private void updateNeonAppearance(
            int color,
            float glowRadius,
            boolean shadowEnabled
    ) {
        activeNeonColor = color;
        activeNeonGlowRadius = glowRadius;
        activeNeonShadowEnabled = shadowEnabled;
        for (TextView textView : menuNeonTextViews) {
            updateNeonText(textView, color, glowRadius, shadowEnabled);
        }
        for (NeonGlowDrawable drawable : menuNeonDrawables) {
            updateNeonDrawable(drawable, color, glowRadius, shadowEnabled);
        }
        for (TextView textView : exitConfirmationNeonTextViews) {
            updateNeonText(textView, color, glowRadius, shadowEnabled);
        }
        for (NeonGlowDrawable drawable : exitConfirmationNeonDrawables) {
            updateNeonDrawable(drawable, color, glowRadius, shadowEnabled);
        }
        for (TextView textView : settingsNeonTextViews) {
            updateNeonText(textView, color, glowRadius, shadowEnabled);
        }
        for (NeonGlowDrawable drawable : settingsNeonDrawables) {
            updateNeonDrawable(drawable, color, glowRadius, shadowEnabled);
        }
        for (TextView textView : rankingsNeonTextViews) {
            updateNeonText(textView, color, glowRadius, shadowEnabled);
        }
        for (NeonGlowDrawable drawable : rankingsNeonDrawables) {
            updateNeonDrawable(drawable, color, glowRadius, shadowEnabled);
        }
        for (TextView textView : garageNeonTextViews) {
            updateNeonText(textView, color, glowRadius, shadowEnabled);
        }
        for (NeonGlowDrawable drawable : garageNeonDrawables) {
            updateNeonDrawable(drawable, color, glowRadius, shadowEnabled);
        }
        settingsBack.setGlowColor(color);
        settingsBack.setGlowRadius(glowRadius);
        settingsBack.setShadowEnabled(shadowEnabled);
        rankingsBack.setGlowColor(color);
        rankingsBack.setGlowRadius(glowRadius);
        rankingsBack.setShadowEnabled(shadowEnabled);
        garageBack.setGlowColor(color);
        garageBack.setGlowRadius(glowRadius);
        garageBack.setShadowEnabled(shadowEnabled);
        garagePreviousCar.setGlowColor(color);
        garagePreviousCar.setGlowRadius(glowRadius);
        garagePreviousCar.setShadowEnabled(shadowEnabled);
        garageNextCar.setGlowColor(color);
        garageNextCar.setGlowRadius(glowRadius);
        garageNextCar.setShadowEnabled(shadowEnabled);
    }
    private void updateNeonText(
            TextView textView,
            int color,
            float glowRadius,
            boolean shadowEnabled
    ) {
        if (shadowEnabled) {
            textView.setShadowLayer(glowRadius, 0f, 0f, color);
        } else {
            textView.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
        }
    }
    private void updateNeonDrawable(
            NeonGlowDrawable drawable,
            int color,
            float glowRadius,
            boolean shadowEnabled
    ) {
        drawable.setGlowColor(color);
        drawable.setGlowRadius(glowRadius);
        drawable.setShadowEnabled(shadowEnabled);
    }
    private void startMenuNeonPulse() {
        if (menuNeonAnimator != null) {
            menuNeonAnimator.cancel();
            menuNeonAnimator = null;
        }
        if (Shared.sharedSave.performanceModeEnabled) {
            updateNeonAppearance(PERFORMANCE_NEON_COLOR, 0f, false);
            return;
        }
        int pink = Color.rgb(255, 45, 187);
        int violet = Color.rgb(157, 48, 255);
        int lightBlue = Color.rgb(78, 214, 255);
        menuNeonAnimator = ValueAnimator.ofObject(
                new ArgbEvaluator(),
                pink,
                violet,
                lightBlue,
                pink
        );
        menuNeonAnimator.setDuration(4000L);
        menuNeonAnimator.setRepeatCount(ValueAnimator.INFINITE);
        menuNeonAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
        menuNeonAnimator.addUpdateListener(animation -> {
            int color = (Integer) animation.getAnimatedValue();
            float progress = animation.getAnimatedFraction();
            float glowRadius = 36f + 12f * (float) Math.sin(progress * Math.PI * 2.0);
            updateNeonAppearance(color, glowRadius, true);
        });
        menuNeonAnimator.start();
    }
    private CompletableFuture<Void> delaySeconds(double seconds) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        long milliseconds;
        if (Shared.sharedSave.devMode == true) {
            milliseconds = 0;
        } else {
            milliseconds = Math.round(seconds * 1000.0);
        }
        mainHandler.postDelayed(() -> future.complete(null), milliseconds);
        return future;
    }
    private CompletableFuture<Void> delaySecondsUnconditionally(double seconds) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        long milliseconds = Math.max(0L, Math.round(seconds * 1000.0));
        mainHandler.postDelayed(() -> future.complete(null), milliseconds);
        return future;
    }
    private CompletableFuture<Void> runOnUi(Runnable action) {
        return CompletableFuture.runAsync(action, uiExecutor);
    }
    @Override
    protected void onDestroy() {
        if (introSequence != null) {
            introSequence.cancel(true);
        }
        if (gameSequence != null) {
            gameSequence.cancel(true);
        }
        if (finishGameReturnSequence != null) {
            finishGameReturnSequence.cancel(true);
        }
        if (creditsSequence != null) {
            creditsSequence.cancel(true);
        }
        if (menuNeonAnimator != null) {
            menuNeonAnimator.cancel();
            menuNeonAnimator = null;
        }
        stopGameNumpadNeonPulse();
        stopGameCarBounce();
        mainHandler.removeCallbacksAndMessages(null);
        releaseBackgroundPlayer();
        if (videoSurface != null) {
            videoSurface.release();
            videoSurface = null;
        }
        super.onDestroy();
    }
}
