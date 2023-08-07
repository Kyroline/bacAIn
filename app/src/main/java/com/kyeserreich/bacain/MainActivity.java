package com.kyeserreich.bacain;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.http.SslError;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Locale;
import java.util.UUID;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private SensorManager mSensorManager;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;
    public Vibrator v = null;
    public boolean visibleState = false;
    private boolean isIntentFinished = false;
    protected static  final int RESULT_SPEECH =1;
    private String[] loginUrl = new String[] {
            "https://chat.openai.com/auth/login",
            "https://auth0.openai.com/u/login/",
            "https://accounts.google.com/",
            "https://login.live.com/",
            "https://appleid.apple.com/"
    };
    WebView webView;
    String text = "";
    ProgressBar progressBar;
    ProgressDialog progressDialog;
    private ActivityResultLauncher<Intent> someActivityResultLauncher;
    Intent data;
    TextToSpeech textToSpeech;
    long startTime;
    Handler handler = new Handler();
    Handler closeHandler = new Handler();
    final int delay = 3000;
    Runnable runnable;
    int paragraphCount;
    public String USER_AGENT = "(Android " + Build.VERSION.RELEASE + ") Chrome/110.0.5481.63 Mobile";
    private SwitchCompat toggleSwitch;
    private boolean switchState = false; // Untuk menyimpan status on/off

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Objects.requireNonNull(mSensorManager).registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 10f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progressBar);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading Please Wait...");
        paragraphCount = 0;

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUserAgentString(USER_AGENT);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);
        webView.setWebViewClient(new MyWebViewClient());
        webView.loadUrl("https://chat.openai.com");

        /*Alertnate code for  startActivityForResult(intent,REQUEST_CODE_SPEECH_INPUT);
          This code replaces deprecated startActivityForResult() method*/
        someActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // There are no request codes
                    data = result.getData();
                }
                isIntentFinished = true;
            }
        });
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(new Locale("id", "ID"));
                }
            }
        });
        // Create an UtteranceProgressListener object to handle callbacks
        UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                // Called when TTS starts speaking
            }

            @Override
            public void onDone(String utteranceId) {
                // Called when TTS finishes speaking
            }

            @Override
            public void onError(String utteranceId) {
                // Called when TTS encounters an error
            }
        };
        // Add the UtteranceProgressListener to the TextToSpeech object
        textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {

                progressBar.setProgress(newProgress);
                progressDialog.show();
                if (newProgress == 100) {
                    progressDialog.dismiss();
                }

                super.onProgressChanged(view, newProgress);
            }
        });
        webView.setVisibility(visibleState ? View.VISIBLE : View.GONE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hentikan Handler jika aktivitas dihancurkan sebelum 10 detik
        closeHandler.removeCallbacksAndMessages(null);
    }

    private void vibrate(int ms) {
        v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private final SensorEventListener mSensorListener = new SensorEventListener() {
        // Mendeteksi getaran
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;
            if (mAccel < -8) {
                onMicPressed();
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    protected void onResume() {
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }
    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mSensorListener);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();

        } else {
            closeApp();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.exit) {
            if (visibleState == true) {
                item.setIcon(R.drawable.baseline_visibility_off_24);
                visibleState = false;
                webView.setVisibility(visibleState ? View.VISIBLE : View.GONE);
                webView.setVisibility(View.GONE);
            } else {
                item.setIcon(R.drawable.baseline_visibility_24);
                visibleState = true;
                webView.setVisibility(visibleState ? View.VISIBLE : View.GONE);
                webView.setVisibility(View.VISIBLE);
            }
            webView.setVisibility(visibleState ? View.VISIBLE : View.GONE);
        }
//        else if (item.getItemId() == R.id.mic) {
//            onMicPressed();
//        } else if (item.getItemId() == R.id.speak) {
//            onSpeakerPressed();
//        }
        return super.onOptionsItemSelected(item);
    }



    private void onSpeakerPressed() {
        String allParagraph, paragraphLength;

        try {
//            allParagraph = "var paragraphs = document.getElementsByTagName('p');"
//                    + "var combinedText='';"
//                    + "for (var i =" + paragraphCount + "-1; i < paragraphs.length; i++) {"
//                    + "combinedText+= paragraphs[i].textContent;"
//                    + " }combinedText;";
            /*  PERUBAHAN  */
            /*  Update sebelumnya kode diatas hanya mendeteksi jawaban berdasarkan tag p nya saja,
            * kadang ada beberapa situasi dimana GPT memberikan list (ol) dengan dengan tag (li) tanpa
            * diapit tag p, sehingga list ini tidak terbaca aplikasi. Solusi dari masalah ini ada bawah  */
            allParagraph =
                    "var x = document.getElementsByClassName('prose');"
                            + "x[" + paragraphCount + "].textContent;";

            webView.evaluateJavascript(allParagraph, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    // Store the paragraph content in a string object
                    Toast.makeText(MainActivity.this, "Membacakan : " + value, Toast.LENGTH_SHORT);
                    String paragraphString = value;
                    String utteranceId = UUID.randomUUID().toString();
                    textToSpeech.speak(paragraphString, TextToSpeech.QUEUE_FLUSH, null, utteranceId);

                }
            });
