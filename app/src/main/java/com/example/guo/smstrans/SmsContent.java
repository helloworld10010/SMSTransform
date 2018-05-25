package com.example.guo.smstrans;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Monitor sms database
 *
 * @author Jackie
 *
 */
public class SmsContent extends ContentObserver {

    private static final String TAG = SmsContent.class.getSimpleName();
    private static final String MARKER = "YOUR_KEYWORD";
    private Cursor cursor = null;
    private Activity mActivity;

    public SmsContent(Handler handler, Activity activity) {
        super(handler);
        this.mActivity = activity;
    }

    /**
     * This method is called when a content change occurs.
     * <p>
     * Subclasses should override this method to handle content changes.
     * </p>
     *
     * @param selfChange True if this is a self-change notification.
     */
    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        Log.d(TAG, "onChange(boolean selfChange). selfChange=" + selfChange);
        onChange(selfChange, null);
    }

    /**
     * Notice: onChange will be triggered twice on some devices when a sms received,
     * eg: samsung s7 edge(API.23) - twice
     *     samsung note3(API.18) - once
     * 06-15 11:45:48.706 D/SmsContent: onChange(boolean selfChange, Uri uri). selfChange=false, uri=content://sms/raw
     * 06-15 11:45:49.466 D/SmsContent: onChange(boolean selfChange, Uri uri). selfChange=false, uri=content://sms/387
     *
     * Generally onChange will be triggered twice, first time is triggered by uri "content://sms/raw"(sms received,
     * but have not written into inbox), second time is triggered by uri "content://sms/387"(number is sms id)
     *
     * Android official comments:
     * This method is called when a content change occurs.
     * Includes the changed content Uri when available.
     * <p>
     * Subclasses should override this method to handle content changes.
     * To ensure correct operation on older versions of the framework that
     * did not provide a Uri argument, applications should also implement
     * the {@link #onChange(boolean)} overload of this method whenever they
     * implement the {@link #onChange(boolean, Uri)} overload.
     * </p><p>
     * Example implementation:
     * <pre><code>
     * // Implement the onChange(boolean) method to delegate the change notification to
     * // the onChange(boolean, Uri) method to ensure correct operation on older versions
     * // of the framework that did not have the onChange(boolean, Uri) method.
     * {@literal @Override}
     * public void onChange(boolean selfChange) {
     *     onChange(selfChange, null);
     * }
     *
     * // Implement the onChange(boolean, Uri) method to take advantage of the new Uri argument.
     * {@literal @Override}
     * public void onChange(boolean selfChange, Uri uri) {
     *     // Handle change.
     * }
     * </code></pre>
     * </p>
     *
     * @param selfChange True if this is a self-change notification.
     * @param uri The Uri of the changed content, or null if unknown.
     */
    @Override
    public void onChange(boolean selfChange, Uri uri) {
        Log.d(TAG, "onChange(boolean selfChange, Uri uri). selfChange=" + selfChange + ", uri=" + uri);

        /**
         * 适配某些较旧的设备，可能只会触发onChange(boolean selfChange)方法，没有传回uri参数，
         * 此时只能通过"content://sms/inbox"来查询短信
         */
        if (uri == null) {
            uri = Uri.parse("content://sms/inbox");
        }
        /**
         * 06-15 11:45:48.706 D/SmsContent: onChange(boolean selfChange, Uri uri). selfChange=false, uri=content://sms/raw
         * 06-15 11:45:49.466 D/SmsContent: onChange(boolean selfChange, Uri uri). selfChange=false, uri=content://sms/387
         *
         * Generally onChange will be triggered twice, first time is triggered by uri "content://sms/raw"(sms received,
         * but have not written into inbox), second time is triggered by uri "content://sms/387"(number is sms id)
         */
        if (uri.toString().equals("content://sms/raw")) {
            return;
        }
        cursor = this.mActivity.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex("_id"));
                String body = cursor.getString(cursor.getColumnIndex("body"));
                Log.d(TAG, "sms id: " + id + "\nsms body: " + body);
                cursor.close();

                // Already got sms body, do anything you want, for example: filter the verify code
                getVerifyCode(body);
            }
        }
        else {
            Log.e(TAG, "error: cursor == null");
        }
    }

    /**
     * Register a monitor of changing of sms
     */
    public void register() {
        Log.d(TAG, "Register sms monitor");
        this.mActivity.getContentResolver().registerContentObserver(
                Uri.parse("content://sms/"), true, this);
    }

    /**
     * Unregister the monitor of changing of sms
     */
    public void unRegister() {
        Log.d(TAG, "Unregister sms monitor");
        this.mActivity.getContentResolver().unregisterContentObserver(this);
    }

    /**
     * Get verify code from sms body
     * @param str
     * @return
     */
    public String getVerifyCode(String str) {
        String verifyCode = null;
        if (smsContentFilter(str)) {
            Log.d(TAG, "sms content matched, auto-fill verify code.");
            verifyCode = getDynamicPassword(str);
        }
        else {
            // Do nothing
            Log.d(TAG, "sms content did not match, do nothing.");
        }
        return verifyCode;
    }

    /**
     * Check if str is verification-code-formatted
     *
     * @param str
     * @return
     */
    private boolean smsContentFilter(String str) {
        Log.d(TAG, "smsContentFilter. smsBody = " + str);
        boolean isMatched = false;
        if (!TextUtils.isEmpty(str)) {
            // Check if str contains keyword
            if (str.contains(MARKER)) {
                Log.d(TAG, "This sms contains \"" + MARKER + "\"");
                // Check if str contains continuous 6 numbers
                Pattern  continuousNumberPattern = Pattern.compile("[0-9\\.]+");
                Matcher m = continuousNumberPattern.matcher(str);
                while(m.find()){
                    if(m.group().length() == 6) {
                        Log.d(TAG, "This sms contains continuous 6 numbers : " + m.group());
                        isMatched = true;
                    }
                }
            }
        }
        return isMatched;
    }

    /**
     * Cut the continuous 6 numbers from str
     *
     * @param str sms content
     * @return verification code
     */
    private String getDynamicPassword(String str) {
        Log.d(TAG, "getDynamicPassword. smsBody = " + str);
        Pattern  continuousNumberPattern = Pattern.compile("[0-9\\.]+");
        Matcher m = continuousNumberPattern.matcher(str);
        String dynamicPassword = "";
        while(m.find()){
            if(m.group().length() == 6) {
                Log.d(TAG, m.group());
                dynamicPassword = m.group();
            }
        }

        Log.d(TAG, "Verification code: " + dynamicPassword);
        return dynamicPassword;
    }
}