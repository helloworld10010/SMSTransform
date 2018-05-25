package com.example.guo.smstrans;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

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
    private final Object lock = new Object();

    //0X00 构造初始化
    TcpClient(Context mContext, ISocketResponse socketListener) {
        this.mContext = mContext;
        this.mISocketResponse = socketListener;
    }

    void initSocketData(String host, int port) {
        mIP = host;
        mPort = port;
        initConnect();
    }

    private synchronized void initConnect() {
        close();
        mState = STATE_OPEN;
        mConnectTCPServer = new Thread(new ConnectTCPServer());
        mConnectTCPServer.start();
    }


    private class ConnectTCPServer implements Runnable {
        public void run() {
            Log.i("----", "Tcpclient客户端：开始连接...");
            try {
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
                        Log.i("----", "Tcpclient客户端：连接成功...");
                    } catch (Exception e) {
                        e.printStackTrace();
                        mState = STATE_CONNECT_FAILED;
                        //界面展示
                        mISocketResponse.onSocketState(STATE_CONNECT_FAILED);
                        Log.i("----", "Tcpclient客户端：连接失败...");
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
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.i("----", "Tcpclient客户端：连接End...");
        }
    }

    private class ReceiveMsg implements Runnable {
        @Override
        public void run() {
            Log.i("----", "Tcpclient客户端：接收数据开始...");
            try {
                while (mState != STATE_CLOSE
                        && mState == STATE_CONNECT_SUCCESS
                        && null != inStream) {
                    byte[] bodyBytes = new byte[1];
                    int offset = 0;
                    int length = 1;
                    int read;
                    while ((read = inStream.read(bodyBytes, offset, length)) > 0) {
                        if (length - read == 0) {   // 读取满了 5个字节
                            if (null != mISocketResponse) {
                                response = new String(bodyBytes, "GB2312");
                                Log.i("----", "Tcpclient客户端：接受数据:" + response);
                                mISocketResponse.onSocketResponse(response);
                            }
                            offset = 0;
                            length = 1;
                            continue;
                        }
                        // 读取不够5个字节
                        offset += read;     // offset = 4
                        length = 1 - offset;    // length = 1

                    }
                    initConnect();//走到这一步，说明服务器socket断了
                    break;
                }
            } catch (SocketException e1) {
                Log.i("----", "Tcpclient客户端：socket.close()...");
                //客户端主动socket.close()会调用这里 java.net.SocketException: Socket closed
                e1.printStackTrace();
            } catch (Exception e2) {
                Log.i("----", "Tcpclient客户端：接受数据异常...");
                e2.printStackTrace();
            }

            Log.i("----", "Tcpclient客户端：接受数据结束...");
        }
    }


    private class SendMsg implements Runnable {
        @Override
        public void run() {
            Log.i("----", "Tcpclient客户端：发送数据开始...");
            try {
                while (mState != STATE_CLOSE && mState == STATE_CONNECT_SUCCESS && null != outStream) {
                    Packet item;
                    while (null != (item = requestQueen.take())) {
                        outStream.write(item.getPacket());
                        outStream.flush();
                        item = null;
                    }
                    Log.i("----", "Tcpclient客户端：发送数据woken up...");
//                    synchronized (lock) {
//                        lock.wait();
//                    }
                }
            } catch (SocketException e1) {
                e1.printStackTrace();
                //发送的时候出现异常，说明socket被关闭了(服务器关闭)
                Log.i("----", "Tcpclient客户端：socket.close...");
                //java.net.SocketException: sendto failed: EPIPE (Broken pipe)
                initConnect();
            } catch (Exception e) {
                Log.i("----", "Tcpclient客户端：发送数据异常...");
                e.printStackTrace();
            }
            Log.i("----", "Tcpclient客户端：发送数据结束...");
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

                try {
                    if (null != mSendMsg && mSendMsg.isAlive()) {
                        mSendMsg.interrupt();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mSendMsg = null;
                }

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

}