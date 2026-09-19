package com.example.horizonretrowave;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
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

    private static final Map<View, ValueAnimator> SCALE_ANIMATORS = new WeakHashMap<>();
    private static final Map<View, ValueAnimator> OPACITY_ANIMATORS = new WeakHashMap<>();
    private static final Map<View, ValueAnimator> POSITION_ANIMATORS = new WeakHashMap<>();

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

    /**
     * Example shared save data. Add future global values here, then add their
     * JSON read/write code in toJson() and fromJson().
     */
    public static final class SharedCar {
        public int id;
        public String model;

        public SharedCar(int id, String model) {
            this.id = id;
            this.model = model;
        }
    }

    public static final class SharedSave {
        public int saveVersion;
        public int points;
        public int gameFinishedOnce;
        public int playerLevel;
        public float masterVolume;
        public boolean musicEnabled;
        public boolean soundEffectsEnabled;
        public String playerName;
        public String selectedLanguage;
        public final ArrayList<String> unlockedItems = new ArrayList<>();
        public final ArrayList<Integer> highScores = new ArrayList<>();
        public final ArrayList<SharedCar> sharedCars = new ArrayList<>();

        public SharedSave() {
            resetToDefaults();
        }

        public void resetToDefaults() {
            saveVersion = 1;
            points = 0;
            gameFinishedOnce = 0;
            playerLevel = 1;
            masterVolume = 1.0f;
            musicEnabled = true;
            soundEffectsEnabled = true;
            playerName = "Player";
            selectedLanguage = "pl";

            unlockedItems.clear();
            unlockedItems.add("default");

            highScores.clear();
            highScores.add(0);

            sharedCars.clear();
            sharedCars.add(new SharedCar(1, "Nissan Silvia S15"));
            sharedCars.add(new SharedCar(2, "Toyota Supra A80"));
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
            json.put("playerName", playerName);
            json.put("selectedLanguage", selectedLanguage);

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

            JSONArray cars = new JSONArray();
            for (SharedCar car : sharedCars) {
                JSONObject carJson = new JSONObject();
                carJson.put("id", car.id);
                carJson.put("model", car.model);
                cars.put(carJson);
            }
            json.put("sharedCars", cars);

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
            playerName = json.optString("playerName", playerName);
            selectedLanguage = json.optString(
                    "selectedLanguage",
                    selectedLanguage
            );

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

            JSONArray cars = json.optJSONArray("sharedCars");
            if (cars != null) {
                sharedCars.clear();
                for (int i = 0; i < cars.length(); i++) {
                    JSONObject carJson = cars.optJSONObject(i);
                    if (carJson != null) {
                        sharedCars.add(new SharedCar(
                                carJson.optInt("id", 0),
                                carJson.optString("model", "")
                        ));
                    }
                }
            }
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
    }
}
