package com.example.guo.smstrans;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 异常总结：
 * client端关闭socket后：
 * 发送线程interruptedException
 * 接收线程SocketException
 * server端关闭socket后：
 * 接收线程阻塞打开，直接重连
 * 重连前调用close()会interrupted两个线程
 */

public class TcpClient {
    //连接状态
    public final int STATE_OPEN = 100;//socket打开
    public final int STATE_CLOSE = 200;//socket关闭
    public final int STATE_CONNECT_START = 300;//开始连接server
    public final int STATE_CONNECT_SUCCESS = 400;//连接成功
    public final int STATE_CONNECT_FAILED = 500;//连接失败
    public final int STATE_CONNECT_WAIT = 600;//等待连接


    private int mState = STATE_CONNECT_START;
    private String response;
    private Thread mConnectTCPServer = null;
    private Thread mReceiveMsg = null;
    private Thread mSendMsg = null;


    private Socket mClientSocket = null;
    private String mIP;
    private int mPort;

    private OutputStream outStream = null;
    private InputStream inStream = null;

    private Context mContext;
    private ISocketResponse mISocketResponse;
    private LinkedBlockingQueue<Packet> requestQueen = new LinkedBlockingQueue<>();
    private Context ctx;

    //0X00 构造初始化
    TcpClient(Context mContext, ISocketResponse socketListener,boolean first) {
        this.mContext = mContext;
        this.mISocketResponse = socketListener;
        ctx = CustomApplication.getContextObject();
    }

    void initSocketData(String host, int port) {
        mIP = host;
        mPort = port;
        initConnect();
    }

    private synchronized void initConnect() {
        // 重新连接前关闭之前的线程，所以有interrupted发生
        close();
        // 连接关闭时马上开启飞行模式，防止短信到来无法转发

        mState = STATE_OPEN;
        mConnectTCPServer = new Thread(new ConnectTCPServer());
        mConnectTCPServer.start();
    }


