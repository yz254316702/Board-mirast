package com.fubeus.fubeustv.fboxreversemiracast;


import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class FboxWifiDirectMainActivity extends Activity implements
        ChannelListener, PeerListListener, ConnectionInfoListener, GroupInfoListener {
    public static final String TAG = "FboxWifiDirectMainActivity";
    public static final boolean DEBUG = true;
    public static final String HRESOLUTION_DISPLAY = "display_resolution_hd";
    public static final String WIFI_P2P_IP_ADDR_CHANGED_ACTION = "android.net.wifi.p2p.IPADDR_INFORMATION";
    public boolean mForceStopScan = false;
    public static final String WIFI_P2P_PEER_IP_EXTRA = "IP_EXTRA";
    public static final String WIFI_P2P_PEER_MAC_EXTRA = "MAC_EXTRA";
    public boolean mStartConnecting = false;
    public boolean mManualInitWfdSession = false;
    public int mConnectImageNum;

    private WifiP2pManager manager;
    private WifiManager mWifiManager;
    private boolean isWifiP2pEnabled = false;
    private String mPort;
    private String mIP;
    private Handler mHandler = new Handler();
    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver mReceiver = null;
    private PowerManager.WakeLock mWakeLock;
    private TextView mConnectWarn;
    private TextView mConnectDesc;
    private TextView mPeerList;
    private boolean retryChannel = false;
    private WifiP2pDevice mDevice = null;
    private ArrayList<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private ProgressDialog progressDialog = null;
    private TextView mDeviceNameShow;
    private TextView mDeviceTitle;
    private String mSavedDeviceName;
    private int mNetId = -1;
    private SharedPreferences mPref;
    private SharedPreferences.Editor mEditor;
    private ArrayList<String> mDeviceList;
    private InputMethodManager imm;
    private boolean mFirstInit = false;

    private void initCert() {
        mStartConnecting = false;
        mDeviceList = new ArrayList<String>();
        mDeviceList.add(" ");
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    private void requestPeers() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        manager.requestPeers(channel, new PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerLists) {
                peers.clear();
                for (WifiP2pDevice device : peerLists.getDeviceList()) {
                    peers.add(device);
                }
                String list = FboxWifiDirectMainActivity.this.getResources().getString(R.string.peer_list);
                for (int i = 0; i < peers.size(); i++) {
                    list += "    " + peers.get(i).deviceName;
                    mPeerList.setText(list);
                }
            }
        });
    }


    private void tryDiscoverPeers() {
        mForceStopScan = false;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        manager.discoverPeers(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                requestPeers();
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Discover peers failed with reason " + reason + ".");
            }
        });
    }

    public void stopPeerDiscovery() {
        mForceStopScan = true;
        manager.stopPeerDiscovery(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Stop peer discovery succeed.");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Stop peer discovery failed with reason " + reason + ".");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        manager.removeGroup(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "removeGroup Success");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "removeGroup Failure");
            }
        });
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, TAG);
        mWakeLock.acquire();
        if (mFirstInit) {
            Intent intent = new Intent(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            removeStickyBroadcastAsUser(intent, UserHandle.ALL);
        } else {
            mFirstInit = true;
        }
        initCert();
        mConnectImageNum = 5;
        mConnectDesc = (TextView) findViewById(R.id.show_connect_desc);
        mConnectWarn = (TextView) findViewById(R.id.show_desc_more);
        mConnectDesc.setFocusable(true);
        mConnectDesc.requestFocus();
        mPeerList = (TextView) findViewById(R.id.peer_devices);
        if (!isNetAvailiable()) {
            mConnectWarn.setText(FboxWifiDirectMainActivity.this.getResources()
                    .getString(R.string.p2p_off_warning));
            mConnectWarn.setVisibility(View.VISIBLE);
            mConnectDesc.setFocusable(false);
        }
        mDeviceNameShow = (TextView) findViewById(R.id.device_dec);
        mDeviceTitle = (TextView) findViewById(R.id.device_title);
        if (mDevice != null) {
            mSavedDeviceName = mDevice.deviceName;
            mDeviceNameShow.setText(mSavedDeviceName);
        } else {
            mDeviceTitle.setVisibility(View.INVISIBLE);
        }
        resetData();
        registerReceiver(mReceiver, intentFilter);
        tryDiscoverPeers();
    }

    private final Runnable startSearchRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mForceStopScan) {
                Log.d(TAG, "ForceStopScan = false ; startSearchRunnable.");
                startSearch();
            }
        }
    };

    private final Runnable searchAgainRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mForceStopScan && peers.isEmpty()) {
                if (DEBUG)
                    Log.d(TAG, "mForceStopScan is false, no peers, search again.");
                startSearch();
            }
        }
    };

    public void cancelSearchTimer() {
        if (DEBUG) Log.d(TAG, " cancelSearchTimer");
        mHandler.removeCallbacks(startSearchRunnable);
        if (DEBUG) Log.d(TAG, " cancel searchAgainRunnable");
        mHandler.removeCallbacks(searchAgainRunnable);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
    }

    public void setDevice(WifiP2pDevice device) {
        mDevice = device;
        if (mDevice != null) {
            if (mDeviceTitle != null) {
                mDeviceTitle.setVisibility(View.VISIBLE);
            }
            mSavedDeviceName = mDevice.deviceName;
            if (mDeviceNameShow != null) {
                mDeviceNameShow.setText(mSavedDeviceName);
            }
        }
        if ((WifiP2pDevice.CONNECTED == mDevice.status)
                || (WifiP2pDevice.INVITED == mDevice.status)) {
            cancelSearchTimer();
        }
        if (DEBUG) {
            Log.d(TAG, "localDevice name:" + mDevice.deviceName + ", status:" + mDevice.status + " (0-CONNECTED,3-AVAILABLE)");
        }
    }

    public void startSearch() {
        if (DEBUG) {
            Log.d(TAG, "startSearch wifiP2pEnabled:" + isWifiP2pEnabled);
        }
        if (mHandler.hasCallbacks(startSearchRunnable))
            cancelSearchTimer();
        if (!isWifiP2pEnabled) {
            if (manager != null && channel != null) {
                mConnectWarn.setVisibility(View.VISIBLE);
                mConnectWarn.setText(FboxWifiDirectMainActivity.this.getResources()
                        .getString(R.string.p2p_off_warning));
                mConnectDesc.setFocusable(false);
            }
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        mWakeLock.release();
        stopPeerDiscovery();
    }

    @Override
    public void onChannelDisconnected() {
        if (manager != null && !retryChannel) {
            Toast.makeText(this, FboxWifiDirectMainActivity.this.getResources().getString(R.string.channel_try),
                    Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(
                    this,
                    FboxWifiDirectMainActivity.this.getResources().getString(R.string.channel_close),
                    Toast.LENGTH_LONG).show();
        }
    }

    public void resetData() {
        String sFinal1 = String.format(getString(R.string.connect_ready), getString(R.string.device_name));
        mConnectDesc.setText(sFinal1);
        peers.clear();

        String list = FboxWifiDirectMainActivity.this.getResources().getString(R.string.peer_list);
        mPeerList.setText(list);
    }

    public void setConnect() {
        mConnectDesc.setText(getString(R.string.connected_info));
    }

    public void setIsWifiP2pEnabled(boolean enable) {
        this.isWifiP2pEnabled = enable;
        String sFinal1 = String.format(getString(R.string.connect_ready), getString(R.string.device_name));
        mConnectDesc.setText(sFinal1);
        if (enable) {
            mConnectWarn.setVisibility(View.INVISIBLE);
            mConnectDesc.setFocusable(false);
        } else {
            mConnectWarn.setText(FboxWifiDirectMainActivity.this.getResources()
                    .getString(R.string.p2p_off_warning));
            mConnectWarn.setVisibility(View.VISIBLE);
            mConnectDesc.setFocusable(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_layout);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        intentFilter.addAction(WIFI_P2P_IP_ADDR_CHANGED_ACTION);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        if (manager != null) {
            manager.setDeviceName(channel, "FBox_Sink",
                    new ActionListener() {
                        public void onSuccess() {

                            mDeviceNameShow.setText(mSavedDeviceName);
                        }

                        public void onFailure(int reason) {

                        }
                    });
        }

        mReceiver = new WiFiDirectBroadcastReceiver(this,manager, channel, FboxWifiDirectMainActivity.this);
        mPref = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mPref.edit();
        changeRole(true);
    }

    public void startSearchTimer() {
        if (DEBUG) Log.d(TAG, " startSearchTimer 6s");
        mHandler.postDelayed(startSearchRunnable, 6000);
    }

    public void discoveryStop() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public void startMiracast(String ip, String port) {
        mPort = port;
        mIP = ip;
        if (mManualInitWfdSession) {
            Log.d(TAG, "waiting startMiracast");
            return;
        }
        setConnect();
        Log.d(TAG, "start miracast");
        Intent intent = new Intent(FboxWifiDirectMainActivity.this, FBoxSinkActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(FBoxSinkActivity.KEY_PORT, mPort);
        bundle.putString(FBoxSinkActivity.KEY_IP, mIP);
        bundle.putBoolean(HRESOLUTION_DISPLAY, mPref.getBoolean(HRESOLUTION_DISPLAY, true));
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        changeRole(false);
        Log.d(TAG, "onDestroy do stopPeerDiscovery");
        setIsWifiP2pEnabled(false);
        resetData();
        mFirstInit = false;
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList devicelist) {
        String list = FboxWifiDirectMainActivity.this.getResources().getString(R.string.peer_list);
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        Log.d(TAG, "===onPeersAvailable===");
        peers.clear();
        mDeviceList.clear();
        mDeviceList.add(" ");
        peers.addAll(devicelist.getDeviceList());
        freshView();
        for (int i = 0; i < peers.size(); i++) {
            if (!peers.get(i).wfdInfo.isWfdEnabled()) {
                Log.d(TAG, "peerDevice:" + peers.get(i).deviceName + " is not a wfd device");
                continue;
            }
            if (!peers.get(i).wfdInfo.isSessionAvailable()) {
                Log.d(TAG, "peerDevice:" + peers.get(i).deviceName + " is an unavailable wfd session");
                continue;
            }

            if ((WifiP2pDevice.INVITED == peers.get(i).status)
                    || (WifiP2pDevice.CONNECTED == peers.get(i).status))
                cancelSearchTimer();
            list += "    " + peers.get(i).deviceName;
            mDeviceList.add(peers.get(i).deviceName);
            if (DEBUG)
                Log.d(TAG, "peerDevice:" + peers.get(i).deviceName + ", status:" + peers.get(i).status + " (0-CONNECTED,3-AVAILABLE)");
        }
        mPeerList.setText(list);
        Log.d(TAG, "===onPeersAvailable===");

        if (!mForceStopScan)
            mHandler.postDelayed(searchAgainRunnable, 5000);
    }

    public void onGroupInfoAvailable(WifiP2pGroup group) {
        if (group != null) {
            Log.d(TAG, "onGroupInfoAvailable true : " + group);
            mNetId = group.getNetworkId();
        } else {
            Log.d(TAG, "onGroupInfoAvailable false");
            mNetId = -1;
        }
    }

    /**
     * @Description TODO
     */
    private void freshView() {
        for (int i = 0; i < peers.size(); i++) {
            if (peers.get(i).status == WifiP2pDevice.CONNECTED) {
                mConnectDesc.setText(getString(R.string.connecting_desc)
                        + peers.get(i).deviceName);
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private boolean isNetAvailiable() {
        if (mWifiManager != null) {
            int state = mWifiManager.getWifiState();
            if (WifiManager.WIFI_STATE_ENABLING == state
                    || WifiManager.WIFI_STATE_ENABLED == state) {
                Log.d(TAG, "WIFI enabled");
                return true;
            } else {
                Log.d(TAG, "WIFI disabled");
                return false;
            }
        }
        return false;
    }


    private void changeRole(boolean isSink) {
        WifiP2pWfdInfo wfdInfo = new WifiP2pWfdInfo();

        if (isSink) {
            wfdInfo.setWfdEnabled(true);
            wfdInfo.setDeviceType(WifiP2pWfdInfo.PRIMARY_SINK);
            wfdInfo.setSessionAvailable(true);
            wfdInfo.setControlPort(7236);
            wfdInfo.setMaxThroughput(50);
        } else {
            wfdInfo.setWfdEnabled(false);
        }

        manager.setWFDInfo(channel, wfdInfo, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Successfully set WFD info.");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Failed to set WFD info with reason " + reason + ".");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                FboxWifiDirectMainActivity.this.changeRole(true);
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

    }
}
