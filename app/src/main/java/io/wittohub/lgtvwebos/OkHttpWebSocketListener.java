package io.wittohub.lgtvwebos;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;

public class OkHttpWebSocketListener extends okhttp3.WebSocketListener {
    private static final int NORMAL_CLOSURE_STATUS = 1000;
    private static WebSocket mWebSocket;
    private static WebSocket mSpecialWebSocket;
    private static OkHttpClient mSpecialWebSocketClient = new OkHttpClient();
    private Context mCallingContext;
    private Activity mCallingActivity;
    private TextView mStatusText;
    private String mCK;
    private SharedPreferences mSharedPref;
    private int cidCount = 0;
    private String cidPrefix = Long.toHexString(Double.doubleToLongBits(Math.random())).substring(0, 8);
    private boolean flagSwitchingProtocolResp = true;
    private int ctrSwitchingProtocolResp = 0;

    public OkHttpWebSocketListener(Context context, Activity activity) {
        this.mCallingContext = context;
        this.mCallingActivity = activity;
        mSharedPref = mCallingActivity.getSharedPreferences("MY_SHARED_PREF", Context.MODE_PRIVATE);

        mStatusText = mCallingActivity.findViewById(R.id.statusText);
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
       if (response.toString().contains("netinput.pointer.sock")) {
            // note: these if/ else blocks are executed in different instances and don't share same member variable states/ data. Therefore shared preference is recommended
            String TV_CMD = mSharedPref.getString("TV_CMD", null);
            if (mSpecialWebSocket != null && TV_CMD != null) {
                mSpecialWebSocket.send("type:button\nname:" + TV_CMD + "\n\n");
            }
        }
        else {
            mWebSocket = webSocket;
            setTextOnUIThread(mStatusText, "Connecting..", Color.GRAY);
            mStatusText.setVisibility(View.VISIBLE);
            mCK = mSharedPref.getString("TV_CK_CODE", null);
            String handshake_payload = null;
            // if no CK exists then make fresh handshake
            if (mCK == null) {
                handshake_payload = "{\"type\":\"register\",\"id\":\"register_0\",\"payload\":{\"forcePairing\":false,\"pairingType\":\"PROMPT\",\"manifest\":{\"manifestVersion\":1,\"appVersion\":\"1.1\",\"signed\":{\"created\":\"20140509\",\"appId\":\"com.lge.test\",\"vendorId\":\"com.lge\",\"localizedAppNames\":{\"\":\"LG Remote App\",\"ko-KR\":\"리모컨 앱\",\"zxx-XX\":\"ЛГ Rэмotэ AПП\"},\"localizedVendorNames\":{\"\":\"LG Electronics\"},\"permissions\":[\"TEST_SECURE\",\"CONTROL_INPUT_TEXT\",\"CONTROL_MOUSE_AND_KEYBOARD\",\"READ_INSTALLED_APPS\",\"READ_LGE_SDX\",\"READ_NOTIFICATIONS\",\"SEARCH\",\"WRITE_SETTINGS\",\"WRITE_NOTIFICATION_ALERT\",\"CONTROL_POWER\",\"READ_CURRENT_CHANNEL\",\"READ_RUNNING_APPS\",\"READ_UPDATE_INFO\",\"UPDATE_FROM_REMOTE_APP\",\"READ_LGE_TV_INPUT_EVENTS\",\"READ_TV_CURRENT_TIME\"],\"serial\":\"2f930e2d2cfe083771f68e4fe7bb07\"},\"permissions\":[\"LAUNCH\",\"LAUNCH_WEBAPP\",\"APP_TO_APP\",\"CLOSE\",\"TEST_OPEN\",\"TEST_PROTECTED\",\"CONTROL_AUDIO\",\"CONTROL_DISPLAY\",\"CONTROL_INPUT_JOYSTICK\",\"CONTROL_INPUT_MEDIA_RECORDING\",\"CONTROL_INPUT_MEDIA_PLAYBACK\",\"CONTROL_INPUT_TV\",\"CONTROL_POWER\",\"READ_APP_STATUS\",\"READ_CURRENT_CHANNEL\",\"READ_INPUT_DEVICE_LIST\",\"READ_NETWORK_STATE\",\"READ_RUNNING_APPS\",\"READ_TV_CHANNEL_LIST\",\"WRITE_NOTIFICATION_TOAST\",\"READ_POWER_STATE\",\"READ_COUNTRY_INFO\"],\"signatures\":[{\"signatureVersion\":1,\"signature\":\"eyJhbGdvcml0aG0iOiJSU0EtU0hBMjU2Iiwia2V5SWQiOiJ0ZXN0LXNpZ25pbmctY2VydCIsInNpZ25hdHVyZVZlcnNpb24iOjF9.hrVRgjCwXVvE2OOSpDZ58hR+59aFNwYDyjQgKk3auukd7pcegmE2CzPCa0bJ0ZsRAcKkCTJrWo5iDzNhMBWRyaMOv5zWSrthlf7G128qvIlpMT0YNY+n/FaOHE73uLrS/g7swl3/qH/BGFG2Hu4RlL48eb3lLKqTt2xKHdCs6Cd4RMfJPYnzgvI4BNrFUKsjkcu+WD4OO2A27Pq1n50cMchmcaXadJhGrOqH5YmHdOCj5NSHzJYrsW0HPlpuAx/ECMeIZYDh6RMqaFM2DXzdKX9NmmyqzJ3o/0lkk/N97gfVRLW5hA29yeAwaCViZNCP8iC9aO0q9fQojoa7NQnAtw==\"}]}}}";
            }
            // if CK exists then send handshake with CK
            else {
                handshake_payload = "{\"type\":\"register\",\"id\":\"register_0\",\"payload\":{\"forcePairing\":false,\"pairingType\":\"PROMPT\",\"client-key\":\"" + mCK + "\",\"manifest\":{\"manifestVersion\":1,\"appVersion\":\"1.1\",\"signed\":{\"created\":\"20140509\",\"appId\":\"com.lge.test\",\"vendorId\":\"com.lge\",\"localizedAppNames\":{\"\":\"LG Remote App\",\"ko-KR\":\"리모컨 앱\",\"zxx-XX\":\"ЛГ Rэмotэ AПП\"},\"localizedVendorNames\":{\"\":\"LG Electronics\"},\"permissions\":[\"TEST_SECURE\",\"CONTROL_INPUT_TEXT\",\"CONTROL_MOUSE_AND_KEYBOARD\",\"READ_INSTALLED_APPS\",\"READ_LGE_SDX\",\"READ_NOTIFICATIONS\",\"SEARCH\",\"WRITE_SETTINGS\",\"WRITE_NOTIFICATION_ALERT\",\"CONTROL_POWER\",\"READ_CURRENT_CHANNEL\",\"READ_RUNNING_APPS\",\"READ_UPDATE_INFO\",\"UPDATE_FROM_REMOTE_APP\",\"READ_LGE_TV_INPUT_EVENTS\",\"READ_TV_CURRENT_TIME\"],\"serial\":\"2f930e2d2cfe083771f68e4fe7bb07\"},\"permissions\":[\"LAUNCH\",\"LAUNCH_WEBAPP\",\"APP_TO_APP\",\"CLOSE\",\"TEST_OPEN\",\"TEST_PROTECTED\",\"CONTROL_AUDIO\",\"CONTROL_DISPLAY\",\"CONTROL_INPUT_JOYSTICK\",\"CONTROL_INPUT_MEDIA_RECORDING\",\"CONTROL_INPUT_MEDIA_PLAYBACK\",\"CONTROL_INPUT_TV\",\"CONTROL_POWER\",\"READ_APP_STATUS\",\"READ_CURRENT_CHANNEL\",\"READ_INPUT_DEVICE_LIST\",\"READ_NETWORK_STATE\",\"READ_RUNNING_APPS\",\"READ_TV_CHANNEL_LIST\",\"WRITE_NOTIFICATION_TOAST\",\"READ_POWER_STATE\",\"READ_COUNTRY_INFO\"],\"signatures\":[{\"signatureVersion\":1,\"signature\":\"eyJhbGdvcml0aG0iOiJSU0EtU0hBMjU2Iiwia2V5SWQiOiJ0ZXN0LXNpZ25pbmctY2VydCIsInNpZ25hdHVyZVZlcnNpb24iOjF9.hrVRgjCwXVvE2OOSpDZ58hR+59aFNwYDyjQgKk3auukd7pcegmE2CzPCa0bJ0ZsRAcKkCTJrWo5iDzNhMBWRyaMOv5zWSrthlf7G128qvIlpMT0YNY+n/FaOHE73uLrS/g7swl3/qH/BGFG2Hu4RlL48eb3lLKqTt2xKHdCs6Cd4RMfJPYnzgvI4BNrFUKsjkcu+WD4OO2A27Pq1n50cMchmcaXadJhGrOqH5YmHdOCj5NSHzJYrsW0HPlpuAx/ECMeIZYDh6RMqaFM2DXzdKX9NmmyqzJ3o/0lkk/N97gfVRLW5hA29yeAwaCViZNCP8iC9aO0q9fQojoa7NQnAtw==\"}]}}}";
            }
            webSocket.send(handshake_payload);
            //SystemClock.sleep(10000);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        Log.i("INFO: ", "Received from TV : " + text);
        Gson gson = new Gson();
        JsonElement element = gson.fromJson(text, JsonElement.class);
        JsonObject jsonObj = element.getAsJsonObject();

        // case: after handshake TV returns a special socket on which we must send data to trigger CMD. This is actually called only when TV returns special socket for calling of certain cmds
        if (jsonObj.has("type") && jsonObj.get("payload").getAsJsonObject().has("socketPath")) {
            if (jsonObj.get("type").getAsString().equals("response")) {
                String specialSocketURL = jsonObj.get("payload").getAsJsonObject().get("socketPath").getAsString();
                connectToSpecialSocket(specialSocketURL);
            }
        }
        // this is called only once. Once it is set, the above if-block takes over
        if (mSpecialWebSocket == null) {
            getSpecialSocket();
        }
        if (mCK == null && jsonObj.get("type").getAsString().equals("registered") && jsonObj.get("payload").getAsJsonObject().has("client-key")) {
            // new CK is sent by TV --> save it on shared pref
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putString("TV_CK_CODE", jsonObj.get("payload").getAsJsonObject().get("client-key").getAsString());
            editor.apply();
            setTVConnectionStatus(true);
            sendCmdToTV("BTN", "GET_APPS");        // fetches app list and sets in a sharedPref to be returned to watch on its load
            sendCmdToTV("BTN", "GET_SOURCES");        // fetches sources and sets in a sharedPref to be returned to watch on its load
        }
        if (jsonObj.has("type")) {
            if (jsonObj.get("type").getAsString().equals("registered")) {
                sendCmdToTV("BTN", "GET_APPS");        // fetches app list and sets in a sharedPref to be returned to watch on its load
                sendCmdToTV("BTN", "GET_SOURCES");        // fetches sources and sets in a sharedPref to be returned to watch on its load
            } else if (jsonObj.get("type").getAsString().equals("error")) {
                setTextOnUIThread(mStatusText, "Please accept as YES on TV to continue", Color.YELLOW);
                mStatusText.setVisibility(View.VISIBLE);
            }
        }
        // receiving all apps and sending it to watch. This is called and set in advance so that when watch requests it..its ready to be returned
        if (jsonObj.get("payload").getAsJsonObject().has("launchPoints")) {
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putString("APP_LIST", jsonObj.get("payload").getAsJsonObject().getAsJsonArray("launchPoints").toString());
            editor.apply();

            Gson gson2 = new Gson();
            JsonElement element2 = gson2.fromJson(jsonObj.get("payload").getAsJsonObject().getAsJsonArray("launchPoints").toString(), JsonElement.class);
            JsonArray appList = element2.getAsJsonArray();
            JsonArray appListFiltered = new JsonArray();
            try {
                // filter the ones without any icon
                for (int i = 0; i < appList.size(); i++) {
                    String iconURL = appList.get(i).getAsJsonObject().get("largeIcon").getAsString();
                    if (iconURL.length() > 1) {
                        appListFiltered.add(appList.get(i).getAsJsonObject());
                    }
                }
                MainActivity.getInstance().setAppList(appListFiltered);

            } catch (IOException e) {
                Log.e("EXCEPTION: ", e.getMessage());
            }
        }
        // receiving all sources and sending it to watch. This is called and set in advance so that when watch requests it..its ready to be returned
        if (jsonObj.get("payload").getAsJsonObject().has("devices")) {
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putString("SRC_LIST", jsonObj.get("payload").getAsJsonObject().getAsJsonArray("devices").toString());
            editor.apply();
        } else if (jsonObj.get("payload").getAsJsonObject().has("client-key")) {
            setTVConnectionStatus(true);
        }
    }

    void setTVConnectionStatus(boolean status) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean("IS_TV_CONNECTED", status);
        editor.apply();
        if (status) {
            setTextOnUIThread(mStatusText, "Connected", Color.GREEN);
            mStatusText.setVisibility(View.GONE);
        } else {
            setTextOnUIThread(mStatusText, "Unable To Connect\nor\nTV is OFF", Color.RED);
            mStatusText.setVisibility(View.VISIBLE);
            MainActivity.getInstance().displayErrorOnWatch("Unable To Connect\nor\nTV is OFF");
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        Log.i("INFO: ", "Receiving bytes : " + bytes.hex());
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
        Log.i("INFO: ", "Closing : " + code + " / " + reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        Log.e("ERROR: ", t.getMessage());
        // storing flag to check in loop in MainActivity for TV connection. This will keep phone app to always be connected to TV.
        setTVConnectionStatus(false);
    }

    private void setTextOnUIThread(TextView view, String message, int bgColor) {
        new Handler(Looper.getMainLooper()).post(
                () -> {
                    view.setText(message);
                    view.setTextColor(bgColor);
                });
    }

    private String getCid() {
        String cid = cidPrefix + "000" + cidCount++;
        return cid;
    }

    public void sendCmdToTV(String cmdType, String value) {
        try {
            // Note: works also without cid. But kept is as safety maybe in the scenarios when same TV is used by two watches
            String payload = "";
            String specialPayload = "{\"id\":\"" + getCid() + "\",\"type\":\"request\",\"uri\":\"ssap://com.webos.service.networkinput/getPointerInputSocket\",\"payload\":{}}";
            if (cmdType.equals("BTN")) {
                // Saving cmd in shared pref to be used in 2nd step to pass in special web socket
                SharedPreferences sharedPref = mCallingActivity.getSharedPreferences("MY_SHARED_PREF", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("TV_CMD", value);
                editor.apply();

                switch (value) {
                    case "HOME":
                    case "BACK":
                    case "MENU":
                    case "UP":
                    case "DOWN":
                    case "LEFT":
                    case "RIGHT":
                    case "ENTER":
                    case "0":
                    case "1":
                    case "2":
                    case "3":
                    case "4":
                    case "5":
                    case "6":
                    case "7":
                    case "8":
                    case "9":

                        payload = specialPayload;
                        if (mSpecialWebSocket != null) {
                            String TV_CMD = mSharedPref.getString("TV_CMD", null);
                            mSpecialWebSocket.send("type:button\nname:" + TV_CMD + "\n\n");
                        }
                        break;
                    case "VOL_UP":
                        payload = "{\"id\":\"" + getCid() + "\",\"type\":\"request\",\"uri\":\"ssap://audio/volumeUp\",\"payload\":{}}";
                        break;
                    case "VOL_DOWN":
                        payload = "{\"id\":\"" + getCid() + "\",\"type\":\"request\",\"uri\":\"ssap://audio/volumeDown\",\"payload\":{}}";
                        break;
                    case "MUTE":
                        payload = "{\"id\":\"" + getCid() + "\",\"type\":\"request\",\"uri\":\"ssap://audio/setMute\",\"payload\":{\"mute\": true}}";
                        break;
                    case "TURN_OFF":
                        payload = "{\"id\":\"" + getCid() + "\",\"type\":\"request\",\"uri\":\"ssap://system/turnOff\",\"payload\":{}}";
                        setTVConnectionStatus(false);
                        break;
                    case "CH_UP":
                        payload = "{\"id\":\"" + getCid() + "\",\"type\":\"request\",\"uri\":\"ssap://tv/channelUp\",\"payload\":{}}";
                        break;
                    case "CH_DOWN":
                        payload = "{\"id\":\"" + getCid() + "\",\"type\":\"request\",\"uri\":\"ssap://tv/channelDown\",\"payload\":{}}";
                        break;
                    case "GET_SOURCES":
                        payload = "{\"id\":\"" + getCid() + "\",\"type\":\"request\",\"uri\":\"ssap://tv/getExternalInputList\",\"payload\":{}}";
                        break;
                    case "GET_APPS":
                        payload = "{\"id\":\"" + getCid() + "\",\"type\":\"request\",\"uri\":\"ssap://com.webos.applicationManager/listLaunchPoints\",\"payload\":{}}";
                        break;
                }
            } else if (cmdType.equals("APP")) {
                payload = "{\"id\":\"" + getCid() + "\",\"type\":\"request\",\"uri\":\"ssap://system.launcher/launch\",\"payload\":{\"id\": \"" + value + "\"}}";
            } else if (cmdType.equals("SRC")) {
                payload = "{\"id\":\"" + getCid() + "\",\"type\":\"request\",\"uri\":\"ssap://tv/switchInput\",\"payload\":{\"inputId\": \"" + value + "\"}}";
            }
            mWebSocket.send(payload);
        } catch (Exception e) {
            Log.e("EXCEPTION: ", e.getMessage());
        }
    }

    void connectToSpecialSocket(String socketURL) {
        Request request = new Request.Builder().url(socketURL).build();
        OkHttpWebSocketListener okHttpWebSocketListener = new OkHttpWebSocketListener(mCallingContext, mCallingActivity);

        if (mSpecialWebSocket == null) {
            mSpecialWebSocket = mSpecialWebSocketClient.newWebSocket(request, okHttpWebSocketListener);
        }
    }

    void getSpecialSocket() {
        String specialPayload = "{\"id\":\"" + getCid() + "\",\"type\":\"request\",\"uri\":\"ssap://com.webos.service.networkinput/getPointerInputSocket\",\"payload\":{}}";
        mWebSocket.send(specialPayload);
    }
}

