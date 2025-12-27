package com.chitui.webview;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "ChitUIPrefs";
    private static final String SERVER_URL_KEY = "server_url";

    private EditText serverAddressInput;
    private Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if server address is already saved
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUrl = prefs.getString(SERVER_URL_KEY, null);

        if (savedUrl != null && !savedUrl.isEmpty()) {
            // Server address exists, go directly to WebView
            openWebView(savedUrl);
            finish();
            return;
        }

        // Show server address input screen
        setContentView(R.layout.activity_main);

        serverAddressInput = findViewById(R.id.server_address_input);
        connectButton = findViewById(R.id.connect_button);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String serverAddress = serverAddressInput.getText().toString().trim();

                if (TextUtils.isEmpty(serverAddress)) {
                    Toast.makeText(MainActivity.this, "Please enter a server address", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Add http:// if no protocol specified
                if (!serverAddress.startsWith("http://") && !serverAddress.startsWith("https://")) {
                    serverAddress = "http://" + serverAddress;
                }

                // Save server address
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(SERVER_URL_KEY, serverAddress);
                editor.apply();

                // Open WebView
                openWebView(serverAddress);
                finish();
            }
        });
    }

    private void openWebView(String url) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("url", url);
        startActivity(intent);
    }
}
