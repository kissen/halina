package me.schaertl.halina;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

public class TextViewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_view);

        // Do display the back button.

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Get objects and arguments.

        final TextView mainText = findViewById(R.id.main_text);

        if (mainText == null) {
            return;
        }

        final Bundle arguments = getIntent().getExtras();

        if (arguments == null) {
            return;
        }

        // Set text.

        final String title = arguments.getString("title");
        setTitle(title);

        final String text = arguments.getString("text");
        mainText.setText(text);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}