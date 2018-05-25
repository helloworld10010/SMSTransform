package com.example.guo.smstrans;

import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TcpClient mTcpClient = null;
    private EditText ip, port;
    private TextView text_SendCount;
    private TextView text_SocketState;

    //连接按钮
    Button btn_connect;
    //取消连接按钮
    Button btn_Cancel;
    //清除统计
    Button btn_CleanSmsCount;
    //退出按钮
    Button btn_exit;
    Packet packet;

    volatile int SmsInfoCount = 0;//短信发送计数
    int SocketState;//短信连接状态
    private SMSContentObserver observer;
    private Animation animation;
    private ImageView running;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        mTcpClient = new TcpClient(MainActivity.this.getApplicationContext(), socketListener);
        // 打开应用权限
        if (!SmsWriteOpUtil.isWriteEnabled(getApplicationContext())) {
            SmsWriteOpUtil.setWriteEnabled(
                    getApplicationContext(), true);
        }

        packet = new Packet();
        Uri uri= Uri.parse("content://sms");
        observer = new SMSContentObserver(new Handler(message -> {
            Log.e("====",(String)message.obj);
            packet.pack((String)message.obj);
            mTcpClient.send(packet);
            return true;
        }),MainActivity.this,SMSContentObserver.MSG_SMS_WHAT);
        getContentResolver().registerContentObserver(uri,true, observer);

        animation = AnimationUtils.loadAnimation(this, R.anim.rotate);

    }

    private void initView() {

        ip = findViewById(R.id.ip);
        port = findViewById(R.id.port);
        text_SendCount = findViewById(R.id.text_SendCount);
        text_SocketState = findViewById(R.id.text_State);

        //连接按钮
        btn_connect= findViewById(R.id.btn_connect);
        btn_connect.setOnClickListener(listener);



        //取消连接按钮
        btn_Cancel= findViewById(R.id.btn_Cancel);
        btn_Cancel.setOnClickListener(listener);

        //清除统计
        btn_CleanSmsCount= findViewById(R.id.btn_CleanSmsCount);
        btn_CleanSmsCount.setOnClickListener(listener);

        running = findViewById(R.id.iv);

    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                //开始连接
                case R.id.btn_connect:
                    mTcpClient.initSocketData(ip.getText().toString(), Integer.valueOf(port.getText().toString()));
                    Log.i("----","ip:"+ip.getText().toString()+"port:"+Integer.valueOf(port.getText().toString()));
                    btn_connect.setEnabled(false);
                    running.startAnimation(animation);

                    break;


                //取消连接
                case R.id.btn_Cancel:
                    mTcpClient.close();
                    //取消读短信线程
                    //taskWork.ClosemStartWork();

                    btn_connect.setEnabled(true);
                    animation.cancel();
                    running.clearAnimation();

                    break;
                //清除统计数据
                case R.id.btn_CleanSmsCount:
                    //if (mSmsSendStatistics.writeText("0")){
                        text_SendCount.setText("0");
                    //}
                    break;

                default:
                    break;

            }
        }
    };

    //回调
    private ISocketResponse socketListener = new ISocketResponse() {
        @Override
        public void onSocketResponse(final String txt) {
            if(observer!=null){
                observer.deleteSms(Integer.valueOf(txt));
            }
            SmsInfoCount++;
            runOnUiThread(() -> text_SendCount.setText(String.valueOf(SmsInfoCount)));
        }

        @Override
        public void onSocketState(final int Flags) {
            if (SocketState == Flags) {
                Log.i("----", "SocketState退出");
                return;
            } else {
                SocketState = Flags;
            }
            runOnUiThread(() -> {
                switch (SocketState) {
                    case 200:
                        text_SocketState.setText("连接关闭");
                        text_SocketState.setTextColor(Color.GRAY);
                        break;
                    case 300:
                        text_SocketState.setText("开始连接..");
                        text_SocketState.setTextColor(Color.BLUE);
                        break;
                    case 400:
                        text_SocketState.setText("连接成功");
                        text_SocketState.setTextColor(Color.GREEN);
                        break;
                    case 500:
                        text_SocketState.setText("连接失败");
                        text_SocketState.setTextColor(Color.RED);
                        break;
                    case 600:
                        text_SocketState.setText("等待连接");
                        text_SocketState.setTextColor(Color.WHITE);
                        break;
                    default:
                        break;
                }
            });
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTcpClient.close();
        getContentResolver().unregisterContentObserver(observer);
    }
}
