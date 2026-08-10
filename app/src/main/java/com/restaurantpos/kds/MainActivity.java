package com.restaurantpos.kds;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "KDSPrefs";
    private static final String KEY_IP = "laptop_ip";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        webView = findViewById(R.id.webview);

        // WebView settings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(MainActivity.this, "Erro ao carregar. Verifique a rede.", Toast.LENGTH_LONG).show();
            }
        });

        // Load saved IP or ask for it
        String savedIp = prefs.getString(KEY_IP, "");
        if (!savedIp.isEmpty()) {
            loadKitchen(savedIp);
        } else {
            showIpDialog();
        }
    }

    private void showIpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Endereço do servidor");
        builder.setMessage("Digite o IP do computador da lanchonete (ex: 192.168.1.14)");

        final EditText input = new EditText(this);
        input.setHint("192.168.1.14");
        builder.setView(input);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String ip = input.getText().toString().trim();
            if (!ip.isEmpty()) {
                prefs.edit().putString(KEY_IP, ip).apply();
                loadKitchen(ip);
            } else {
                Toast.makeText(MainActivity.this, "IP inválido.", Toast.LENGTH_SHORT).show();
                showIpDialog();
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void loadKitchen(String ip) {
        String url = "http://" + ip + ":3000/kitchen";
        webView.loadUrl(url);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}