//            paragraphLength = "(function() { return document.getElementsByTagName('p').length; })();";
            paragraphLength = "(function() { return document.getElementsByClassName('prose').length; })();";
            webView.evaluateJavascript(paragraphLength, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    paragraphCount = Integer.parseInt(value);

                }
            });


        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void onMicPressed() {
        vibrate(100);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "id");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "id");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "");
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000);

        try {
//            someActivityResultLauncher.launch(intent);
                startActivityForResult(intent, RESULT_SPEECH);
                isIntentFinished = false;
                webView.requestFocus();
                new CountDownTimer(10000, 10000) {
                    @Override
                    public void onTick(long millisUntilFinished) {}
                    @Override
                    public void onFinish() {
                        if (!isIntentFinished) {
                            finishActivity(RESULT_SPEECH);
                        }
                    }
                }.start();
            } catch (Exception e) {
            Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void closeApp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to Exit ?").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            webView.setVisibility(visibleState ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d("URL", view.getUrl());
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            boolean isFound = false;
            if (view.getUrl().indexOf("https://chat.openai.com/") !=-1 && (view.getUrl().indexOf("login" +
                    "") == -1)) {
                new CountDownTimer(1000, 1000) {
                    public void onFinish() {
                        webView.setVisibility(visibleState ? View.VISIBLE : View.GONE);
                    }

                    public void onTick(long millisUntilFinished) {}
                }.start();
            } else {
                webView.setVisibility(visibleState ? View.VISIBLE : View.GONE);
            }
            for (String uri : loginUrl) {
                if (view.getUrl().indexOf(uri) !=-1) {
                    if(uri == loginUrl[0]) {
                        speak("Silakan login terlebih dahulu");
                        Log.d("URI", uri);
                    }
                    Log.d("URI", "URI Pada : " + uri);
                    isFound = true;
                    break;
                } //true
            }
            if (isFound) {
                webView.setVisibility(View.VISIBLE);
            }
            webView.requestFocus();

        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            speak("Web gagal dimuat");
            webView.reload();
        }

        @Override
        public void onReceivedHttpError(
                WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler,
                                       SslError error) {
        }
    }

    public void checkEnd() {
        handler.postDelayed(runnable = new Runnable() {
            public void run() {
//                webView.post(() -> webView.evaluateJavascript(
//                        "var paragraphs = document.getElementsByTagName('p');"
//                                + "var combinedText='';"
//                                + "for (var i =" + paragraphCount + "-1; i < paragraphs.length; i++) {"
//                                + "combinedText+= paragraphs[i].textContent;"
//                                + " }combinedText;", new ValueCallback<String>() {
                /*  PERUBAHAN  */
                /*  Update sebelumnya kode diatas hanya mengambil jawaban berdasarkan tag p saja, kadang
                 * ada beberapa situasi dimana GPT memberikan list (ol) dengan dengan tag (li) tanpa diapit
                 * tag p, sehingga list ini tidak terbaca aplikasi. Solusi dari masalah ini ada bawah  */
//                webView.post(() -> webView.evaluateJavascript(
//                        "var x = document.getElementsByClassName('prose');"
//                                + "var y = x[" + paragraphCount + "].childNodes;"
//                                + "combinedText = '';"
//                                + "for (var i = 0; i < y.length; i++) {"
//                                + "if (y[i].tagName == 'P') {"
//                                + "combinedText += y[i].textContent;"
//                                + "} else if (y[i].tagName == 'OL') {"
//                                + "var z = y[i].childNodes;"
//                                + "for (var t = 0; t < z.length; t++) {"
//                                + "combinedText += z[t].textContent;"
//                                + "}"
//                                + "}"
//                                + "}combinedText;", new ValueCallback<String>() {
                webView.post(() -> webView.evaluateJavascript(
                        "var x = document.getElementsByClassName('prose');"
                                + "x[" + paragraphCount + "].textContent;", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                String paragraphString = value;
                                if (paragraphString.length() != text.length()) {
                                    Log.d("Current", paragraphString);
                                    Log.d("Previous", text);
                                    text = paragraphString;
                                } else {
                                    long timeElapsed = System.currentTimeMillis() - startTime;
                                    String[] splited = text.split("\\s+");
                                    speak(paragraphString);
                                    handler.removeCallbacks(runnable);
                                    MainActivity.this.runOnUiThread(new Runnable() {
                                        public void run() {
                                            Toast.makeText(MainActivity.this, "Time Elapsed: " + timeElapsed / 1000 + " s\nJumlah kata: " + splited.length + "\nJumlah karakter: " + text.length(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }
                        }));
                handler.postDelayed(runnable, delay);
            }
        }, delay);
    }

    private void speak(String paragraph) {
        String paragraphLength;
        Toast.makeText(MainActivity.this, "Membacakan", Toast.LENGTH_SHORT);
        if (paragraph == "null") {
            try {
                String utteranceId = UUID.randomUUID().toString();
                textToSpeech.speak("Gagal menerima masukan, tolong ulangi pertanyaan anda.", TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            try {
                String utteranceId = UUID.randomUUID().toString();
                textToSpeech.speak(paragraph, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                paragraphLength = "(function() { return document.getElementsByClassName('prose').length; })();";
//            paragraphLength = "(function() { return document.getElementsByTagName('p').length; })();";
                webView.evaluateJavascript(paragraphLength, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        paragraphCount = Integer.parseInt(value);

                    }
                });


            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        isIntentFinished = true;
        try {
            startTime = System.currentTimeMillis();
            String str = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            if (str.toLowerCase() == "manual aplikasi") {
                String utteranceId = UUID.randomUUID().toString();
                textToSpeech.speak("Untuk memulai goyangkan ponsel dan tanyakan pertanyaan. ChatGPT akan " +
                        "menjawab pertanyaan secara otomatis.", TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            } else {
                //melakukan manipulasi perubahan pada textarea menggunakan keyboard agar button bisa terupdate
                pressPrompt(str);
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void pressPrompt(String str) {
        new CountDownTimer(1000, 1000) {
            public void onFinish() {
                webView.setVisibility(View.VISIBLE);
                long downTime = 900;
                long eventTime = 1000;
                int metaState = 0;
                MotionEvent me = MotionEvent.obtain(
                        downTime,
                        eventTime,
                        MotionEvent.ACTION_DOWN,
                        (float) (webView.getWidth() / 2),  //100 atau 10.24
                        (float) (webView.getHeight() / 1.15),  //1700
                        metaState
                );
                webView.dispatchTouchEvent(me);
                me = MotionEvent.obtain(
                        downTime,
                        eventTime,
                        MotionEvent.ACTION_UP,
                        (float) (webView.getWidth() / 2),  //50 atau 20.48
                        (float) (webView.getHeight() / 1.15),  //1700
                        metaState
                );
                webView.dispatchTouchEvent(me);
                refreshButton(str);
            }

            public void onTick(long millisUntilFinished) {
            }
        }.start();
    }
    private void refreshButton(String str) {
        new CountDownTimer(1000, 1000) {
            public void onFinish() {
                webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE));
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(webView.getWindowToken(), 0);
                webView.setVisibility(visibleState ? View.VISIBLE : View.GONE);
                //memasukkan hasil speech recognition ke textarea
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("(function() { var d = document.getElementsByTagName('textarea').length;");
                stringBuilder.append("document.getElementsByTagName('textarea')[d-1].value='");
                stringBuilder.append(str);
                stringBuilder.append("';document.querySelector('button.absolute').disabled = false;");
                stringBuilder.append("})();");
                webView.evaluateJavascript(stringBuilder.toString(), null);
                pressButton();
            }

            public void onTick(long millisUntilFinished) {
            }
        }.start();
    }
    private void pressButton() {
        new CountDownTimer(1000, 1000) {
            public void onFinish() {
                webView.evaluateJavascript("(function() { document.querySelector('button.absolute').click();})();",
                        null);
                checkEnd();
            }

            public void onTick(long millisUntilFinished) {
            }
        }.start();
    }
}