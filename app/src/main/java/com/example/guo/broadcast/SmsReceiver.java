package com.example.guo.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SmsReceiver extends BroadcastReceiver {
    public static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    public static final String SMS_DELIVER = "android.provider.Telephony.SMS_DELIVER";
    private static final String TAG = "====";
    public SmsReceiver() {
        Log.i("yjj", "new SmsReceiver");
    }
    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.e(TAG, "on receive--"+intent.getAction());
        Cursor cursor = null;
        try {
            if (SMS_RECEIVED.equals(intent.getAction())||SMS_DELIVER.equals(intent.getAction())) {
                Log.e(TAG, "sms received!");
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    final SmsMessage[] messages = new SmsMessage[pdus.length];
                    for (int i = 0; i < pdus.length; i++) {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    }
                    if (messages.length > 0) {
                        String content = messages[0].getMessageBody();
                        String sender = messages[0].getOriginatingAddress();
                        long msgDate = messages[0].getTimestampMillis();
                        String smsToast = "New SMS received from : "
                                + sender + "\n'"
                                + content + "'";
                        Toast.makeText(context, smsToast, Toast.LENGTH_LONG)
                                .show();
                        Log.e(TAG, "message from: " + sender + ", message body: " + content
                                + ", message date: " + msgDate);
                        //自己的逻辑
                    }
                }
                cursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), new String[] { "_id", "address", "read", "body", "date" }, null, null, "date desc");
                if (null == cursor){
                    return;
                }
                Log.e(TAG,"sms cursor count is "+cursor.getCount());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception : " + e);
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
    }
}