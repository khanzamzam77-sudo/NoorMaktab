package com.noormaktab.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Noor Maktab: shows the dua page (assets/index.html) full screen and gives it the
 * phone's own text-to-speech engine, so Arabic is spoken by Google/Samsung TTS.
 */
public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    private static final String HOST = "appassets.androidplatform.net";
    private static final int PICK_FILES = 7;

    private WebView web;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;
    private final List<Voice> voiceList = new ArrayList<>();
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (HOST.equals(uri.getHost())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {
                }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = params.createIntent();
                if (params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                try {
                    startActivityForResult(intent, PICK_FILES);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new TtsBridge(), "AndroidTTS");
        tts = new TextToSpeech(this, this);
        web.loadUrl("https://" + HOST + "/assets/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILES || fileCallback == null) return;
        Uri[] result = null;
        if (resultCode == RESULT_OK && data != null) {
            ClipData clip = data.getClipData();
            if (clip != null && clip.getItemCount() > 0) {
                result = new Uri[clip.getItemCount()];
                for (int i = 0; i < clip.getItemCount(); i++) result[i] = clip.getItemAt(i).getUri();
            } else if (data.getData() != null) {
                result = new Uri[]{data.getData()};
            }
        }
        fileCallback.onReceiveValue(result);
        fileCallback = null;
    }

    // ---------- text to speech ----------

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) return;
        tts.setLanguage(new Locale("ar"));
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { sendToPage(id, "start", 0); }
            @Override public void onDone(String id) { sendToPage(id, "end", 0); }
            @Override public void onError(String id) { sendToPage(id, "error", 0); }
            @Override public void onError(String id, int code) { sendToPage(id, "error", 0); }
            @Override public void onStop(String id, boolean interrupted) { sendToPage(id, "end", 0); }
            @Override public void onRangeStart(String id, int start, int end, int frame) { sendToPage(id, "range", start); }
        });
        collectVoices();
        ttsReady = true;
        runOnUiThread(() -> web.evaluateJavascript("window.__ttsReady&&window.__ttsReady()", null));
    }

    /** Arabic, Hindi, Urdu and English voices that are installed, best quality first. */
    private void collectVoices() {
        voiceList.clear();
        try {
            Set<Voice> all = tts.getVoices();
            if (all != null) {
                for (Voice v : all) {
                    String lang = v.getLocale().getLanguage();
                    if (!(lang.equals("ar") || lang.equals("hi") || lang.equals("ur") || lang.equals("en"))) continue;
                    Set<String> f = v.getFeatures();
                    if (f != null && f.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) continue;
                    voiceList.add(v);
                }
            }
        } catch (Exception ignored) {
        }
        Collections.sort(voiceList, (a, b) -> {
            if (a.getQuality() != b.getQuality()) return b.getQuality() - a.getQuality();
            return Boolean.compare(a.isNetworkConnectionRequired(), b.isNetworkConnectionRequired());
        });
    }

    private void sendToPage(String id, String event, int value) {
        final String js = "window.__tts&&window.__tts(" + JSONObject.quote(id) + ",'" + event + "'," + value + ")";
        runOnUiThread(() -> web.evaluateJavascript(js, null));
    }

    private class TtsBridge {
        @JavascriptInterface
        public String voices() {
            JSONArray out = new JSONArray();
            if (!ttsReady) return out.toString();
            try {
                int n = 0;
                for (Voice v : voiceList) {
                    n++;
                    Locale loc = v.getLocale();
                    String langName = loc.getLanguage().equals("ar") ? "Arabic" : loc.getDisplayLanguage(Locale.ENGLISH);
                    String label = langName + " " + n
                            + (v.getQuality() >= Voice.QUALITY_HIGH ? " · Natural" : "")
                            + (v.isNetworkConnectionRequired() ? " · Online" : " · Offline");
                    JSONObject o = new JSONObject();
                    o.put("name", label);
                    o.put("lang", loc.toLanguageTag());
                    o.put("voiceURI", v.getName());
                    out.put(o);
                }
                if (voiceList.isEmpty() && tts.isLanguageAvailable(new Locale("ar")) >= TextToSpeech.LANG_AVAILABLE) {
                    JSONObject o = new JSONObject();
                    o.put("name", "Arabic (phone)");
                    o.put("lang", "ar");
                    o.put("voiceURI", "");
                    out.put(o);
                }
            } catch (Exception ignored) {
            }
            return out.toString();
        }

        @JavascriptInterface
        public void speak(String text, String lang, float rate, String id, String voiceName) {
            if (!ttsReady) { sendToPage(id, "error", 0); return; }
            boolean voiceSet = false;
            if (voiceName != null && !voiceName.isEmpty()) {
                for (Voice v : voiceList) {
                    if (v.getName().equals(voiceName)) { tts.setVoice(v); voiceSet = true; break; }
                }
            }
            if (!voiceSet) tts.setLanguage(Locale.forLanguageTag(lang == null || lang.isEmpty() ? "ar" : lang));
            tts.setSpeechRate(rate > 0 ? rate : 1f);
            int r = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id);
            if (r == TextToSpeech.ERROR) sendToPage(id, "error", 0);
        }

        @JavascriptInterface
        public void stop() {
            if (tts != null) tts.stop();
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        web.destroy();
        super.onDestroy();
    }
}
