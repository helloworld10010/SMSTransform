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
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

public class TransforService extends Service {

    TcpClient mTcpClient = null;
    private SMSContentObserver observer;
    int socketState;//短信连接状态
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Log.e("====","服务中的handler message");
            switch (msg.what){

                        case 200:
                            updateNotification("连接关闭");
                            break;
                        case 300:
                            updateNotification("开始连接");
                            break;
                        case 500:
                            updateNotification("连接失败");
                            break;
                        case 600:
                            updateNotification("等待连接");
                            break;
                        case 400:
                            // 创建 notification
                            createNotifition("连接成功");
                            break;
                        default:
                            break;
            }
        }
    };
    private Packet mPacket;


    public TransforService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("===","Service onCreate");


        mTcpClient = new TcpClient(this.getApplicationContext(), new ISocketResponse() {
            @Override
            public void onSocketResponse(final String txt) {
                Log.e("===", "接收到的数据：" + txt);
                if (observer != null && !TextUtils.isEmpty(txt)) {
                    observer.deleteSms(txt);
                }
            }

            @Override
            public void onSocketState(final int Flags) {
                if (socketState == Flags) {
                    Log.i("----", "SocketState退出");
                    return;
                } else {
                    socketState = Flags;
                }
                handler.sendEmptyMessage(socketState);
            }

        }, true);

        mPacket = new Packet();
        Uri uri = Uri.parse("content://sms");
        observer = new SMSContentObserver(new Handler(message -> {
            Log.e("====", (String) message.obj);
            mPacket.pack((String) message.obj);
            mTcpClient.send(mPacket);
            return true;
        }), this, SMSContentObserver.MSG_SMS_WHAT);
        getContentResolver().registerContentObserver(uri, true, observer);
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
            Log.e("====","ip---"+ip+"  port----"+port);
            mTcpClient.initSocketData(ip,port);
        }
        return Service.START_REDELIVER_INTENT;
    }

    private void createNotifition(String state) {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentText("短信转发工作中..");
        builder.setContentTitle("短信转发监控");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setTicker(state);
        builder.setWhen(System.currentTimeMillis());
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 10, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(pendingIntent);
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
        getContentResolver().unregisterContentObserver(observer);
        Log.e("====","Service destroy");
    }
}
