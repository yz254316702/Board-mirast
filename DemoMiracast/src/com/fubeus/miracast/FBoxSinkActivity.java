
package com.fubeus.miracast;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.StringTokenizer;

public class FBoxSinkActivity extends Activity {
    public static final String TAG = "fbox_sink";

    public static final String KEY_IP = "ip";
    public static final String KEY_PORT = "port";
    private File mFolder = new File("/data/data/com.fubeus.miracast");
    private String strSessionID = null;
    private String strIP = null;
    private String mIP;
    private String mPort;
    private boolean mMiracastRunning = false;
    private PowerManager.WakeLock mWakeLock;
    private Handler mMiracastHandler = null;
    private SurfaceView mSurfaceView;
    protected Handler mSessionHandler;
    private static final int CMD_MIRACAST_FINISHVIEW = 1;
    private static final int CMD_MIRACAST_EXIT = 2;
    private boolean mEnterStandby = false;
    private Context mContext;

    static {
        try {
            System.loadLibrary("wfd_fbox_jni");
        } catch (Exception e) {
            Log.d(TAG, "pradeep File error");
        }
    }

    private FileObserver mFileObserver = new FileObserver(mFolder.getPath(), FileObserver.MODIFY) {
        public void onEvent(int event, String path) {
            Log.d(TAG, "File changed : path=" + path + " event=" + event);
            if (null == path) {
                return;
            }

            if (path.equals(new String("sessionId"))) {
                File ipFile = new File(mFolder, path);
                String fullName = ipFile.getPath();
                parseSessionId(fullName);
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (!networkInfo.isConnected() && mMiracastRunning) {
                    stopMiracast(true);
                    finishView();
                }
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                Log.d(TAG, "ACTION_SCREEN_OFF");
                mEnterStandby = true;
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Log.e(TAG, "ACTION_SCREEN_ON");
                if (mEnterStandby) {
                    if (mMiracastRunning) {
                        stopMiracast(true);
                        finishView();
                    }
                    mEnterStandby = false;
                }
            }
        }
    };

    private void finishView() {
        Log.e(TAG, "finishView");
        Message msg = Message.obtain();
        msg.what = CMD_MIRACAST_FINISHVIEW;
        mSessionHandler.sendMessage(msg);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.sink);
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mSurfaceView = (SurfaceView) findViewById(R.id.wifiDisplaySurface);
        mContext = this;

