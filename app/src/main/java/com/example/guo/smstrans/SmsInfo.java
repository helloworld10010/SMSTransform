package com.example.guo.smstrans;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Created by Zhang on 2017/4/26.
 */

class SmsInfo {

    //全部短信
    private static final String SMS_ALL = "content://sms/";
    //正则表达去86
    private static final Pattern Remove86 = Pattern.compile("^((\\+{0,1}86){0,1})");

    //
    private final Context mContext;
    private ContentResolver mContentResolver;
    //Cursor cursor;

    //短信数据库信息
    private int SmsInfo_id;//短信ID


    public String getSmsInfo_address() {
        return SmsInfo_address;
    }

    public String getSmsInfo_body() {
        return SmsInfo_body;
    }

    public String getSmsInfo_data() {
        return SmsInfo_data;
    }


    private String SmsInfo_address;//短信号码
    private String SmsInfo_body;//短信内容
    private String SmsInfo_data;
    private Cursor mCursor;

    SmsInfo(Context mContext) {
        this.mContext = mContext;
        mCursor = null;
    }

    //读取短信
    boolean readSmsCode(int index) {
        // 添加异常捕捉
        try {
            //关闭游标
            if ( mCursor!=null) {
                mCursor.close();
            }

            mContentResolver = mContext.getContentResolver();
            String[] projection = new String[]{"_id", "address", "body", "date"};
            mCursor = mContentResolver.query(Uri.parse(SMS_ALL), projection, null, null, null);

            if (mCursor == null) {
                return false ;
            }

            //移动位置
            mCursor.moveToPosition(index);
            // 如果游标到达末尾
            if (mCursor.isAfterLast()) {
                mCursor.close();
                return false;
            }
            //mCursor.moveToNext();
            SmsInfo_address = mCursor.getString(mCursor.getColumnIndex("address"));

            //判断是否有短信
            if (SmsInfo_address == null || SmsInfo_address.equals("")) {
                //关闭游标
                mCursor.close();
                return false;
            }

           //电话号码去+86
            SmsInfo_address = Remove86.matcher(SmsInfo_address).replaceAll("");

            //短信id
            SmsInfo_id = mCursor.getInt(mCursor.getColumnIndex("_id"));

            //读取短信内容
            SmsInfo_body = mCursor.getString(mCursor.getColumnIndex("body"));
           /* //短信内容处理 460021244932571&3014
            String[] Received_body = SmsInfo_body.split("&");
            String Received_imei = Received_body[0];
            String Received_projectid = Received_body[1].substring(0, 4);
            SmsInfo_body = Received_imei + "&" + Received_projectid;
*/

            //获取短信时间
            long millisecond = mCursor.getLong(mCursor.getColumnIndex("date"));
            //时间格式转换
            Date date = new Date(millisecond);
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
            SmsInfo_data = format.format(date);


        } catch (Exception e) {
            e.printStackTrace();
            if (mCursor != null) {
                mCursor.close();
            }
            return false;
        }
        return true;
    }


    //上传成功删除短信
    boolean DeleteSms() {
        try {
            //假设游标没有关闭
            String where = "_id =" + SmsInfo_id;
            //delete (Uri url, String where, String[] selectionArgs)
            int rows = mContentResolver.delete(Uri.parse(SMS_ALL), where, null);
            Log.i("----", "SmsInfo_id" + SmsInfo_id + "删除短信" + rows + "\n");
            if (rows <= 0) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return true;
    }

}
