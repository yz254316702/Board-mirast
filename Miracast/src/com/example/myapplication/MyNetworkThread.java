package com.example.myapplication;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.os.Handler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by anushanker on 19/1/16.
 */

public class MyNetworkThread extends Thread {
    private final String TAG = "NetworkThread";
    private InetAddress mRemoteDevice;
    private InetAddress mDefaultRemoteDevice = null;
    private Socket mRTSPSocket;
    private BufferedInputStream mInStream;
    private BufferedOutputStream mOutStream;
    private MyHandler mHandler=null;
    private Message mNotify;
    private Handler mMainHandler;
    enum message {
        kWhatstartRTSPClient,
        kWhatCloseRTSPClient,
    }

    @Override
    public void run() {
        super.run();
        Looper.prepare();
        mHandler = new MyHandler();
        if (mHandler == null) {
            Log.e(TAG, "Error creating Handler");
        } else {
            Log.v(TAG, "Handler is created Successfully");
        }
        Log.v(TAG, "Thread Started");
        Looper.loop();
    }

    MyNetworkThread(Handler handler) {
        super("Network_Thread");
        Log.v(TAG, "My Network Thread Created");
        mMainHandler = handler;
        start();
    }
    // Handler class
    private class MyHandler extends Handler {

        MyHandler() {
            super();
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);


