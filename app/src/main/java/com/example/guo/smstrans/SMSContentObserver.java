package com.example.guo.smstrans;


import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class SMSContentObserver extends ContentObserver {
    private Handler mHandler;
    private Context mContext;
    /**观察类型：所有内容或仅收件箱*/
    private int observerType;
    private static final Pattern Remove86 = Pattern.compile("^((\\+{0,1}86){0,1})");
    /**观察所有内容*/
    public static final int MSG_SMS_WHAT = 1;
    /**仅观察收件箱*/
    public static final int MSG_SMS_INBOX_WHAT = 2;
    private volatile int msgId;

    public SMSContentObserver(Handler handler, Context context, int observerType) {
        super(handler);
        this.mHandler = handler;
        this.mContext = context;
        this.observerType = observerType;
    }
    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        Log.e("====","进入onChange了！");
        onChange(selfChange,null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        //Log.e("====","本次uri是"+uri.toString());

        if(uri == null){
            uri = Uri.parse("content://sms");
        }

        if (uri.toString().equals("content://sms/raw") || uri.toString().equals("content://sms/")) {
            //Log.e("====","无用，跳过～");
            return;
        }

        if (observerType == MSG_SMS_WHAT) {
            Cursor cursor = mContext.getContentResolver().query(uri, new String[] { "_id", "address", "body", "type", "date" }, "read=0", null, "date desc");
            if (cursor != null && cursor.getCount()>0) {
                if (cursor.moveToFirst()) { //最后收到的短信在第一条. This method will return false if the cursor is empty
                    msgId = cursor.getInt(cursor.getColumnIndex("_id"));
                    String msgAddr = cursor.getString(cursor.getColumnIndex("address"));
                    msgAddr = Remove86.matcher(msgAddr).replaceAll("");
                    String msgBody = cursor.getString(cursor.getColumnIndex("body"));
                    String msgType = cursor.getString(cursor.getColumnIndex("type"));
                    String msgDate = cursor.getString(cursor.getColumnIndex("date"));
                    String date = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(new Date(Long.parseLong(msgDate)));
//                    String msgObj = "收件箱\nId：" + msgId + "\n号码：" + msgAddr + "\n内容：" + msgBody + "\n类型：" + msgType + "\n时间：" + date + "\n";
                    String msgObj = "address:" + msgAddr + "@body:" + msgBody + "@date:" + date + "@id"+msgId+"@";
                    mHandler.sendMessage(Message.obtain(mHandler, MSG_SMS_WHAT, msgObj));

                }
                cursor.close();
            }else{
                Log.e("====","MSG_SMS_WHAT cursor = null && cursor.getCount()<0");
            }
        } else if (observerType == MSG_SMS_INBOX_WHAT) {
            Cursor cursor = mContext.getContentResolver().query(uri, null, "read=?", new String[]{"0"}, "date desc");//Passing null will return all columns, which is inefficient.
            //等价于附加条件 if (cursor.getInt(cursor.getColumnIndex("read")) == 0) //表示短信未读。这种方式不靠谱啊，建议用上面的方式！
            if (cursor != null && cursor.getCount()>0) {
                StringBuilder sb = new StringBuilder("未读短信\n");
                if (cursor.moveToFirst()) {
                    String sendNumber = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    String msgDate = cursor.getString(cursor.getColumnIndex("date"));

                    sb.append("号码：" + sendNumber + "\n内容：" + body + "\n时间：" + msgDate +"\n");
                }
                mHandler.obtainMessage(MSG_SMS_INBOX_WHAT, sb.toString()).sendToTarget();
                cursor.close();
            }else{
                Log.e("====","MSG_SMS_INBOX_WHAT cursor = null && cursor.getCount()<0");
            }
        }
    }

    public boolean deleteSms(int smsId) {
        try {
            //假设游标没有关闭
            //delete (Uri url, String where, String[] selectionArgs)
            int rows = mContext.getContentResolver().delete(Uri.parse("content://sms/"),"_id=?", new String[]{String.valueOf(smsId)});
            if (rows <= 0) {
                Log.e("====", " sms id=" + msgId + "删除失败-影响行数" + rows + "\n");
                return false;
            } else{
                Log.e("====", " sms id-" + msgId + " 删除成功-影响行数" + rows + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}