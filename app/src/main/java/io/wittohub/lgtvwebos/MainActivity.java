package io.wittohub.lgtvwebos;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.wittohub.lgtvwebos.databinding.ActivityMainBinding;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class MainActivity extends Activity {

    private static MainActivity instance;
    private ActivityMainBinding binding;
    SharedPreferences.OnSharedPreferenceChangeListener prefListener;        // imp to be class member or else goes to gc and doesn't work
    private static OkHttpWebSocketListener mOkHttpWebSocketListener;
    private OkHttpClient mWebSocketClient;
    private String mTV_IP;
    private ScrollView mViewLoadingScreen;
    private ScrollView mViewMainScreen;
    private ScrollView mViewStatusScreen;
    private TextView mTextViewErrorMsg;
    private ImageButton mBtn_refresh;
    private JsonArray mAppList;
    private final View.OnClickListener ClickListener = v -> {
        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE));
        int index = (Integer) v.getTag();
        String appID = mAppList.get(index).getAsJsonObject().get("id").getAsString();
        JsonObject data = new JsonObject();
        data.addProperty("cmdType", "APP");
        data.addProperty("value", appID);
        sendDataToTV(data);
    };

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        instance = this;

        mViewLoadingScreen = findViewById(R.id.viewLoadingScreen);
        mViewMainScreen = findViewById(R.id.viewMainScreen);
        mViewStatusScreen = findViewById(R.id.viewStatusScreen);
        mTextViewErrorMsg = findViewById(R.id.textViewErrorMsg);
        mBtn_refresh = findViewById(R.id.btn_refresh);

        // Note: do not use sharedPref as class member variable since it won't be available to all instances of MainActivity --> for example in receiveData
        SharedPreferences sharedPref = getSharedPreferences("MY_SHARED_PREF", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("TV_CMD", null);       // Note: always init to null or else last btn is executed on first special socket connection
        editor.apply();

        mWebSocketClient = new OkHttpClient();
        mOkHttpWebSocketListener = new OkHttpWebSocketListener(getApplicationContext(), this);

        mTV_IP = sharedPref.getString("TV_IP", null);

        /*
            This listener will be called once TV IP is set after auto discovery
         */
        prefListener =
                (prefs, key) -> {
                    if (key.equals("TV_IP")) {
                        mTV_IP = sharedPref.getString("TV_IP", null);
                        connectToTV();
                    }
                };

        sharedPref.registerOnSharedPreferenceChangeListener(prefListener);

        /*
            If TV IP is not available then start auto discovery
         */
        if (mTV_IP == null) {
            displayStatus("Searching for TV..", Color.YELLOW);
            ScanLocalNetworkDevices scanLocalDevices = new ScanLocalNetworkDevices(getApplicationContext(), this);
            scanLocalDevices.startPingService(getApplicationContext());
        }
        /*
            If TV IP is available then directly connect and check for handshake in onOpen listener in OkHttpWebSocketListener
         */
        else {
            connectToTV();

//            new Timer().scheduleAtFixedRate(new TimerTask() {
//                @Override
//                public void run() {
//                    // auto reconnect to TV every 5s if NOT yet due to OFF or lost connection
//                    if (!sharedPref.getBoolean("IS_TV_CONNECTED", false)) {
//                        connectToTV();
//                    }
//                }
//            }, 0, 5000);


        }
    }

    void connectToTV() {
        showLoadingIndicator();
        Request request = new Request.Builder().url("ws://" + mTV_IP + ":3000").build();
        mWebSocketClient.newWebSocket(request, mOkHttpWebSocketListener);
    }

    public void btnPressed(View v) {
        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE));

        JsonObject data = new JsonObject();
        data.addProperty("cmdType", "BTN");
        switch (v.getId()) {
            case R.id.btn_home:
                data.addProperty("value", "HOME");
                break;
            case R.id.btn_mute:
                data.addProperty("value", "MUTE");
                break;
            case R.id.btn_vol_plus:
                data.addProperty("value", "VOL_UP");
                break;
            case R.id.btn_vol_minus:
                data.addProperty("value", "VOL_DOWN");
                break;
            case R.id.btn_back:
                data.addProperty("value", "BACK");
                break;
            case R.id.btn_power_off:
                data.addProperty("value", "TURN_OFF");
                displayStatus("TV is OFF", Color.RED);
                mBtn_refresh.setVisibility(View.GONE);
                break;
            case R.id.btn_ch_up:
                data.addProperty("value", "CH_UP");
                break;
            case R.id.btn_ch_down:
                data.addProperty("value", "CH_DOWN");
                break;
            case R.id.btn_settings:
                data.addProperty("value", "MENU");
                break;
            case R.id.btn_source:
                data.addProperty("value", "GET_SOURCES");
                break;
            case R.id.btn_nav_up:
                data.addProperty("value", "UP");
                break;
            case R.id.btn_nav_down:
                data.addProperty("value", "DOWN");
                break;
            case R.id.btn_nav_left:
                data.addProperty("value", "LEFT");
                break;
            case R.id.btn_nav_right:
                data.addProperty("value", "RIGHT");
                break;
            case R.id.btn_enter:
                data.addProperty("value", "ENTER");
                break;
            case R.id.btn_key0:
                data.addProperty("value", "0");
                break;
            case R.id.btn_key1:
                data.addProperty("value", "1");
                break;
            case R.id.btn_key2:
                data.addProperty("value", "2");
                break;
            case R.id.btn_key3:
                data.addProperty("value", "3");
                break;
            case R.id.btn_key4:
                data.addProperty("value", "4");
                break;
            case R.id.btn_key5:
                data.addProperty("value", "5");
                break;
            case R.id.btn_key6:
                data.addProperty("value", "6");
                break;
            case R.id.btn_key7:
                data.addProperty("value", "7");
                break;
            case R.id.btn_key8:
                data.addProperty("value", "8");
                break;
            case R.id.btn_key9:
                data.addProperty("value", "9");
                break;
        }
        sendDataToTV(data);
    }

    public void sendDataToTV(JsonObject data) {
        try {
            String cmdType = data.get("cmdType").getAsString();
            String value = data.get("value").getAsString();

            if (value.equals("GET_SOURCES")) {
                SharedPreferences sharedPref = getSharedPreferences("MY_SHARED_PREF", Context.MODE_PRIVATE);
                //sendDataToWatch(sharedPref.getString("SRC_LIST", null).getBytes());

                Gson gson = new Gson();
                JsonElement element = gson.fromJson(sharedPref.getString("SRC_LIST", null), JsonElement.class);
                JsonArray srcList = element.getAsJsonArray();
                JsonArray srcListFiltered = new JsonArray();
                try {
                    // filter the ones without any icon
                    for (int i = 0; i < srcList.size(); i++) {
                        String iconURL = srcList.get(i).getAsJsonObject().get("icon").getAsString();
                        if (iconURL.length() > 1) {
                            srcListFiltered.add(srcList.get(i).getAsJsonObject());
                        }
                    }
                    showDialogWithSources(srcListFiltered);

                } catch (Exception e) {
                    Log.e("EXCEPTION: ", e.getMessage());
                }
            }
            else if (value.equals("TURN_OFF")) {
                displayStatus("TV is OFF", Color.RED);
            }
            mOkHttpWebSocketListener = new OkHttpWebSocketListener(getApplicationContext(), this);
            mOkHttpWebSocketListener.sendCmdToTV(cmdType, value);


        } catch (Exception e) {
            Log.e("EXCEPTION: ", e.getMessage());
        }

    }

    void showDialogWithSources(JsonArray sources) {
        Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.custom_dialog);
        ListView lv = dialog.findViewById(R.id.lv);
        dialog.setCancelable(true);
        dialog.show();

        List<String> list = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) {
            list.add(sources.get(i).getAsJsonObject().get("label").getAsString());

        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.textcenter, list);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener((adapter1, v, index, arg3) -> {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE));
            JsonObject data = new JsonObject();
            data.addProperty("cmdType", "SRC");
            data.addProperty("value", sources.get(index).getAsJsonObject().get("id").getAsString());
            sendDataToTV(data);
        });
    }

    public void checkStatus(View view) {
        showLoadingIndicator();
        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE));
        if (Build.VERSION.SDK_INT >= 11) {
            recreate();
        } else {
            Intent intent = getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            overridePendingTransition(0, 0);

            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    void showLoadingIndicator(){
        runOnUiThread(() -> {
            mViewMainScreen.setVisibility(View.GONE);
            mViewStatusScreen.setVisibility(View.GONE);
            mViewLoadingScreen.setVisibility(View.VISIBLE);
        });
    }
    void displayStatus(String message, int color) {
        try{
            runOnUiThread(() -> {
                mViewLoadingScreen.setVisibility(View.GONE);
                mTextViewErrorMsg.setText(message);
                mTextViewErrorMsg.setTextColor(color);
                mViewStatusScreen.setBackgroundColor(Color.BLACK);
                mViewStatusScreen.setVisibility(View.VISIBLE);
                if(message.equals("Connected")){
                    mViewStatusScreen.setVisibility(View.GONE);
                    mBtn_refresh.setVisibility(View.GONE);
                    mViewMainScreen.setVisibility(View.VISIBLE);
                }
                else{
                    mViewMainScreen.setVisibility(View.GONE);
                    mBtn_refresh.setVisibility(View.VISIBLE);
                }
            });
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    void setAppList(JsonArray appList) throws IOException {
        Context parentThis = this;
        mAppList = appList;       // used inside onClick listener of app icon to resolve appID based on index
        new Thread(() -> {
            // Do network action in this function
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT, 1);

            LinearLayout parent = findViewById(R.id.layoutApps);
            final LinearLayout[] layout2 = new LinearLayout[1];

            ImageButton[] appBtn = new ImageButton[appList.size()];
            for (int i = 0; i < appList.size(); i++) {
                String iconURL = appList.get(i).getAsJsonObject().get("largeIcon").getAsString();
                URL appImgURL;
                try {
                    if (iconURL.length() > 1) {
                        appImgURL = new URL(iconURL);
                        InputStream inputStream = (InputStream) appImgURL.getContent();
                        Drawable drawable = Drawable.createFromStream(inputStream, null);

                        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                        Drawable d = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 90, 90, true));

                        appBtn[i] = new ImageButton(parentThis);
                        appBtn[i].setImageDrawable(d);
                        appBtn[i].setLayoutParams(lp);
                        ((ViewGroup.MarginLayoutParams) appBtn[i].getLayoutParams()).setMargins(15, 15, 15, 15);
                        appBtn[i].setOnClickListener(ClickListener);
                        appBtn[i].setBackgroundColor(Color.TRANSPARENT);
                        appBtn[i].setTag(i);
                        appBtn[i].setId(i);

                        int finalI = i;
                        runOnUiThread(() -> {
                            if (finalI % 3 == 0) {
                                layout2[0] = new LinearLayout(getApplicationContext());
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                params.gravity = Gravity.CENTER;
                                layout2[0].setLayoutParams(params);
                                layout2[0].setOrientation(LinearLayout.HORIZONTAL);
                                parent.addView(layout2[0]);
                            }
                            layout2[0].addView(appBtn[finalI]);

                        });
                    }

                } catch (Exception e) {
                    Log.e("EXCEPTION: ", e.getMessage());
                }

            }
        }).start();
    }
}