package com.kyeserreich.bacain;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Debug;
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
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
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

    public boolean visibleState = false;
    WebView webView;
    String text = "Custom wee woo wee woo";
    ProgressBar progressBar;
    ProgressDialog progressDialog;
    private ActivityResultLauncher<Intent> someActivityResultLauncher;
    Intent data;
    TextToSpeech textToSpeech;
    Handler handler = new Handler();
    final int delay = 5000; // 1000 milliseconds == 1 second
    Runnable runnable;

    int paragraphCount;
    public String USER_AGENT = "(Android " + Build.VERSION.RELEASE + ") Chrome/110.0.5481.63 Mobile";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Locale locale = new Locale("id", "ID");
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
        getBaseContext().getResources().getDisplayMetrics());

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Objects.requireNonNull(mSensorManager).registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 10f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progressBar);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading Please Wait...");
        paragraphCount = 1;

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
            if (mAccel > 12) {
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
            if (webView.getVisibility() == View.VISIBLE) {
                webView.setVisibility(View.GONE);
            } else if (webView.getVisibility() == View.GONE) {
                webView.setVisibility(View.VISIBLE);
            } else {
                //invisible
            }
        } else if (item.getItemId() == R.id.mic) {
            onMicPressed();
        } else if (item.getItemId() == R.id.speak) {
            onSpeakerPressed();
        } else if (item.getItemId() == R.id.app_bar_switch) {
            if (visibleState == true) {
                visibleState = false;
                Log.d("VISIBLE STATE", "False");
            } else {
                visibleState = true;
                Log.d("VISIBLE STATE", "True");
            }
        }
        return super.onOptionsItemSelected(item);
    }



    private void onSpeakerPressed() {
        String allParagraph, paragraphLength;

        try {

            allParagraph = "var paragraphs = document.getElementsByTagName('p');"
                    + "var combinedText='';"
                    + "for (var i =" + paragraphCount + "-1; i < paragraphs.length; i++) {"
                    + "combinedText+= paragraphs[i].textContent;"
                    + " }combinedText;";

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
            paragraphLength = "(function() { return document.getElementsByTagName('p').length; })();";
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
        // Menampilkan speech recognizer untuk mendapatkan pertanyaan
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, new Locale("id", "ID"));
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Something");
        try {
            someActivityResultLauncher.launch(intent);
            webView.requestFocus();
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
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(visibleState ? View.VISIBLE : View.GONE);
            webView.requestFocus();

        }
    }

    public void checkEnd() {
        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                Log.d("REPEATER", "START");
                webView.post(() -> webView.evaluateJavascript(
                        "var paragraphs = document.getElementsByTagName('p');"
                                + "var combinedText='';"
                                + "for (var i =" + paragraphCount + "-1; i < paragraphs.length; i++) {"
                                + "combinedText+= paragraphs[i].textContent;"
                                + " }combinedText;", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                String paragraphString = value;
                                text = value;
                                Log.d("REPEATER", value);
                                if (paragraphString == text) {
                                    speak(paragraphString);
                                    handler.removeCallbacks(runnable);
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
        if (paragraph == "\"\"") {
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

                paragraphLength = "(function() { return document.getElementsByTagName('p').length; })();";
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
        try {
            String str = data.getStringArrayListExtra("android.speech.extra.RESULTS").get(0);
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
        Toast.makeText(MainActivity.this, "Width: " + webView.getWidth() + " Height: " + webView.getHeight(), Toast.LENGTH_SHORT).show();
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
                webView.evaluateJavascript("(function() { document.querySelector('button.absolute').click();})();", null);
                checkEnd();
            }

            public void onTick(long millisUntilFinished) {
            }
        }.start();
    }
}