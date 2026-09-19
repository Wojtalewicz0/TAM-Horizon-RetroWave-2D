package com.example.horizonretrowave;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor uiExecutor = command -> mainHandler.post(command);

    private TextView helloText;
    private CompletableFuture<Void> introSequence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Find the TextView declared in activity_main.xml.
        helloText = findViewById(R.id.hello_text);
        Shared.element(helloText).PositionTo(480f, 540f, 0L);

        /*
         * Example of using and changing data from the global SharedSave object:
         *
         * if (Shared.sharedSave.points > 10) {
         *     Shared.sharedSave.playerLevel = 2;
         *
         *     // Save the changed value permanently in HorizonSave.dat.
         *     Shared.SaveSharedSave(this);
         * }
         */

        /*
         * Examples of using the global list of SharedCar objects:
         *
         * if (!Shared.sharedSave.sharedCars.isEmpty()) {
         *     // Read values from the first car.
         *     Shared.SharedCar firstCar = Shared.sharedSave.sharedCars.get(0);
         *     String model = firstCar.model;
         *     int carId = firstCar.id;
         *
         *     // Modify an existing car.
         *     firstCar.model = "Nissan Silvia S15 Tuned";
         *     firstCar.id = 10;
         * }
         *
         * // Add a new car.
         * Shared.sharedSave.sharedCars.add(
         *         new Shared.SharedCar(3, "Mazda RX-7 FD")
         * );
         *
         * // Remove the first car.
         * if (!Shared.sharedSave.sharedCars.isEmpty()) {
         *     Shared.sharedSave.sharedCars.remove(0);
         * }
         *
         * // Persist all changes in HorizonSave.dat.
         * Shared.SaveSharedSave(this);
         */

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Start the non-blocking startup sequence immediately after the view
        // hierarchy has been created.
        startAppSequence();
    }

    /**
     * A Java/Android equivalent of a linear WPF async startup sequence.
     *
     * Each thenCompose() continues the sequence only after the previous
     * CompletableFuture has finished. No thread is blocked while waiting.
     */
    private void startAppSequence() {
        introSequence = delaySeconds(5.0)
                .thenCompose(ignore -> runOnUi(() ->
                        helloText.setText(R.string.intro_finished)
                ))
                .thenCompose(ignore -> delaySeconds(5.0))
                .thenCompose(ignore -> runOnUi(() ->
                        helloText.setText(R.string.starting_menu)
                ))
                .thenCompose(ignore -> delaySeconds(3.0))
                .thenCompose(ignore -> runOnUi(() -> {
                    // GONE removes the view from layout and hit testing.
                    // Alpha makes the visual intention explicit as well.
                    Shared.OpacityTo(helloText, 1.0f, 0.0f, 0L);
                    helloText.setVisibility(View.GONE);
                }));
    }

    /**
     * The Android equivalent of: await Task.Delay(seconds * 1000).
     */
    private CompletableFuture<Void> delaySeconds(double seconds) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        long milliseconds = Math.round(seconds * 1000.0);
        mainHandler.postDelayed(() -> future.complete(null), milliseconds);

        return future;
    }

    /**
     * Runs a UI action on Android's main thread and returns a future that is
     * completed when that action finishes.
     */
    private CompletableFuture<Void> runOnUi(Runnable action) {
        return CompletableFuture.runAsync(action, uiExecutor);
    }

    @Override
    protected void onDestroy() {
        // Stop the sequence and remove delayed callbacks when the Activity is
        // destroyed, for example after a configuration change.
        if (introSequence != null) {
            introSequence.cancel(true);
        }
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