    private class ConnectTCPServer implements Runnable {
        public void run() {
            Log.e("====", "Tcpclient客户端：开始连接...");
//            try {
            while (mState != STATE_CLOSE) {
                try {
                    mState = STATE_CONNECT_START;
                    mClientSocket = new Socket();

                    //10分钟连接不上超时
                    int timeout = 600 * 1000;
                    mClientSocket.connect(new InetSocketAddress(mIP, mPort), timeout);
                    mState = STATE_CONNECT_SUCCESS;
                    //界面展示
                    mISocketResponse.onSocketState(STATE_CONNECT_SUCCESS);
                    Log.e("====", "Tcpclient客户端：连接成功...");

                    // 连接成功后关闭飞行模式
                    if(isAirplaneModeOn(ctx)){
                        setAirplaneModeOn(ctx,false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mState = STATE_CONNECT_FAILED;
                    //界面展示
                    mISocketResponse.onSocketState(STATE_CONNECT_FAILED);
                    Log.e("====", "Tcpclient客户端：连接失败...");
                }

                if (mState == STATE_CONNECT_SUCCESS) {
                    try {
                        outStream = mClientSocket.getOutputStream();
                        inStream = mClientSocket.getInputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mReceiveMsg = new Thread(new ReceiveMsg());
                    mReceiveMsg.start();
                    mSendMsg = new Thread(new SendMsg());
                    mSendMsg.start();
                    break;
                } else {
                    mState = STATE_CONNECT_WAIT;
                    //界面展示
                    mISocketResponse.onSocketState(STATE_CONNECT_WAIT);
                    //如果有网络没有连接上，则定时取连接，没有网络则直接退出
                    if (NetworkUtil.isNetworkAvailable(mContext)) {
                        try {
                            SystemClock.sleep(1000);//程序睡眠1秒钟
                            Thread.sleep(15 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
//            } catch (Exception e) {
//                e.printStackTrace();
//                Log.i("----","ConnectTCPServer最外边的catch");
//            }

            Log.e("====", "Tcpclient客户端：连接End...");
        }
    }

    private class ReceiveMsg implements Runnable {
        @Override
        public void run() {
            Log.e("====", "Tcpclient客户端：接收数据开始...");
            try {
                while (mState != STATE_CLOSE
                        && mState == STATE_CONNECT_SUCCESS
                        && null != inStream) {
//                    byte[] bodyBytes = new byte[8];
//                    int offset = 0;
//                    int length = 1;
//                    int read;
//                    while ((read = inStream.read(bodyBytes, offset, length)) > 0) {
//                        if (length - read == 0) {   // 读取满了 5个字节
//                            if (null != mISocketResponse) {
//                                response = new String(bodyBytes, "GB2312");
//                                Log.e("====", "Tcpclient客户端：接受数据:" + response);
//                                mISocketResponse.onSocketResponse(response);
//                            }
//                            offset = 0;
//                            length = 1;
//                            continue;
//                        }
//                        // 读取不够5个字节
//                        offset += read;     // offset = 4
//                        length = 1 - offset;    // length = 1
                    BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (null != mISocketResponse) {
                            Log.e("====", "Tcpclient客户端：接受数据:" + line);
                            mISocketResponse.onSocketResponse(line);
                        }

                    }
                    // 服务端断开后走这里重新连接
                    initConnect();//走到这一步，说明服务器socket断了
                    break;
                }
            } catch (SocketException e1) {
                //客户端主动socket.close()会调用这里 java.net.SocketException: Socket closed
                Log.e("====", "Tcpclient客户端ReceiveMsg：socket.close()...");
                e1.printStackTrace();

            } catch (Exception e2) {
                Log.e("====", "Tcpclient客户端：接受数据异常...");
                e2.printStackTrace();
            }

            Log.e("====", "Tcpclient客户端：接受数据结束...");
        }
    }


    private class SendMsg implements Runnable {
        @Override
        public void run() {
            Log.e("====", "Tcpclient客户端：发送数据开始...");
            try {
                while (mState != STATE_CLOSE && mState == STATE_CONNECT_SUCCESS && null != outStream) {
                    Packet item;
                    while (null != (item = requestQueen.take())) {
                        outStream.write(item.getPacket());
                        outStream.flush();
                        item = null;
                    }
                    Log.e("====", "Tcpclient客户端：发送数据woken up...");
//                    synchronized (lock) {
//                        lock.wait();
//                    }
                }
            } catch (SocketException e1) {
                e1.printStackTrace();
                //发送的时候出现异常，说明socket被关闭了(服务器关闭)
                Log.e("====", "Tcpclient客户端SendMsg：socket.close...");
                mISocketResponse.onSocketState(STATE_CLOSE);
                initConnect();

            } catch (Exception e) {
                Log.e("====", "Tcpclient客户端：发送数据异常... 改变第一次标记");
                // interruptedException
                e.printStackTrace();
            }
            Log.e("====", "Tcpclient客户端：发送数据结束...");
        }
    }

    public synchronized void close() {
        try {
            if (mState != STATE_CLOSE) {
                try {
                    if (null != mClientSocket) {
                        mClientSocket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mClientSocket = null;
                }

                try {
                    if (null != outStream) {
                        outStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    outStream = null;
                }

                try {
                    if (null != inStream) {
                        inStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    inStream = null;
                }

                try {
                    if (null != mConnectTCPServer && mConnectTCPServer.isAlive()) {
                        //停止该线程
                        mConnectTCPServer.interrupt();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mConnectTCPServer = null;
                }
                // 停止发送/接收线程
                try {
                    if (null != mSendMsg && mSendMsg.isAlive()) {
                        mSendMsg.interrupt();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mSendMsg = null;
                }

                // 停止线程
                try {
                    if (null != mReceiveMsg && mReceiveMsg.isAlive()) {
                        mReceiveMsg.interrupt();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mReceiveMsg = null;
                }

                mState = STATE_CLOSE;
                //界面展示
                mISocketResponse.onSocketState(mState);
            }
            requestQueen.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //------------------------------------------------------------------------------------------------
    //获取连接状态
    int getmState() {
        //界面展示
        mISocketResponse.onSocketState(mState);
        return mState;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }


    //发送数据
    public int send(Packet in) {
        try {
            requestQueen.put(in);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        synchronized (lock) {
//            lock.notifyAll();
//        }
        return in.getId();
    }

    //取消
    public void cancel(int reqId) {
        Iterator<Packet> mIterator = requestQueen.iterator();
        while (mIterator.hasNext()) {
            Packet packet = mIterator.next();
            if (packet.getId() == reqId) {
                mIterator.remove();
            }
        }
    }


    public boolean isAirplaneModeOn(Context ctx) {
        //4.2以下
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.System.getInt(ctx.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else { //4.2或4.2以上
            return Settings.Global.getInt(ctx.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

    public void setAirplaneModeOn(Context ctx,boolean isEnable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Settings.System.putInt(ctx.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, isEnable ? 1 : 0);
        } else { //4.2或4.2以上
            Settings.Global.putInt(ctx.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, isEnable ? 1 : 0);
        }
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", isEnable);
        ctx.sendBroadcast(intent);
    }

    public void setWifiEnable(Context context, boolean state){
        //首先，用Context通过getSystemService获取wifimanager
        WifiManager mWifiManager = (WifiManager)
                context.getSystemService(Context.WIFI_SERVICE);
        //调用WifiManager的setWifiEnabled方法设置wifi的打开或者关闭，只需把下面的state改为布尔值即可（true:打开 false:关闭）
        mWifiManager.setWifiEnabled(state);
    }


}