            switch (msg.what) {
                case 1: {
                    Log.v(TAG, "Creating RTSP Client");
                    createRTSPClientSocket();
                    break;
                }
                case 2: {
                    Log.v(TAG, "Creating RTSP Client from MAC");
                    Bundle bun = msg.getData();
                    String remoteMac = bun.getString("remoteMAC");
                    Log.v(TAG, "Remote MAC : " + remoteMac);
                    String remoteIP = getIP(remoteMac);
                    Log.v(TAG, "Remote IP : " + remoteIP);
                    try {
                        mRemoteDevice = InetAddress.getByName(remoteIP);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    createRTSPClientSocket();
                    break;
                }
                case 3: {
                    Log.v(TAG, "Closing RTSP Client");
                    try {
                        if (mRTSPSocket != null) {
                            mRTSPSocket.close();
                        }
                        mRTSPSocket = null;
                        Looper looper = this.getLooper();
                        looper.quitSafely();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case 4: {
                    readMsg();
                    break;
                }
                case 5: {

                    checkP2pClients();
                    break;
                }
                case 6: {
                    Bundle data = msg.getData();
                    String address = data.getString("address");
                    checkIsReachable(address);
                    break;
                }
                default:
                    Log.e(TAG, "No Such case found");
            }
        }
    }

    public void isRechable(String remoteAddress) {
        int what = 6;
        Bundle data = new Bundle();
        data.putString("address", remoteAddress);
        Message msg = mHandler.obtainMessage();
        msg.what = what;
        msg.setData(data);
        if (msg == null) {
            Log.e(TAG, "Unable to obtain Message..1");
            return;
        }
        msg.sendToTarget();
        return;
    }

    public void createRTSPClient(InetAddress remoteDevice) {
        mRemoteDevice = remoteDevice;
        int what = 1;
        Message msg = mHandler.obtainMessage();
        msg.what = what;
        if (msg == null) {
            Log.e(TAG, "Unable to obtain Message..1");
            return;
        }
        msg.sendToTarget();
        return;
    }
    public void createRTSPClient(String remoteDevice) {

        int what = 2;
        Message msg = mHandler.obtainMessage();
        msg.what = what;
        if (msg == null) {
            Log.e(TAG, "Unable to obtain Message..2");
            return;
        }
        Bundle bun = new Bundle();
        bun.putString("remoteMAC", remoteDevice);
        msg.setData(bun);
        msg.sendToTarget();
        return;
    }

    public void closeRTSPClient() {
        int what = 3;
        Message msg = mHandler.obtainMessage();
        msg.what = what;
        if (msg == null) {
            Log.e(TAG, "Unable to obtain Message..3");
            return;
        }
        msg.sendToTarget();
        return;
    }

    private void sendNotification(String msg) {
        int what = 1;
        Bundle bun = new Bundle();
        bun.putString("message", msg);
        Message lMsg = mMainHandler.obtainMessage();
        lMsg.what = what;
        //Log.v(TAG, "Notification Msg. What : " + lMsg.what);
        lMsg.setData(bun);
        lMsg.sendToTarget();
    }

    private void checkIsReachable(String address) {
        try {
            InetAddress clientInet = InetAddress.getByName(address);
            if(clientInet.isReachable(200)) {
                Log.v(TAG, "Client " + address + "is Reachable");
                mDefaultRemoteDevice = clientInet;
                return;
            } else {
                Log.v(TAG, "No response timeout. Client " + address + "is NOT Reachable");
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void readMsg() {
        if (mRTSPSocket == null){
            Log.v(TAG,"unable to read as socket is null");
            //send error to main activity
            return;
        }
        Log.v(TAG, "Attempt to Read Data on RTSP Socket");
        try {
            int size;
            size = mInStream.available();
            if (size > 0) {
                Log.v(TAG, "Received " + size + " bytes from remote device  ");
                sendNotification("Received " + size + " bytes from remote device  ");
                byte[] bytes = new byte[size];
                int x = mInStream.read(bytes, 0, size);
                Log.v(TAG, "Read " + x + " bytes from buffer");
                String msg = new String(bytes);
                Log.v(TAG, "Recevied from Source : " + msg);
                sendNotification("Recevied from Source : " + msg);
                sendM1Response();
            } else {
                Log.w(TAG, "No Data to read for RTSP Client");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        int read = 4;
        Message msg = Message.obtain();
        if (msg == null) {
            Log.e(TAG, "Unable to obtain Message...4");
        }
        msg.what = read;
        //msg.sendToTarget();
        if (!mHandler.sendMessageDelayed(msg, 100)) {
            Log.e(TAG, "Failed to send message");
        }
    }

    private void sendMsg(String msg) {
        int err = 0;

        try {
            mOutStream.write(msg.getBytes(), 0 , msg.length());
            mOutStream.flush();
            Log.v(TAG, "Sent to Source : " + msg);
            sendNotification("Sent to Source : " + msg);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void checkClients() {
        Log.v(TAG,"checkClients");
        int what = 5;
        Message msg = null;
        msg = mHandler.obtainMessage();
        msg.what = what;
        if (msg == null) {
            Log.e(TAG,"Error obtaining Message..5");
            return;
        }
        msg.sendToTarget();
        return;
    }

    private String getIP(String mac) {
        String ip = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            Log.v(TAG, "ARP table Contains : ");

            String line = "";
            while((line = br.readLine()) != null) {
               // Log.v(TAG, " " + line);
                String[] tokens = line.split("\\s+");
                // The ARP table has the form:
                //   IP address        HW type    Flags     HW address           Mask   Device
                //   192.168.178.21    0x1        0x2       00:1a:2b:3c:4d:5e    *      tiwlan0
               // Log.v(TAG, "Token[3] : " + tokens[3]);
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

    private void createRTSPClientSocket() {
        // socket communication.....
        Log.v(TAG, "Remote device: " + mRemoteDevice);
        try {
            //mRTSPSocket = new Socket(mRemoteDevice, 7236, null, 7236);
            mRTSPSocket = new Socket();
            //socket options
            mRTSPSocket.setReuseAddress(true);
            mRTSPSocket.setReceiveBufferSize(512 * 1024);
            mRTSPSocket.setTcpNoDelay(true);

            mRTSPSocket.bind(new InetSocketAddress(7236));
            mRTSPSocket.connect(new InetSocketAddress(mRemoteDevice, 7236));
            if (mRTSPSocket == null) {
                Log.e(TAG, "Failed to create RTSP Client socket");
                return;
            } else if (mRTSPSocket.isConnected()) {
                Log.v(TAG, "Socket is connected to Remote : " + mRTSPSocket.getInetAddress().toString() + ":" + mRTSPSocket.getPort());
                Log.v(TAG, "local address: " + mRTSPSocket.getLocalAddress() + ":" + mRTSPSocket.getLocalPort() );

            } else {
                Log.e(TAG, "RTSP Client Socket is not connected");
                return;
            }



            mInStream = new BufferedInputStream(mRTSPSocket.getInputStream());
            mOutStream = new BufferedOutputStream(mRTSPSocket.getOutputStream());
            //start reading data on RTSP client
            readMsg();

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void checkP2pClients(){
        Log.v(TAG,"checkP2pClients");
        String temp = "192.168.49.";

        for (int i = 2; i<= 255; i++) {
            String clientIP = temp + i;
            try {
                InetAddress clientInet = InetAddress.getByName(clientIP);
                if(clientInet.isReachable(100)) {
                    Log.v(TAG, "Client " + clientIP + "is Reachable");
                    mDefaultRemoteDevice = clientInet;
                    return;
                } else {
                    Log.v(TAG, "No response timeout");
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return;
    }

    private void sendM1Response() {
        String msg = "RTSP/1.0 200 OK\r\n" +
                "CSeq: 0\r\n" +
                "Public: org.wfa.wfd1.0, GET_PARAMETER, SET_PARAMETER\r\n";
        sendMsg(msg);

        //Send M2
        sendM2();
    }

    private void sendM2() {
        String msg = "OPTIONS * RTSP/1.0\r\n" +
                "\r\n" +
                "CSeq: 0\r\n" +
                "\r\n" +
                "Require: org.wfa.wfd1.0\r\n";
        sendMsg(msg);
    }



}
