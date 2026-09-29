package com.example.horizonretrowave;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.view.View;
import android.view.ViewParent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Shared UI helpers used by both activities and by future screens.
 *
 * The logical coordinates are the coordinates of SceneHost: 1920 x 1080.
 * Animation durations are expressed in milliseconds.
 *
 * Java does not support extension methods on Android's View class. The
 * element(View) wrapper gives a similar, chainable calling style:
 *
 * Shared.element(button)
 *         .PositionTo(960, 540, 500)
 *         .ScaleTo(1f, 2f, 500)
 *         .OpacityTo(1f, 0f, 300);
 */
public final class Shared {

    private static final String TAG = "Shared";
    private static final String SAVE_FILE_NAME = "HorizonSave.dat";
    private static final String DEFAULT_PROFILE_PHOTO = "main_profile";
    private static final String PROFILE_PHOTO_FILE_NAME = "profile_photo.jpg";

    private static final Map<View, ValueAnimator> SCALE_ANIMATORS = new WeakHashMap<>();
    private static final Map<View, ValueAnimator> OPACITY_ANIMATORS = new WeakHashMap<>();
    private static final Map<View, ValueAnimator> BLACK_FADE_ANIMATORS = new WeakHashMap<>();
    private static final Map<View, ValueAnimator> POSITION_ANIMATORS = new WeakHashMap<>();
    private static final Map<String, SoundInstance> SOUND_INSTANCES = new HashMap<>();

    private static Context applicationContext;

    /**
     * Global save/state object shared by Shared.java, StartActivity.java and
     * MainActivity.java.
     *
     * Usage in any Activity or helper:
     *
     * if (Shared.sharedSave.points >= 100) {
     *     // unlock something
     * }
     * Shared.sharedSave.points += 10;
     * Shared.sharedSave.musicEnabled = false;
     * Shared.SaveSharedSave(this);
     *
     * LoadSharedSave() is called from StartActivity when the application
     * starts, so the same values are then available to MainActivity.
     */
    public static final SharedSave sharedSave = new SharedSave();

    private Shared() {
        // Utility class; do not create instances.
    }

    public static Element element(View view) {
        if (view == null) {
            throw new IllegalArgumentException("The UI element cannot be null.");
        }
        return new Element(view);
    }

