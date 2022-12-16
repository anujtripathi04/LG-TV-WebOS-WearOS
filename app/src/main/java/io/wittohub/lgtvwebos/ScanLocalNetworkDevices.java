package io.wittohub.lgtvwebos;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.wittohub.lgtvwebos.model.LocalDeviceInfo;

public class ScanLocalNetworkDevices {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());
    LocalDeviceInfo mTVDetails = new LocalDeviceInfo(null, null);
    boolean mIsTVFound = false;

    private Context mCallingContext;
    private Activity mCallingActivity;
    SharedPreferences mSharedPref;

    public ScanLocalNetworkDevices(Context context, Activity activity){
        this.mCallingContext = context;
        this.mCallingActivity = activity;
        mSharedPref = mCallingActivity.getSharedPreferences("MY_SHARED_PREF", Context.MODE_PRIVATE);
    }

    public void startPingService(Context context)
    {
        executor.execute(() -> {
            List<LocalDeviceInfo> deviceInfoList  = new ArrayList<>();
            try {
                WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
                String subnet = getSubnetAddress(mWifiManager.getDhcpInfo().gateway);
                String ip2 = "192.168.0.219";
                String tmp = InetAddress.getByAddress(new byte[] {(byte) 192, (byte) 168, 0, (byte) 219}).getHostName();
                System.out.println(tmp);
                for (int i=1; i<255; i++){
                    String ip = subnet + "." + i;
                    String hostname = InetAddress.getByName(ip).getHostName();
                    //if (InetAddress.getByName(host).isReachable(1)){
                    if (true){
                        LocalDeviceInfo localDeviceInfo = new LocalDeviceInfo(ip, hostname);
                        deviceInfoList.add(localDeviceInfo);
                    }
                    else
                    {
                        Log.e("ERROR: ", "Not Reachable IP: " + String.valueOf(ip));
                    }
                }
                for (int i=0; i<deviceInfoList.size(); i++){
                    System.out.println("hostname="+deviceInfoList.get(i).getHostname().toLowerCase());
                    if(deviceInfoList.get(i).getHostname().toLowerCase().contains("lgwebostv")){
                        Log.i("INFO: ", "TV auto discovered");
                        mIsTVFound = true;
                        mTVDetails = deviceInfoList.get(i);
                    }
                }
                if(mIsTVFound){
                    MainActivity.getInstance().displayStatus("TV Found: " + mTVDetails.getHostname(), Color.GREEN);
                    SharedPreferences.Editor editor = mSharedPref.edit();
                    editor.putString("TV_IP", mTVDetails.getIp());
                    editor.apply();
                }
                else{
                    MainActivity.getInstance().displayStatus("No TV Found\n Please configure manually", Color.RED);
                }
            }
            catch(Exception e){
                Log.e("EXCEPTION: ", e.getMessage());
            }
            handler.post(() -> {
                //UI Thread work here
            });
        });
    }


    private String getSubnetAddress(int address)
    {
        String ipString = String.format(
                "%d.%d.%d",
                (address & 0xff),
                (address >> 8 & 0xff),
                (address >> 16 & 0xff));

        return ipString;
    }
}

