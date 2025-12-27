package com.chitui.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chitui.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val PREFS_NAME = "ChitUIPrefs"
        private const val SERVER_URL_KEY = "server_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if server address is already saved
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString(SERVER_URL_KEY, null)

        if (!savedUrl.isNullOrEmpty()) {
            // Server address exists, go directly to WebView
            openWebView(savedUrl)
            finish()
            return
        }

        // Show server address input screen
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.connectButton.setOnClickListener {
            val serverAddress = binding.serverAddressInput.editText?.text.toString().trim()

            if (serverAddress.isEmpty()) {
                Toast.makeText(this, "Please enter a server address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add http:// if no protocol specified
            val finalUrl = if (!serverAddress.startsWith("http://") &&
                             !serverAddress.startsWith("https://")) {
                "http://$serverAddress"
            } else {
                serverAddress
            }

            // Save server address
            prefs.edit().putString(SERVER_URL_KEY, finalUrl).apply()

            // Open WebView
            openWebView(finalUrl)
            finish()
        }
    }

    private fun openWebView(url: String) {
        val intent = Intent(this, WebViewActivity::class.java).apply {
            putExtra("url", url)
        }
        startActivity(intent)
    }
}