        mSurfaceView.getHolder().addCallback(new SurfaceCallback());
        mSurfaceView.getHolder().setKeepScreenOn(true);
        mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        mPort = bundle.getString(KEY_PORT);
        mIP = bundle.getString(KEY_IP);
        MiracastThread mMiracastThread = new MiracastThread();
        new Thread(mMiracastThread).start();
        synchronized (mMiracastThread) {
            while (null == mMiracastHandler) {
                try {
                    mMiracastThread.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        mFileObserver.startWatching();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        mWakeLock.acquire();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, intentFilter);


        mSessionHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "handleMessage, msg.what=" + msg.what);
                switch (msg.what) {
                    case CMD_MIRACAST_FINISHVIEW:
                        Window window = getWindow();
                        WindowManager.LayoutParams wl = window.getAttributes();
                        wl.alpha = 0.0f;
                        window.setAttributes(wl);
                        Intent homeIntent = new Intent(FBoxSinkActivity.this, FboxWifiDirectMainActivity.class);
                        homeIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                        FBoxSinkActivity.this.startActivity(homeIntent);
                        FBoxSinkActivity.this.finish();
                        break;
                    case CMD_MIRACAST_EXIT:
                        unregisterReceiver(mReceiver);
                        stopMiracast(true);
                        mWakeLock.release();
                        quitLoop();
                        mFileObserver.stopWatching();
                        break;
                }
            }
        };
    }

    protected void onDestroy() {
        super.onDestroy();
        if (mSessionHandler != null) {
            mSessionHandler.removeCallbacksAndMessages(null);
        }
        mSessionHandler = null;
        Log.d(TAG, "Sink Activity destory");
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(mContext)
                .setTitle("FBox sink stop")
                .setMessage("Are you sure want to stop FBox sink?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (mMiracastRunning) {
                            stopMiracast(true);
                            dialog.cancel();
                        }
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "sink activity onResume");
    }

    private boolean parseSessionId(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        String info = new String();
        try {
            reader = new BufferedReader(new FileReader(file));
            info = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (info == null) {
                    Log.d(TAG, "parseSessionId info is NULL");
                    return false;
                } else {
                    StringTokenizer strToke = new StringTokenizer(info, " ");
                    strSessionID = strToke.nextToken();
                    String sourceInfo = "IP address: " + strIP + ",  session Id: " + strSessionID;
                    Log.e("wpa_supplicant", sourceInfo);
                    return true;
                }
            } else
                return false;
        }
    }

    private String getlocalip() {
        StringBuilder IFCONFIG = new StringBuilder();
        String ipAddr = "192.168.43.1";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && !inetAddress.isLinkLocalAddress()
                            && inetAddress.isSiteLocalAddress()) {
                        IFCONFIG.append(inetAddress.getHostAddress().toString());
                        ipAddr = IFCONFIG.toString();
                    }
                }
            }
        } catch (Exception ex) {
        }
        return ipAddr;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, " start sink activity onPause");
        if (mIP != null) {
            Message msg = Message.obtain();
            msg.what = CMD_MIRACAST_EXIT;
            mSessionHandler.sendMessage(msg);
        }
        Log.d(TAG, " end sink activity onPause");
    }


    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
    }

    public void startMiracast(String ip, String port) {
        mMiracastRunning = true;
        Message msg = Message.obtain();
        msg.what = CMD_MIRACAST_START;
        Bundle data = msg.getData();
        data.putString(KEY_IP, ip);
        data.putString(KEY_PORT, port);
        if (mMiracastHandler != null) {
            mMiracastHandler.sendMessage(msg);
        }
    }

    public void stopMiracast(boolean owner) {
        if (mMiracastRunning && owner) {
            mMiracastRunning = false;
            nativeSetTeardown();
            nativeDisconnectSink();
        } else if (mMiracastRunning) {
            mMiracastRunning = false;
            nativeDisconnectSink();
        }
        mIP = null;
        mPort = null;
        finish();
    }

    private native void nativeConnectWifiSource(FBoxSinkActivity sink, Surface surface, String ip, int port);

    private native void nativeDisconnectSink();

    private native void nativeSetTeardown();

    private final int CMD_MIRACAST_START = 10;

    class MiracastThread implements Runnable {
        public void run() {
            Looper.prepare();
            mMiracastHandler = new Handler() {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case CMD_MIRACAST_START: {
                            Bundle data = msg.getData();
                            String ip = data.getString(KEY_IP);
                            String port = data.getString(KEY_PORT);
                            nativeConnectWifiSource(FBoxSinkActivity.this, mSurfaceView.getHolder().getSurface(), ip, Integer.parseInt(port));
                        }
                        break;
                        default:
                            break;
                    }
                }
            };

            synchronized (this) {
                notifyAll();
            }
            Looper.loop();
        }
    }

    ;

    public void quitLoop() {
        if (mMiracastHandler != null && mMiracastHandler.getLooper() != null) {
            Log.v(TAG, "miracast thread quit");
            mMiracastHandler.removeCallbacksAndMessages(null);
            mMiracastHandler.getLooper().quit();
            mMiracastHandler = null;
        }
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // TODO Auto-generated method stub
            Log.v(TAG, "surfaceChanged");
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.e(TAG, "surfaceCreated mSurfaceView.getHolder().getSurface() is" + mSurfaceView.getHolder().getSurface() + "and holder.getSurface() is %p" + holder.getSurface() + mMiracastRunning + mEnterStandby);
            if (mIP == null) {
                finishView();
                return;
            }
            if (mMiracastRunning == false && mEnterStandby == false) {
                startMiracast(mIP, mPort);
                strIP = getlocalip();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.v(TAG, "surfaceDestroyed");
            if (mMiracastRunning)
                stopMiracast(true);
        }
    }
}
