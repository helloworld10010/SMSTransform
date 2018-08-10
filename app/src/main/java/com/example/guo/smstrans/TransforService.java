package com.example.guo.smstrans;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class TransforService extends Service {

    private TcpClient mTcpClient = null;
    private SMSContentObserver mSmsContentObserver;
    int socketState;//短信连接状态
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){

                        case TcpClient.STATE_CLOSE:
                            updateNotification("连接关闭");
                            break;
                        case TcpClient.STATE_CONNECT_START:
                            updateNotification("开始连接");
                            break;
                        case TcpClient.STATE_CONNECT_FAILED:
                            updateNotification("连接失败");
                            break;
                        case TcpClient.STATE_CONNECT_WAIT:
                            updateNotification("等待连接");
                            break;
                        case TcpClient.STATE_CONNECT_SUCCESS:
                            // 创建 notification
                            createNotifition("连接成功");
                            break;
                        default:
                            break;
            }
        }
    };
    private Packet mPacket;
    private PowerManager mPm;
    private PowerManager.WakeLock mWl;

    private Timer mTimer;
    // 上次服务器回复时间
    private volatile long lastResponseFromServer = System.currentTimeMillis();


    public TransforService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("===","Service onCreate");
        mPm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWl = mPm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myservice");
        mWl.acquire();

        mTcpClient = new TcpClient(this.getApplicationContext(), new ISocketResponse() {
            @Override
            public void onSocketResponse(final String txt) {
                // 更新接收时间
                Log.e("====","上次接收时间为:"+lastResponseFromServer);
                lastResponseFromServer = System.currentTimeMillis();
                Log.e("====","更新接收时间为:"+lastResponseFromServer);

                Log.e("===", "接收到的数据：" + txt);
                if (mSmsContentObserver != null && !TextUtils.isEmpty(txt)) {
                    mSmsContentObserver.deleteSms(txt);
                }
            }

            @Override
            public void onSocketState(int flag) {
                handler.sendEmptyMessage(flag);
            }

        });

        mPacket = new Packet();
        Uri uri = Uri.parse("content://sms");
        mSmsContentObserver = new SMSContentObserver(new Handler(message -> {
            Log.e("====", (String) message.obj);
            mPacket.pack((String) message.obj);
            mTcpClient.send(mPacket);
            return true;
        }), this, SMSContentObserver.MSG_SMS_WHAT);
        getContentResolver().registerContentObserver(uri, true, mSmsContentObserver);

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // 超过十分钟重连
                if((System.currentTimeMillis()-lastResponseFromServer)>1000*60*10) {
                    Log.e("timer", "已经超过10分钟 "+"上次接受时间:" + lastResponseFromServer + "  当前时间:" + System.currentTimeMillis());
                    mTcpClient.initSocketData(Config.HOST,Config.PORT);
                }
            }
            // 轮寻频率5分钟
        },60*1000*1,1000*60*5);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e("====","Service  onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null){
            String ip = intent.getStringExtra("ip");
            int port = intent.getIntExtra("port",0);
            mTcpClient.initSocketData(ip,port);
        } else {
            mTcpClient.initSocketData(Config.HOST,Config.PORT);
        }
        return Service.START_REDELIVER_INTENT;
    }

    private void createNotifition(String state) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 10, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification.Builder builder = new Notification.Builder(this)
        .setContentText("短信转发工作中..")
        .setContentTitle("短信转发监控")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setTicker(state)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(pendingIntent);

        Notification notification = builder.build();

        startForeground(10000, notification);
    }

    private void updateNotification(String state){
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, "hello")
                .setTicker(state)
                .setContentText(state+"..")
                .setContentTitle("短信转发监控")
                .setSmallIcon(R.mipmap.ic_launcher);
        nm.notify(10000,nb.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTcpClient.close();
        getContentResolver().unregisterContentObserver(mSmsContentObserver);
        if (mWl.isHeld()) {
            mWl.release();
        }
        mTimer.purge();
        Log.e("====","Service destroy");
    }
}
