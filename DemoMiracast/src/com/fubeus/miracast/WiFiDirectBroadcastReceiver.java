package com.fubeus.miracast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private String mWfdMac;
    private String mWfdPort;
    private boolean mWfdIsConnected = false;
    private boolean mSinkIsConnected = false;
    private WifiP2pManager manager;
    private Channel channel;
    private FboxWifiDirectMainActivity activity;
    private final String DEFAULT_PORT = "7236";
    Object lock = new Object();

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
                                       FboxWifiDirectMainActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                activity.setIsWifiP2pEnabled(true);
            } else {
                activity.setIsWifiP2pEnabled(false);
                activity.resetData();

            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
           if (manager != null && !activity.mForceStopScan && !activity.mStartConnecting) {
                manager.requestPeers(channel, (PeerListListener) activity);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (manager == null) {
                return;
            }
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            WifiP2pInfo p2pInfo = (WifiP2pInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
            WifiP2pGroup p2pGroup = (WifiP2pGroup) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
            if (networkInfo.isConnected()) {
                mWfdIsConnected = true;
                if (p2pGroup.isGroupOwner() == true) {
                    WifiP2pDevice device = null;
                    for (WifiP2pDevice c : p2pGroup.getClientList()) {
                        device = c;
                        break;
                    }
                    if (device != null && device.wfdInfo != null) {
                        mWfdPort = String.valueOf(device.wfdInfo.getControlPort());
                        mWfdMac = device.deviceAddress;
                    }
                } else {
                    WifiP2pDevice device = p2pGroup.getOwner();
                    if (device != null && device.wfdInfo != null) {
                        mWfdPort = String.valueOf(device.wfdInfo.getControlPort());
                        if (mWfdPort.equals(DEFAULT_PORT))
                            activity.startMiracast(p2pInfo.groupOwnerAddress.getHostAddress(), mWfdPort);
                        else {
                           activity.startMiracast(p2pInfo.groupOwnerAddress.getHostAddress(), DEFAULT_PORT);
                        }
                    }
                }
                mSinkIsConnected = false;
            } else {
                mWfdIsConnected = false;
                activity.resetData();
                if (!activity.mForceStopScan)
                    activity.startSearch();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            activity.resetData();
            activity.setDevice((WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
        } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
            int discoveryState = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE,
                    WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED);
            if (activity != null && discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
                activity.discoveryStop();
                if (!activity.mForceStopScan && !activity.mStartConnecting)
                    activity.startSearchTimer();
            }
        } else if (FboxWifiDirectMainActivity.WIFI_P2P_IP_ADDR_CHANGED_ACTION.equals(action)) {
            String ipaddr = intent.getStringExtra(FboxWifiDirectMainActivity.WIFI_P2P_PEER_IP_EXTRA);
            String macaddr = intent.getStringExtra(FboxWifiDirectMainActivity.WIFI_P2P_PEER_MAC_EXTRA);
            ;
            if (ipaddr != null && macaddr != null) {
                if (mWfdIsConnected) {
                    if (!mSinkIsConnected) {
                        if ((mWfdMac.substring(0, 11)).equals(macaddr.substring(0, 11))) {
                            activity.startMiracast(ipaddr, mWfdPort);
                            mSinkIsConnected = true;
                        }
                    }
                }
            }
        }
    }
}