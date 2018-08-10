package com.example.guo.smstrans;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.port)
    EditText port;
    @BindView(R.id.ip)
    EditText ip;
    //连接按钮
    @BindView(R.id.btn_connect)
    Button btnConnect;
    //取消连接按钮
    @BindView(R.id.btn_Cancel)
    Button btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        // 打开应用权限
//        if (!SmsWriteOpUtil.isWriteEnabled(getApplicationContext())) {
//            boolean p = SmsWriteOpUtil.setWriteEnabled(
//                    getApplicationContext(), true);
//            Log.i("====", "打开删除短信权限-" + String.valueOf(p));
//        }
    }

    @OnClick({R.id.btn_connect,R.id.btn_Cancel})
    public void onClick(View v) {
        switch (v.getId()) {
            //开始连接
            case R.id.btn_connect:
                Intent intent = new Intent(MainActivity.this,TransforService.class);
                intent.putExtra("ip",ip.getText().toString());
                intent.putExtra("port",Integer.valueOf(port.getText().toString()));
                startService(intent);
                btnConnect.setEnabled(false);
                break;
            //取消连接
            case R.id.btn_Cancel:
                //取消读短信线程
                //taskWork.ClosemStartWork();
                btnConnect.setEnabled(true);
                Intent intent1 = new Intent(this,TransforService.class);
                stopService(intent1);
                break;

            default:
                break;

        }
    }
}
