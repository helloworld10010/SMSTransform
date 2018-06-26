package com.example.guo.smstrans;

import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    TcpClient mTcpClient = null;
    @BindView(R.id.port)
    EditText port;
    @BindView(R.id.ip)
    EditText ip;
    @BindView(R.id.text_SendCount)
    TextView textSendCount;
    @BindView(R.id.text_State)
    TextView textSocketState;
    //连接按钮
    @BindView(R.id.btn_connect)
    Button btnConnect;
    //取消连接按钮
    @BindView(R.id.btn_Cancel)
    Button btnCancel;
    //清除统计
    @BindView(R.id.btn_CleanSmsCount)
    Button btnCleansmscount;
    @BindView(R.id.iv)
    ImageView mRunning;

    Packet packet;
    volatile int SmsInfoCount = 0;//短信发送计数
    int SocketState;//短信连接状态
    private SMSContentObserver observer;
    private Animation animation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mTcpClient = new TcpClient(MainActivity.this.getApplicationContext(), new ISocketResponse() {
            @Override
            public void onSocketResponse(final String txt) {
                Log.e("===", "接收到的数据：" + txt);
                if (observer != null && !TextUtils.isEmpty(txt)) {
                    observer.deleteSms(txt);
                    SmsInfoCount++;
                    runOnUiThread(() -> textSendCount.setText(String.valueOf(SmsInfoCount)));
                }
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
                            textSocketState.setText("连接关闭");
                            textSocketState.setTextColor(Color.GRAY);
                            // 连接关闭后 马上开启飞行模式，防止短信过来不能转发

                            break;
                        case 300:
                            textSocketState.setText("开始连接..");
                            textSocketState.setTextColor(Color.BLUE);
                            break;
                        case 500:
                            textSocketState.setText("连接失败");
                            textSocketState.setTextColor(Color.RED);
                            break;
                        case 600:
                            textSocketState.setText("等待连接");
                            textSocketState.setTextColor(Color.WHITE);
                            break;
                        case 400:
                            textSocketState.setText("连接成功");
                            textSocketState.setTextColor(Color.GREEN);
                            break;
                        default:
                            break;
                    }
                });
            }

        }, true);

        // 打开应用权限
        if (!SmsWriteOpUtil.isWriteEnabled(getApplicationContext())) {
            boolean p = SmsWriteOpUtil.setWriteEnabled(
                    getApplicationContext(), true);
            Log.i("====", "打开删除短信权限-" + String.valueOf(p));
        }

        packet = new Packet();
        Uri uri = Uri.parse("content://sms");
        observer = new SMSContentObserver(new Handler(message -> {
            Log.e("====", (String) message.obj);
            packet.pack((String) message.obj);
            mTcpClient.send(packet);
            return true;
        }), MainActivity.this, SMSContentObserver.MSG_SMS_WHAT);
        getContentResolver().registerContentObserver(uri, true, observer);

        animation = AnimationUtils.loadAnimation(this, R.anim.rotate);

    }

    @OnClick({R.id.btn_connect,R.id.btn_Cancel,R.id.btn_CleanSmsCount})
    public void onClick(View v) {
        switch (v.getId()) {
            //开始连接
            case R.id.btn_connect:
                mTcpClient.initSocketData(ip.getText().toString(), Integer.valueOf(port.getText().toString()));
                Log.i("----", "ip:" + ip.getText().toString() + "port:" + Integer.valueOf(port.getText().toString()));
                btnConnect.setEnabled(false);
                mRunning.startAnimation(animation);
                break;
            //取消连接
            case R.id.btn_Cancel:
                mTcpClient.close();
                //取消读短信线程
                //taskWork.ClosemStartWork();
                btnConnect.setEnabled(true);
                animation.cancel();
                mRunning.clearAnimation();
                break;
            //清除统计数据
            case R.id.btn_CleanSmsCount:
                textSendCount.setText("0");
                break;

            default:
                break;

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTcpClient.close();
        getContentResolver().unregisterContentObserver(observer);
    }
}
