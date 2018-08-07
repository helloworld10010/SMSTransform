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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    public static final int STATE_OPEN = 100;//socket打开
    public static final int STATE_CLOSE = 200;//socket关闭
    public static final int STATE_CONNECT_START = 300;//开始连接server
    public static final int STATE_CONNECT_SUCCESS = 400;//连接成功
    public static final int STATE_CONNECT_FAILED = 500;//连接失败
    public static final int STATE_CONNECT_WAIT = 600;//等待连接


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
    private Executor executor = Executors.newFixedThreadPool(4);

    public TcpClient(Context mContext, ISocketResponse socketListener) {
        this.mContext = mContext;
        this.mISocketResponse = socketListener;
    }

    public void initSocketData(String host, int port) {
        mIP = host;
        mPort = port;
        initConnect();
    }

    private synchronized void initConnect() {
        // 重新连接前关闭之前的线程，所以有interrupted发生
        Log.e("====","initConnection/////////////////////////////////////////////////////////////////////////////");
        SystemClock.sleep(500);
        close();

        mState = STATE_OPEN;
        mConnectTCPServer = new Thread(new ConnectTCPServer());
        mConnectTCPServer.start();
    }


    private class ConnectTCPServer implements Runnable {
        public void run() {
            Log.e("====", "Tcpclient客户端：开始连接...");
            while (mState != STATE_CLOSE) {
                try {
                    mState = STATE_CONNECT_START;
                    mClientSocket = new Socket();
                    //10分钟连接不上超时
                    int timeout = 600 * 1000;
                    mClientSocket.connect(new InetSocketAddress(mIP, mPort), timeout);
                    if(mClientSocket.isConnected()){
                        mState = STATE_CONNECT_SUCCESS;
                        //界面展示
                        mISocketResponse.onSocketState(STATE_CONNECT_SUCCESS);
                        Log.e("====", "Tcpclient客户端：连接成功...");
                    } else {
                        throw new Exception("连接失败,重新连接..");
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

                    executor.execute(new ReceiveMsg());
                    executor.execute(new SendMsg());
                    break;
                } else {
                    mState = STATE_CONNECT_WAIT;
                    //界面展示
                    mISocketResponse.onSocketState(STATE_CONNECT_WAIT);
                    //如果有网络没有连接上，则定时取连接，没有网络则直接退出
                    if (NetworkUtil.isNetworkAvailable(mContext)) {
                        Log.e("====","网络状态正常");
                        try {
                            Thread.sleep(15 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    } else {
                        Log.e("====","无网络，准备结束重连");
                        break;
                    }
                }
            }

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
                }
            } catch (SocketException e1) {
                e1.printStackTrace();
                //发送的时候出现异常，说明socket被关闭了(服务器关闭)
                Log.e("====", "Tcpclient客户端SendMsg：socket.close...");
                mISocketResponse.onSocketState(STATE_CLOSE);
                initConnect();

            } catch (Exception e) {
                Log.e("====", "Tcpclient客户端：发送数据异常... ");
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
                        // 在阻塞状态中调用interrupt抛出interruptexception
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
    public int getmState() {
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
}