    /**
     * Stores the application context used by the global sound functions.
     * Activities should call this once during onCreate().
     */
    public static synchronized void Initialize(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null.");
        }
        applicationContext = context.getApplicationContext();
    }

    /**
     * Saves the current profile photo in the application's private storage.
     * SharedSave.playerPhoto is changed only after the image was written
     * successfully; SaveSharedSave() should be called afterwards to persist
     * that identifier in HorizonSave.dat.
     */
    public static synchronized boolean SaveProfilePhoto(
            Context context,
            Bitmap profilePhoto
    ) {
        if (context == null || profilePhoto == null) {
            return false;
        }

        Context appContext = context.getApplicationContext();
        try (FileOutputStream output = appContext.openFileOutput(
                PROFILE_PHOTO_FILE_NAME,
                Context.MODE_PRIVATE
        )) {
            boolean compressed = profilePhoto.compress(
                    Bitmap.CompressFormat.JPEG,
                    92,
                    output
            );
            if (compressed) {
                sharedSave.playerPhoto = PROFILE_PHOTO_FILE_NAME;
            }
            return compressed;
        } catch (IOException | RuntimeException exception) {
            Log.e(TAG, "Could not save profile photo.", exception);
            return false;
        }
    }

    /**
     * Loads the saved custom profile photo. A null result means that the
     * default drawable should be used instead.
     */
    public static synchronized Bitmap LoadProfilePhoto(Context context) {
        if (context == null
                || !PROFILE_PHOTO_FILE_NAME.equals(sharedSave.playerPhoto)) {
            return null;
        }

        File photoFile = new File(
                context.getApplicationContext().getFilesDir(),
                PROFILE_PHOTO_FILE_NAME
        );
        if (!photoFile.isFile()) {
            sharedSave.playerPhoto = DEFAULT_PROFILE_PHOTO;
            return null;
        }

        return BitmapFactory.decodeFile(photoFile.getAbsolutePath());
    }

    /**
     * Starts an audio resource from app/src/main/res/raw/.
     *
     * Examples:
     * Shared.SoundPlay("intro", 1.0f);
     * Shared.SoundPlay("intro.mp3", 0.5f);
     * Shared.SoundPlay("menu.mp3", 0.7f, true);
     *
     * The extension is optional. Volume is in the range 0..1. The two-
     * argument overload plays once; the three-argument overload can loop
     * until SoundStop("menu.mp3") is called. The four-argument overload
     * can run a callback after one-shot playback ends.
     */
    public static void SoundPlay(
            String fileName,
            float volume
    ) {
        SoundPlay(fileName, volume, false);
    }

    /**
     * Starts an audio resource, optionally looping it indefinitely.
     *
     * A looping sound stays registered in Shared.SOUND_INSTANCES until
     * SoundStop(fileName) or SoundStopAll() is called.
     */
    public static synchronized void SoundPlay(
            String fileName,
            float volume,
            boolean isLooping
    ) {
        SoundPlay(fileName, volume, isLooping, null);
    }

    /** Starts a sound and optionally runs an action after one-shot playback ends. */
    public static synchronized void SoundPlay(
            String fileName,
            float volume,
            boolean isLooping,
            Runnable onCompletion
    ) {
        Context context = requireApplicationContext();
        String resourceName = normalizeRawResourceName(fileName);
        int resourceId = context.getResources().getIdentifier(
                resourceName,
                "raw",
                context.getPackageName()
        );

        if (resourceId == 0) {
            Log.e(TAG, "Audio resource not found in res/raw: " + fileName);
            return;
        }

        SoundStop(resourceName);

        try {
            MediaPlayer player = MediaPlayer.create(context, resourceId);
            if (player == null) {
                Log.e(TAG, "MediaPlayer could not create: " + fileName);
                return;
            }

            SoundInstance instance = new SoundInstance(
                    resourceName,
                    player,
                    sharedSave.soundEffectsEnabled ? clamp01(volume) : 0.0f,
                    isLooping
            );

            player.setVolume(instance.currentVolume, instance.currentVolume);
            player.setLooping(isLooping);
            player.setOnCompletionListener(completedPlayer -> {
                boolean wasCurrentInstance = false;
                synchronized (Shared.class) {
                    if (instance.isLooping) {
                        // MediaPlayer normally does not dispatch completion
                        // while looping, but keep the contract safe if a
                        // device implementation does dispatch it.
                        if (!instance.released && !completedPlayer.isPlaying()) {
                            completedPlayer.start();
                        }
                        return;
                    }

                    if (SOUND_INSTANCES.get(instance.resourceName) == instance) {
                        SOUND_INSTANCES.remove(instance.resourceName);
                        wasCurrentInstance = true;
                    }
                    releaseSoundInstance(instance);
                }

                if (wasCurrentInstance && onCompletion != null) {
                    onCompletion.run();
                }
            });

            SOUND_INSTANCES.put(resourceName, instance);
            player.start();
        } catch (RuntimeException exception) {
            Log.e(TAG, "Could not play audio resource: " + fileName, exception);
        }
    }

    /**
     * Fades an already playing sound to targetVolume over durationSeconds.
     * Duration is expressed in seconds to match the original WPF helper.
     * A duration of zero changes the volume immediately.
     */
    public static synchronized void SoundFade(
            String fileName,
            float targetVolume,
            double durationSeconds
    ) {
        String resourceName = normalizeRawResourceName(fileName);
        SoundInstance instance = SOUND_INSTANCES.get(resourceName);

        if (instance == null || instance.released) {
            Log.w(TAG, "SoundFade ignored; sound is not playing: " + fileName);
            return;
        }

        if (instance.fadeAnimator != null) {
            instance.fadeAnimator.cancel();
            instance.fadeAnimator = null;
        }

        float safeTargetVolume = sharedSave.soundEffectsEnabled
                ? clamp01(targetVolume)
                : 0.0f;
        if (!sharedSave.soundEffectsEnabled || durationSeconds <= 0.0) {
            setSoundVolume(instance, safeTargetVolume);
            return;
        }

        ValueAnimator animator = ValueAnimator.ofFloat(
                instance.currentVolume,
                safeTargetVolume
        );
        animator.setDuration(Math.max(1L, Math.round(durationSeconds * 1000.0)));
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            synchronized (Shared.class) {
                if (!instance.released) {
                    setSoundVolume(instance, (float) animation.getAnimatedValue());
                }
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                synchronized (Shared.class) {
                    if (instance.fadeAnimator == animator) {
                        instance.fadeAnimator = null;
                    }
                }
            }
        });

        instance.fadeAnimator = animator;
        animator.start();
    }

    /**
     * Enables or disables every active sound immediately. Re-enabling uses
     * volume 1.0 for each active MediaPlayer, matching the settings toggle.
     */
    public static synchronized void SetSoundEffectsEnabled(boolean enabled) {
        sharedSave.soundEffectsEnabled = enabled;

        // Work from a snapshot so future changes to SOUND_INSTANCES cannot
        // affect this pass while each sound is updated through SoundFade().
        ArrayList<String> activeSounds = new ArrayList<>(SOUND_INSTANCES.keySet());
        float targetVolume = enabled ? 1.0f : 0.0f;
        for (String resourceName : activeSounds) {
            SoundFade(resourceName, targetVolume, 0.0);
        }
    }

    /**
     * Stops a sound and releases its native MediaPlayer resources.
     */
    public static synchronized void SoundStop(String fileName) {
        String resourceName = normalizeRawResourceName(fileName);
        SoundInstance instance = SOUND_INSTANCES.remove(resourceName);

        if (instance != null) {
            releaseSoundInstance(instance);
        }
    }

    /** Stops and releases every sound currently managed by Shared. */
    public static synchronized void SoundStopAll() {
        for (SoundInstance instance : SOUND_INSTANCES.values()) {
            releaseSoundInstance(instance);
        }
        SOUND_INSTANCES.clear();
    }

    private static void setSoundVolume(SoundInstance instance, float volume) {
        instance.currentVolume = clamp01(volume);
        if (!instance.released) {
            instance.player.setVolume(
                    instance.currentVolume,
                    instance.currentVolume
            );
        }
    }

    private static void releaseSoundInstance(SoundInstance instance) {
        if (instance.fadeAnimator != null) {
            instance.fadeAnimator.cancel();
            instance.fadeAnimator = null;
        }

        if (!instance.released) {
            instance.released = true;
            instance.player.stop();
            instance.player.release();
        }
    }

    private static Context requireApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException(
                    "Call Shared.Initialize(context) before using sound functions."
            );
        }
        return applicationContext;
    }

    private static String normalizeRawResourceName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Sound file name cannot be empty.");
        }

        String resourceName = fileName.trim();
        int extensionStart = resourceName.lastIndexOf('.');
        if (extensionStart > 0) {
            resourceName = resourceName.substring(0, extensionStart);
        }

        return resourceName.toLowerCase(Locale.US);
    }

    private static final class SoundInstance {
        private final String resourceName;
        private final MediaPlayer player;
        private final boolean isLooping;
        private float currentVolume;
        private ValueAnimator fadeAnimator;
        private boolean released;

        private SoundInstance(
                String resourceName,
                MediaPlayer player,
                float currentVolume,
                boolean isLooping
        ) {
            this.resourceName = resourceName;
            this.player = player;
            this.isLooping = isLooping;
            this.currentVolume = currentVolume;
        }
    }

    /**
     * Replaces the application's HorizonSave.dat with the current SharedSave
     * object encoded as readable JSON.
     */
    public static synchronized void SaveSharedSave(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null.");
        }

        Context appContext = context.getApplicationContext();

        try (FileOutputStream output = appContext.openFileOutput(
                SAVE_FILE_NAME,
                Context.MODE_PRIVATE
        )) {
            // MODE_PRIVATE truncates the old file before writing the new save.
            String json = sharedSave.toJson().toString(2);
            output.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException | JSONException exception) {
            throw new IllegalStateException(
                    "Could not save " + SAVE_FILE_NAME,
                    exception
            );
        }
    }

    /**
     * Loads SharedSave from internal storage. If the file does not exist or
     * contains invalid data, hardcoded defaults are saved and loaded again.
     */
    public static synchronized void LoadSharedSave(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null.");
        }

        Context appContext = context.getApplicationContext();
        File saveFile = appContext.getFileStreamPath(SAVE_FILE_NAME);

        if (!saveFile.exists()) {
            sharedSave.resetToDefaults();
            SaveSharedSave(appContext);
        }

        try {
            JSONObject json = readSaveJson(appContext);
            sharedSave.fromJson(json);
        } catch (IOException | JSONException exception) {
            Log.e(TAG, "Save was missing or invalid. Restoring defaults.", exception);
            sharedSave.resetToDefaults();
            SaveSharedSave(appContext);

            try {
                JSONObject freshJson = readSaveJson(appContext);
                sharedSave.fromJson(freshJson);
            } catch (IOException | JSONException secondException) {
                throw new IllegalStateException(
                        "Could not reload the fresh " + SAVE_FILE_NAME,
                        secondException
                );
            }
        }
    }

    private static JSONObject readSaveJson(Context context)
            throws IOException, JSONException {
        try (FileInputStream input = context.openFileInput(SAVE_FILE_NAME);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            byte[] data = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(data)) != -1) {
                buffer.write(data, 0, bytesRead);
            }

            String json = buffer.toString(StandardCharsets.UTF_8.name());
            return new JSONObject(json);
        }
    }

    public static final class SharedSave {
        /** Hard-coded developer switch; intentionally excluded from save JSON. */
        public boolean devMode;
        public int saveVersion;
        public int points;
        public int gameFinishedOnce;
        public int playerLevel;
        public float masterVolume;
        public int startGameSeconds;
        public int inGameSeconds;
        /** Points earned in the current run; this is intentionally transient. */
        public int inGamePoints;
        public int inGamePointsEarned;
        public int lowRange;
        public int inGameLowRange;
        public int highRange;
        public int inGameHighRange;
        public int numberToGuess;
        public int attemptsCounter;
        public boolean musicEnabled;
        public boolean soundEffectsEnabled;
        public boolean performanceModeEnabled;
        public String playerName;
        public String playerTable;
        /** "main_profile" or PROFILE_PHOTO_FILE_NAME in internal storage. */
        public String playerPhoto;
        public String selectedLanguage;
        public int carSelected;
        public String car1Name;
        public String car2Name;
        public String car3Name;
        public final ArrayList<String> unlockedItems = new ArrayList<>();
        public final ArrayList<Integer> highScores = new ArrayList<>();

        public SharedSave() {
            resetToDefaults();
        }

        public void resetToDefaults() {
            devMode = false;
            saveVersion = 1;
            points = 0;
            gameFinishedOnce = 0;
            playerLevel = 1;
            masterVolume = 1.0f;
            musicEnabled = true;
            startGameSeconds = 60;
            inGameSeconds = 0;
            inGamePoints = 0;
            inGamePointsEarned = 0;
            lowRange = 0;
            inGameLowRange = 0;
            highRange = 5;
            inGameHighRange = 10;
            attemptsCounter = 0;
            soundEffectsEnabled = true;
            performanceModeEnabled = false;
            playerName = "null";
            playerTable = "FORZA";
            playerPhoto = DEFAULT_PROFILE_PHOTO;
            selectedLanguage = "pl";
            carSelected = 1;
            car1Name = "Nissan 350Z RJN Sport";
            numberToGuess = 0;
            car2Name = "Abflug Supra S900 JZA80";
            car3Name = "Toyota BMW Z4 \"Supra A90\"";

            unlockedItems.clear();
            unlockedItems.add("default");

            highScores.clear();
            highScores.add(0);
        }

        private JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("saveVersion", saveVersion);
            json.put("points", points);
            json.put("gameFinishedOnce", gameFinishedOnce);
            json.put("playerLevel", playerLevel);
            json.put("masterVolume", masterVolume);
            json.put("musicEnabled", musicEnabled);
            json.put("soundEffectsEnabled", soundEffectsEnabled);
            json.put("performanceModeEnabled", performanceModeEnabled);
            json.put("playerName", playerName);
            json.put("playerTable", getPlayerTableCode());
            json.put("playerPhoto", playerPhoto);
            json.put("selectedLanguage", selectedLanguage);
            json.put("carSelected", carSelected);
            json.put("car1Name", car1Name);
            json.put("car2Name", car2Name);
            json.put("car3Name", car3Name);

            JSONArray items = new JSONArray();
            for (String item : unlockedItems) {
                items.put(item);
            }
            json.put("unlockedItems", items);

            JSONArray scores = new JSONArray();
            for (Integer score : highScores) {
                scores.put(score);
            }
            json.put("highScores", scores);

            return json;
        }

        private void fromJson(JSONObject json) throws JSONException {
            // Defaults also provide fallback values for fields added later.
            resetToDefaults();

            saveVersion = json.optInt("saveVersion", saveVersion);
            points = json.optInt("points", points);
            gameFinishedOnce = json.optInt("gameFinishedOnce", gameFinishedOnce);
            playerLevel = json.optInt("playerLevel", playerLevel);
            masterVolume = (float) json.optDouble("masterVolume", masterVolume);
            musicEnabled = json.optBoolean("musicEnabled", musicEnabled);
            soundEffectsEnabled = json.optBoolean(
                    "soundEffectsEnabled",
                    soundEffectsEnabled
            );
            performanceModeEnabled = json.optBoolean(
                    "performanceModeEnabled",
                    performanceModeEnabled
            );
            playerName = json.optString("playerName", playerName);
            playerTable = normalizePlayerTable(
                    json.optString("playerTable", playerTable)
            );
            playerPhoto = json.optString("playerPhoto", playerPhoto);
            selectedLanguage = json.optString(
                    "selectedLanguage",
                    selectedLanguage
            );
            carSelected = Math.max(1, Math.min(3, json.optInt("carSelected", carSelected)));
            car1Name = json.optString("car1Name", car1Name);
            car2Name = json.optString("car2Name", car2Name);
            car3Name = json.optString("car3Name", car3Name);

            JSONArray items = json.optJSONArray("unlockedItems");
            if (items != null) {
                unlockedItems.clear();
                for (int i = 0; i < items.length(); i++) {
                    unlockedItems.add(items.optString(i));
                }
            }

            JSONArray scores = json.optJSONArray("highScores");
            if (scores != null) {
                highScores.clear();
                for (int i = 0; i < scores.length(); i++) {
                    highScores.add(scores.optInt(i));
                }
            }
        }

        /** Returns the player tag as uppercase text limited to five characters. */
        public String getPlayerTableCode() {
            return normalizePlayerTable(playerTable);
        }

        private static String normalizePlayerTable(String value) {
            String normalized = value == null ? "" : value.trim().toUpperCase(Locale.US);
            return normalized.length() > 5 ? normalized.substring(0, 5) : normalized;
        }
    }

    /**
     * Moves the element center in the logical 1920 x 1080 SceneHost.
     * A duration of zero applies the target position immediately. Positive
     * durations use a constant linear speed from the current position.
     */
    public static void PositionTo(
            View element,
            float x,
            float y,
            long durationMilliseconds
    ) {
        SceneHost host = directSceneHostOf(element);
        if (host == null) {
            throw new IllegalArgumentException(
                    "PositionTo requires the element to be a direct child of SceneHost."
            );
        }

        SceneHost.SceneLayoutParams params =
                (SceneHost.SceneLayoutParams) element.getLayoutParams();

        cancelAnimator(POSITION_ANIMATORS, element);

        if (durationMilliseconds <= 0) {
            params.centerX = x;
            params.centerY = y;
            host.requestLayout();
            return;
        }

        float startX = params.centerX;
        float startY = params.centerY;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(durationMilliseconds);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            params.centerX = startX + (x - startX) * progress;
            params.centerY = startY + (y - startY) * progress;
            host.requestLayout();
        });
        removeAnimatorWhenFinished(animator, POSITION_ANIMATORS, element);

        POSITION_ANIMATORS.put(element, animator);
        animator.start();
    }

    /**
     * Scales an element from fromScale to toScale. A duration of zero applies
     * the final scale immediately. The element scales around its center.
     */
    public static void ScaleTo(
            View element,
            float fromScale,
            float toScale,
            long durationMilliseconds
    ) {
        cancelAnimator(SCALE_ANIMATORS, element);

        float safeFromScale = Math.max(0f, fromScale);
        float safeToScale = Math.max(0f, toScale);
        setElementScale(element, safeFromScale);

        if (durationMilliseconds <= 0) {
            setElementScale(element, safeToScale);
            return;
        }

        ValueAnimator animator = ValueAnimator.ofFloat(safeFromScale, safeToScale);
        animator.setDuration(durationMilliseconds);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            setElementScale(element, scale);
        });
        removeAnimatorWhenFinished(animator, SCALE_ANIMATORS, element);

        SCALE_ANIMATORS.put(element, animator);
        animator.start();
    }

    /**
     * Changes opacity from fromOpacity to toOpacity. Values are normally in
     * the range 0..1. A duration of zero applies the final opacity immediately.
     */
    public static void OpacityTo(
            View element,
            float fromOpacity,
            float toOpacity,
            long durationMilliseconds
    ) {
        cancelAnimator(OPACITY_ANIMATORS, element);

        float safeFromOpacity = clamp01(fromOpacity);
        float safeToOpacity = clamp01(toOpacity);
        element.setAlpha(safeFromOpacity);

        if (durationMilliseconds <= 0) {
            element.setAlpha(safeToOpacity);
            return;
        }

        ValueAnimator animator = ValueAnimator.ofFloat(
                safeFromOpacity,
                safeToOpacity
        );
        animator.setDuration(durationMilliseconds);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation ->
                element.setAlpha((float) animation.getAnimatedValue())
        );
        removeAnimatorWhenFinished(animator, OPACITY_ANIMATORS, element);

        OPACITY_ANIMATORS.put(element, animator);
        animator.start();
    }

    /**
     * Fades a full-screen black overlay and manages its touch blocking.
     *
     * While the overlay is visible, including during the animation, it is
     * clickable and blocks all interaction with views below it. When the
     * final opacity is zero, the overlay becomes GONE and no longer has a
     * hitbox. Opacity values are in the range 0..1.
     */
    public static void BlackFade(
            View overlay,
            float fromOpacity,
            float toOpacity,
            long durationMilliseconds
    ) {
        if (overlay == null) {
            throw new IllegalArgumentException("Black fade overlay cannot be null.");
        }

        cancelAnimator(BLACK_FADE_ANIMATORS, overlay);
        cancelAnimator(OPACITY_ANIMATORS, overlay);

        float safeFromOpacity = clamp01(fromOpacity);
        float safeToOpacity = clamp01(toOpacity);

        // A fully transparent overlay must stay absent from hit testing.
        if (safeFromOpacity <= 0f && safeToOpacity <= 0f) {
            finishBlackFade(overlay, 0f);
            return;
        }

        overlay.setVisibility(View.VISIBLE);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        overlay.setAlpha(safeFromOpacity);

        if (durationMilliseconds <= 0) {
            finishBlackFade(overlay, safeToOpacity);
            return;
        }

        ValueAnimator animator = ValueAnimator.ofFloat(
                safeFromOpacity,
                safeToOpacity
        );
        animator.setDuration(durationMilliseconds);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation ->
                overlay.setAlpha((float) animation.getAnimatedValue())
        );
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (BLACK_FADE_ANIMATORS.get(overlay) == animator) {
                    BLACK_FADE_ANIMATORS.remove(overlay);
                    finishBlackFade(overlay, safeToOpacity);
                }
            }
        });

        BLACK_FADE_ANIMATORS.put(overlay, animator);
        animator.start();
    }

    private static void finishBlackFade(View overlay, float finalOpacity) {
        overlay.setAlpha(finalOpacity);

        if (finalOpacity <= 0f) {
            overlay.setClickable(false);
            overlay.setFocusable(false);
            overlay.setVisibility(View.GONE);
        } else {
            overlay.setVisibility(View.VISIBLE);
            overlay.setClickable(true);
            overlay.setFocusable(true);
        }
    }

    private static void setElementScale(View element, float scale) {
        SceneHost host = directSceneHostOf(element);
        if (host != null) {
            host.setElementScale(element, scale);
        } else {
            element.setPivotX(element.getWidth() / 2f);
            element.setPivotY(element.getHeight() / 2f);
            element.setScaleX(scale);
            element.setScaleY(scale);
        }
    }

    private static SceneHost directSceneHostOf(View element) {
        ViewParent parent = element.getParent();
        if (parent instanceof SceneHost
                && element.getLayoutParams() instanceof SceneHost.SceneLayoutParams) {
            return (SceneHost) parent;
        }
        return null;
    }

    private static void cancelAnimator(
            Map<View, ValueAnimator> animators,
            View element
    ) {
        ValueAnimator oldAnimator = animators.remove(element);
        if (oldAnimator != null) {
            oldAnimator.cancel();
        }
    }

    private static void removeAnimatorWhenFinished(
            ValueAnimator animator,
            Map<View, ValueAnimator> animators,
            View element
    ) {
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (animators.get(element) == animator) {
                    animators.remove(element);
                }
            }
        });
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    /** Chainable wrapper which provides element-like method calls in Java. */
    public static final class Element {
        private final View view;

        private Element(View view) {
            this.view = view;
        }

        public Element PositionTo(
                float x,
                float y,
                long durationMilliseconds
        ) {
            Shared.PositionTo(view, x, y, durationMilliseconds);
            return this;
        }

        public Element ScaleTo(
                float fromScale,
                float toScale,
                long durationMilliseconds
        ) {
            Shared.ScaleTo(view, fromScale, toScale, durationMilliseconds);
            return this;
        }

        public Element OpacityTo(
                float fromOpacity,
                float toOpacity,
                long durationMilliseconds
        ) {
            Shared.OpacityTo(view, fromOpacity, toOpacity, durationMilliseconds);
            return this;
        }

        public Element BlackFade(
                float fromOpacity,
                float toOpacity,
                long durationMilliseconds
        ) {
            Shared.BlackFade(view, fromOpacity, toOpacity, durationMilliseconds);
            return this;
        }
    }
}
