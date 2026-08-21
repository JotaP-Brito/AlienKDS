package com.restaurantpos.kds;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SharedPreferences prefs;
    private TextView connectionStatus;
    private ToneGenerator alertTone;
    private AudioManager audioManager;
    private long lastAlertAt;
    private boolean pageHadError;
    private static final String PREFS_NAME = "KDSPrefs";
    private static final String KEY_IP = "laptop_ip";
    private static final long ALERT_DEBOUNCE_MS = 800L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        webView = findViewById(R.id.webview);
        connectionStatus = findViewById(R.id.connection_status);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        alertTone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

        findViewById(R.id.reload_button).setOnClickListener(view -> webView.reload());
        findViewById(R.id.server_button).setOnClickListener(view -> showIpDialog());

        // WebView settings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.addJavascriptInterface(new OrderAlertBridge(), "AndroidKds");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    pageHadError = true;
                    showConnectionError();
                }
            }

            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                pageHadError = true;
                showConnectionError();
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                pageHadError = false;
                connectionStatus.setText("Conectando…");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!pageHadError) {
                    connectionStatus.setText("Conectado");
                    installOrderObserver();
                }
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
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("192.168.1.14 ou 192.168.1.14:3000");
        input.setText(prefs.getString(KEY_IP, ""));
        input.setSelectAllOnFocus(true);
        builder.setView(input);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String server = normalizeServer(input.getText().toString());
            if (server != null) {
                prefs.edit().putString(KEY_IP, server).apply();
                loadKitchen(server);
            } else {
                Toast.makeText(MainActivity.this, "Endereço inválido.", Toast.LENGTH_SHORT).show();
                showIpDialog();
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void loadKitchen(String ip) {
        String url = "http://" + ip + "/kitchen";
        webView.loadUrl(url);
    }

    /** Accepts an IP address/hostname with an optional port and always stores host:port. */
    private String normalizeServer(String value) {
        String candidate = value == null ? "" : value.trim();
        if (candidate.startsWith("http://")) {
            candidate = candidate.substring("http://".length());
        } else if (candidate.startsWith("https://")) {
            // The KDS server is reached over the local HTTP endpoint below.
            return null;
        }
        candidate = candidate.replaceAll("/+$", "");
        if (candidate.isEmpty() || candidate.contains("/") || candidate.contains(" ")) {
            return null;
        }

        Uri parsed = Uri.parse("http://" + candidate);
        if (parsed.getHost() == null || parsed.getHost().isEmpty() || parsed.getUserInfo() != null) {
            return null;
        }
        return parsed.getPort() == -1 ? candidate + ":3000" : candidate;
    }

    private void showConnectionError() {
        connectionStatus.setText("Sem conexão");
        Toast.makeText(this, "Não foi possível carregar. Verifique a rede e o servidor.", Toast.LENGTH_LONG).show();
    }

    /** Called only by the KDS page's injected observer when a new ticket is rendered. */
    private class OrderAlertBridge {
        @JavascriptInterface
        public void onNewOrder() {
            runOnUiThread(MainActivity.this::playNewOrderAlert);
        }
    }

    private void playNewOrderAlert() {
        long now = System.currentTimeMillis();
        if (now - lastAlertAt < ALERT_DEBOUNCE_MS) {
            return;
        }
        lastAlertAt = now;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest request = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(new android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .build();
            audioManager.requestAudioFocus(request);
        } else {
            //noinspection deprecation
            audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        }
        alertTone.startTone(ToneGenerator.TONE_PROP_BEEP2, 650);
    }

    private void installOrderObserver() {
        // The POS owns the page, so observe its rendered ticket list rather than polling an undocumented API.
        String script = "(function(){"
                + "if(window.__kdsOrderObserverInstalled)return;window.__kdsOrderObserverInstalled=true;"
                + "var selector='[data-order-id],[data-order],[data-ticket-id],.order-card,.kds-order,.order,.ticket';"
                + "var known=new Set(),ready=false;"
                + "function key(el){return el.getAttribute('data-order-id')||el.getAttribute('data-order')||el.getAttribute('data-ticket-id')||el.id||('text:'+((el.innerText||'').replace(/\\s+/g,' ').trim().slice(0,160)));}"
                + "function scan(root){var els=[];if(root.nodeType===1){if(root.matches&&root.matches(selector))els.push(root);if(root.querySelectorAll)els=els.concat([].slice.call(root.querySelectorAll(selector)));}els.forEach(function(el){var k=key(el);if(!k||known.has(k))return;known.add(k);if(ready&&window.AndroidKds)window.AndroidKds.onNewOrder();});}"
                + "scan(document.body);new MutationObserver(function(mutations){mutations.forEach(function(m){[].forEach.call(m.addedNodes,scan);});}).observe(document.body,{childList:true,subtree:true});"
                + "setTimeout(function(){ready=true;},2500);"
                + "window.addEventListener('kds:new-order',function(){if(ready&&window.AndroidKds)window.AndroidKds.onNewOrder();});"
                + "})();";
        webView.evaluateJavascript(script, null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (alertTone != null) {
            alertTone.release();
        }
        webView.removeJavascriptInterface("AndroidKds");
        super.onDestroy();
    }
}
