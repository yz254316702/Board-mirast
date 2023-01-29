package com.example.myapplication;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.text.method.ScrollingMovementMethod;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.net.wifi.WifiManager;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.security.auth.login.LoginException;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "Wi-Fi";
    private final String TAG1 = "Wifi-P2P";
    private WifiManager mWifi;
    private WifiP2pManager mWifiP2p;
    private WifiP2pManager.Channel mP2pChannel;
    private IntentFilter mIntent;
    private MyThread mThread;
    private MyBroadcastReceiver mReceiver;
    private WifiP2pManager.ActionListener mListner;
    private MyNetworkThread myNetworkThread;
    private TextView myText;
    private String mRemoteAddress = null;
    private int mControlPort = 7236;

    private class MyThread extends Thread {
        public void run() {
            Looper.prepare();
            Looper.loop();
        }

        public Looper getLooper() {
            return Looper.myLooper();
        }
    }
/*
    private class NetworkThread extends Thread {
        public void run() {
            Looper.prepare();
            Looper.loop();
        }

        public Looper getLooper() {
            return Looper.myLooper();
        }
    }*/

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.v(TAG1, "on Back Pressed");
        if (myNetworkThread != null) {
            myNetworkThread.closeRTSPClient();
        }
        mWifiP2p.stopPeerDiscovery(mP2pChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.v(TAG1, "Stopped peer discovery Successfully");
            }

            @Override
            public void onFailure(int reason) {
                Log.v(TAG1, "Failed to Stop peer discovery for reason: " + reason);
            }
        });

        mWifiP2p.removeGroup(mP2pChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.v(TAG1, "Removed P2P Group Successfully");
            }

            @Override
            public void onFailure(int reason) {
                Log.v(TAG1, "Failed to Remove P2P Group for reason: " + reason);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG1, "on Stop called");
        /*unregisterReceiver(mReceiver);
        if (myNetworkThread != null) {
            myNetworkThread.closeRTSPClient();
        }*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        mReceiver = new MyBroadcastReceiver();
        registerReceiver(mReceiver, mIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        myText = (TextView) findViewById(R.id.editText2);
        myText.setMovementMethod(new ScrollingMovementMethod());

        // my wife code start here anushakner
        //get wifi enabled if not already

        mWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (mWifi == null) {
            Log.e(TAG,"Error getting Wifi Service");
        } else {
            if (mWifi.isWifiEnabled()) {
                Log.i(TAG, "Wifi is enabled");
                showMessage("Wifi is enabled");

            } else {
                Log.e(TAG, "Wifi is disabled...enabling Wi-Fi");
                showMessage("Wifi is disabled...enabling Wi-Fi");

                boolean enabled = true;
                boolean err = mWifi.setWifiEnabled(enabled);
                if (!err) {
                   Log.e(TAG,"Wifi enable failed with err : " + err);
                    showMessage("Wifi enable failed with err : " + err);
                }
            }

            mIntent = new IntentFilter();
            mIntent.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            mIntent.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            mIntent.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
            mIntent.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mIntent.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
            // check for p2p device
         /*   int currentapiVersion = android.os.Build.VERSION.SDK_INT;
            if (currentapiVersion >= android.os.Build.VERSION_CODES.LOLLIPOP){
                if (!mWifi.isP2pSupported()) {
                    Log.e(TAG,"P2P is not supported on this device");
                }
            }*/

            mWifiP2p = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
            if (mWifiP2p == null) {
                Log.e(TAG1, "Error getting Wi-Fi P2P Service");
            } else {
                mThread = new MyThread();
                //mThread.start();
                mP2pChannel = mWifiP2p.initialize(this.getApplicationContext(), mThread.getLooper(), new WifiP2pManager.ChannelListener() {
                            @Override
                            public void onChannelDisconnected() {
                                Log.e(TAG1, "P2P Channel to frmaework is disconnetced");
                            }
                        }
                );
            }
            myNetworkThread = new MyNetworkThread(mHandler);
        }
    }

    private void showMessage(CharSequence msg) {
        myText.append(System.getProperty("line.separator"));
        myText.append(msg);
        //myText.setText(msg);
    }

    private class MyActionListner implements WifiP2pManager.ActionListener {
        @Override
        public void onSuccess() {
            Log.v(TAG1, " WFD info set Successfully");
        }

        @Override
        public void onFailure(int reason) {
            Log.v(TAG1, " WFD info set Failed with reason : " + reason);
        }
    }

    private void setWfdInfo()
    {
        // Set WFD info to supplicant
        Log.v(TAG1, "Setting WFD Info");
        String className = "android.net.wifi.p2p.WifiP2pWfdInfo";
        try {
            Class classtoSearch = Class.forName(className);
            Log.v(TAG1, "Getting WFDInfo constructor");
            Constructor constructor = classtoSearch.getConstructor();
            Log.v(TAG1, "Getting WFDInfo instance...");
            Object newInstance = constructor.newInstance();
            Log.v(TAG1, "Got WFDInfo instance");


            Method isWfdEnabled = newInstance.getClass().getDeclaredMethod("isWfdEnabled", null);
            isWfdEnabled.setAccessible(true);
            boolean bIsWFDEnabled = (boolean) isWfdEnabled.invoke(newInstance, null);
            if (bIsWFDEnabled) {
                Log.v(TAG1, "WFD already enabled, RETURNING !!!!");
                return;
            }

            Class[] param1 = new Class[1];
            param1[0] = int.class;
            Method setDeviceType = newInstance.getClass().getDeclaredMethod("setDeviceType", param1);

            setDeviceType.setAccessible(true);
            Object obj1 = new Object();
            obj1 = new Integer(1);
            setDeviceType.invoke(newInstance, obj1);
            Log.v(TAG1, "Set WFD Device type");

            Class[] param2 = new Class[1];
            param2[0] = int.class;
            Method setControlPort = classtoSearch.getDeclaredMethod("setControlPort", param2);
            setControlPort.setAccessible(true);
            Object obj2 = new Object();
            obj2 = new Integer(7236);
            setControlPort.invoke(newInstance, obj2);
            Log.v(TAG1, "Set WFD Control Port to 7236");

            Class[] param3 = new Class[1];
            param3[0] = int.class;
            Method setMaxThroughput = classtoSearch.getDeclaredMethod("setMaxThroughput",param3);
            setMaxThroughput.setAccessible(true);
            Object obj3 = new Object();
            obj3 = new Integer(50);
            setMaxThroughput.invoke(newInstance, obj3);
            Log.v(TAG1, "Set WFD setMaxThroughput to 50");

            Class[] param4 = new Class[1];
            param4[0] = boolean.class;
            Method setWfdEnabled = classtoSearch.getDeclaredMethod("setWfdEnabled",param4);
            setWfdEnabled.setAccessible(true);
            Object obj4 = new Object();
            boolean enable = true;
            obj4 = enable;
            setWfdEnabled.invoke(newInstance, obj4);
            Log.v(TAG1, "Set WFD Enabled");

            Class[] param5 = new Class[1];
            param5[0] = boolean.class;
            Method setSessionAvailable = classtoSearch.getDeclaredMethod("setSessionAvailable",param4);
            setSessionAvailable.setAccessible(true);
            Object obj5 = new Object();
            boolean enabled = true;
            obj5 = enabled;
            setSessionAvailable.invoke(newInstance, obj5);
            Log.v(TAG1, "Set Session Available");

            String methodToInvoke = "setWFDInfo";
            mListner = new MyActionListner();

            Class params[] = new Class[3];
            params[0] = mP2pChannel.getClass();
            params[1] = newInstance.getClass();
            params[2] = WifiP2pManager.ActionListener.class;

            Method setWFDInfo = WifiP2pManager.class.getDeclaredMethod(methodToInvoke, params);
            Log.v(TAG1, "Got setWFDInfo Method");
            setWFDInfo.setAccessible(true);
            Log.v(TAG1, "Set setWFDInfo Method Accessible");

            Object[] args = new Object[3];
            args[0] = mP2pChannel;
            args[1] = newInstance;
            args[2] = mListner;
            setWFDInfo.invoke(mWifiP2p, args);

            Log.v(TAG1, "Invoked setWFDInfo Method");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

   /* private int getRemoteControlPort(WifiP2pDevice dev){
        int port = 0;


        try {
            Field wfdInfo = WifiP2pDevice.class.getDeclaredField("wfdInfo");
            wfdInfo.setAccessible(true);
            port = (int) wfdInfo.
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return port;
    }*/

    private String getRemoteMACAddress(WifiP2pGroup p2pGroup) {

            List devList = new ArrayList();
            if (devList.addAll(p2pGroup.getClientList())) {
                if (devList.size() == 0) {
                    Log.e(TAG, "No Remote client found");
                    return null;
                }

                Log.v(TAG1, "P2P Group Client Size : " + devList.size());
                for (int i = 0; i < devList.size(); i++) {
                    WifiP2pDevice peerDevice = (WifiP2pDevice) devList.get(i);
                    return peerDevice.deviceAddress;
                }
            } else {
                Log.v(TAG1, "There are no clients in the group");
            }
            return null;
    }

    private String getIP(String mac) {
        String ip = null;
        try {

            Log.v(TAG, "ARP table Contains : ");
            BufferedReader tr = new BufferedReader(new FileReader("/proc/net/arp"));
            String line1 = "";
            while((line1 = tr.readLine()) != null) {
                Log.v(TAG, " " + line1);

            }
            tr.close();
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line = "";
            while((line = br.readLine()) != null) {
                // Log.v(TAG, " " + line);
                String[] tokens = line.split("\\s+");
                // The ARP table has the form:
                //   IP address        HW type    Flags     HW address           Mask   Device
                //   192.168.178.21    0x1        0x2       00:1a:2b:3c:4d:5e    *      tiwlan0
                // Log.v(TAG, "Token[3] : " + tokens[3]);
                if ((mac == null) && tokens[5].equalsIgnoreCase("p2p0")) {
                    ip = tokens[0];
                }
                if(tokens.length >= 4 && tokens[3].equalsIgnoreCase(mac)) {
                    ip = tokens[0];
                    //  Log.v(TAG, "ip : " + ip);
                    break;
                }
                //Log.v(TAG, "Token[3] : " + tokens[3]);
            }
            br.close();
        }
        catch(Exception e) { Log.e(TAG, e.toString()); }
        return ip;
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG1, "Recieved intent : " + action);
            showMessage("Recieved intent : " + action);

            if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
                WifiP2pInfo p2pInfo =(WifiP2pInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);

                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                WifiP2pGroup p2pGroup = (WifiP2pGroup) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);

                if (networkInfo.isConnected()) {
                    //WifiP2pInfo details...
                    if (p2pInfo.isGroupOwner) {
                        //mRemoteAddress = getIP("0c:d2:ab:00:01:dd");
                        try {
                            Log.v(TAG1, "Making current thread sleep for 2000ms");
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mRemoteAddress = getIP(null);
                        Log.v(TAG1, "Remote Device's Address : " + mRemoteAddress);

                    }

                    Log.v(TAG1, "Is GO : " + (String) ((p2pInfo.isGroupOwner) ? "Yes" : "No"));
                    Log.v(TAG1, "P2pInfo details : " + p2pInfo.toString());
                    showMessage(p2pInfo.toString());

                    //Netwrok info details....
                    Log.v(TAG1, "Is connected : " + (String) (networkInfo.isConnected() ? "Yes" : "No"));
                    Log.v(TAG1, "NetworkInfo details : " + networkInfo.toString());

                    //P2pGroup details....
                    Log.v(TAG1, "group interface : " + p2pGroup.getInterface());
                    Log.v(TAG1, "p2p Group details : " + p2pGroup.toString());
                }
                else {
                    Log.v(TAG1, "Connection not stablished");
                    showMessage("Connection not stablished");
                }

                //Retrive URI and proceed with RTSP Client creation

                if (networkInfo.isConnected()) {

                    if (!p2pInfo.isGroupOwner) {
                        mRemoteAddress = p2pInfo.groupOwnerAddress.getHostAddress();
                        //WifiP2pDevice dev = p2pGroup.getOwner();
                        //mControlPort = getRemoteControlPort(dev);
                        Log.v(TAG1, "Remote device is group owner, Remote Device Address : " + mRemoteAddress);
                    } else if (mRemoteAddress == null) {
                       /* myNetworkThread.checkClients();
                        try {
                            Log.v(TAG1, "Making current thread sleep for 1000ms");
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }*/
                        String remoteMAC = getRemoteMACAddress(p2pGroup);
                        if (remoteMAC == null) {
                            Log.e(TAG1, "Failed to get Remote device MAC");
                            return;
                            // need to finish the activity...
                        }
                        mRemoteAddress = getIP(remoteMAC);

                        Log.v(TAG1, "Host device is group owner, Remote Device Address: " + mRemoteAddress);
                    } else {
                        if (myNetworkThread != null) {
                           // myNetworkThread.isRechable(mRemoteAddress);
                            /*try {
                                Log.v(TAG1, "Making current thread sleep for 500ms.....");
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }*/
                        }
                    }
                    Uri uri = Uri.parse("wfd://" + mRemoteAddress + ":" + mControlPort);
                    Intent lIntent = new Intent();

                    lIntent.setClassName("com.example.myapplication", "com.example.myapplication.MyWFDPlayer");
                    lIntent.setData(uri);
                    startActivity(lIntent);

                    // Stop P2P disovery
                    mWifiP2p.stopPeerDiscovery(mP2pChannel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.v(TAG1, "Peers Discovery stopped successfully");
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.v(TAG1, "Peers Discovery failed with reason : " + reason);
                        }
                    });
                }

               /* if (networkInfo.isConnected() && myNetworkThread == null) {
                 //   mNetworkThread = new NetworkThread();
                    // we can add a handler to get notification from MyNetworkThread
                 //   mNetworkThread.start();

                    myNetworkThread = new MyNetworkThread(mHandler);
                    try {
                        Log.v(TAG1, "Making current thread sleep for 100ms");
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (myNetworkThread != null) {
                        Log.v(TAG1, "My Network thread created Successfully");
                        if (!p2pInfo.isGroupOwner) {
                            Log.v(TAG1, "Remote device is group owner");
                            myNetworkThread.createRTSPClient(p2pInfo.groupOwnerAddress);
                        } else {
                            String remoteMAC = getRemoteMACAddress(p2pGroup);
                            if (remoteMAC == null) {
                                Log.e(TAG1, "Failed to get Remote device MAC");
                                return;
                                // need to finish the activity...
                            }
                            myNetworkThread.checkClients();
                            myNetworkThread.createRTSPClient(remoteMAC);

                        }
                    }
                }*/

            } else if (action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, 0);

                if (WifiP2pManager.WIFI_P2P_STATE_ENABLED == state) {
                    Log.i(TAG1, "P2P is Enabled");
                    showMessage("P2P is Enabled");
                    // St Wfd Info
                    setWfdInfo();
                    // start P2P discovery

                    mWifiP2p.discoverPeers(mP2pChannel, new WifiP2pManager.ActionListener() {
                                @Override
                                public void onSuccess() {
                                    Log.v(TAG1, "Discover peers successful");
                                }

                                @Override
                                public void onFailure(int reason) {
                                    Log.v(TAG1, "Discover peers failed with reason : " + reason);
                                }
                        }
                    );
                } else if (WifiP2pManager.WIFI_P2P_STATE_DISABLED == state) {
                    Log.i(TAG1, "P2P is Disabled");
                    showMessage("P2P is Disabled");

                } else {
                    Log.e(TAG1, "Unknown State");
                }


            } else if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {

                WifiP2pDeviceList list = (WifiP2pDeviceList) intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);

                List devList = new ArrayList();
                if (devList.addAll(list.getDeviceList())) {

                    if (devList.size() == 0) {
                        Log.e(TAG1, "No Peers found");
                        return;
                    }
                    Log.v(TAG1, "Following Peers detected :");
                    for (int i = 0; i < devList.size(); i++) {
                        WifiP2pDevice peerDevice = (WifiP2pDevice) devList.get(i);
                        Log.v(TAG1, "Peer " + i + ": " + peerDevice.toString());
                    }
                }

            } else if (action.equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)) {

                WifiP2pDevice thisDevice = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                Log.v(TAG1, "Host device details : " + thisDevice.toString());

            } else if (action.equals(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)) {

                int discovery = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 0);
                if (WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED == discovery) {
                    Log.v(TAG1, "Discovery started");

                } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED == discovery) {
                    Log.v(TAG1, "Discovery stopped");
                } else {
                    Log.e(TAG1, "Discovery returned wrong value : " + discovery);
                }

            }
        }
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //Log.v(TAG1, "Received Msg form NetworkThread. What : " + msg.what);
            switch(msg.what) {
                case 1:
                {
                    Bundle bun = msg.getData();
                    String textMsg = bun.getString("message");
                    showMessage(textMsg);
                    break;
                }
                default:
                    Log.e(TAG1, "No Such case");
                    showMessage("No Such case");
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
