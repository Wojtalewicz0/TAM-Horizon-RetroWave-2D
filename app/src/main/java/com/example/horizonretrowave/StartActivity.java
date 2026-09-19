package com.example.horizonretrowave;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * First screen shown when the application is launched.
 *
 * This is a separate Activity from MainActivity. Starting MainActivity does
 * not restart the application process; Android changes to the next Activity.
 */
public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the global save before either screen starts using shared data.
        Shared.LoadSharedSave(getApplicationContext());

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_start);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.start_main),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );
                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );
                    return insets;
                }
        );

        View startButton = findViewById(R.id.start_button);
        Shared.element(startButton).PositionTo(960f, 540f, 0L);

        startButton.setOnClickListener(view -> {
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);

            // The start screen is finished, so pressing Back from MainActivity
            // will exit this flow instead of returning to the start screen.
            finish();
        });
    }
}
