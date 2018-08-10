package com.example.guo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;

public class fun {
     static String ProjectID="0001";
//     static SMS sms;
     static String imei;
//     static smsloop loop=null;
     static boolean isrun=false;
     static Context context=null;
//     static socketth socket=null;
     static int HeartCycle=30;
     static int LoopCycle=1;
     static boolean openLog=true;
     static Date receTime=new Date();
     static HashMap<Integer, byte[]> ls = new HashMap<Integer, byte[]>();
     public static String getPhoneIMSI(Context context){
          TelephonyManager mTelephonyMgr = (TelephonyManager)
                  context.getSystemService(Context.TELEPHONY_SERVICE);
          @SuppressLint("MissingPermission") String imsi = mTelephonyMgr.getSubscriberId();
          if(imsi == null) {
               imsi = "";
          }
          return imsi ;
     }
     public static String getPhoneIMEI(Context context) {
          if (context == null) {
               return null;
          }
          try {
               TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
               if (telephonyManager == null) {
                    return null;
               }
               @SuppressLint("MissingPermission") String deviceId = telephonyManager.getDeviceId();
               return deviceId == null ? null : deviceId.trim();
          } catch (SecurityException e) {
               return null;
          } catch (Exception e2) {
               return null;
          }
     }
     public static void Log(String key,String value){
          if(openLog)
               Log.v(key, value);
     }
     public static byte[] int2byte(int res) {
          byte[] targets = new byte[4];
          targets[0] = (byte) (res & 0xff);
          targets[1] = (byte) ((res >> 8) & 0xff);
          targets[2] = (byte) ((res >> 16) & 0xff);
          targets[3] = (byte) (res >>> 24);

          return targets;
     }

     public static int byte2Int(byte[] res) {
          int targets = (res[0] & 0xff) | ((res[1] << 8) & 0xff00) | ((res[2] << 24) >>> 8) | (res[3] << 24);
          return targets;
     }
     public static byte[]  MackPackage(byte[] body,byte[] data){
          if(data!=null) {
               int k = 8 + body.length + data.length;
               byte[] pkage = new byte[k];
               byte[] bodyhead = int2byte(body.length);
               byte[] datahead = int2byte(data.length);
               System.arraycopy(bodyhead, 0, pkage, 0, bodyhead.length);
               System.arraycopy(datahead, 0, pkage, bodyhead.length, datahead.length);
               System.arraycopy(body, 0, pkage, bodyhead.length+datahead.length, body.length);
               System.arraycopy(data, 0, pkage, bodyhead.length+datahead.length+body.length, data.length);
               return pkage;
          }else{
               int k = 8 + body.length;
               byte[] pkage = new byte[k];
               byte[] bodyhead = int2byte(body.length);
               byte[] datahead = int2byte(0);
               System.arraycopy(bodyhead, 0, pkage, 0, bodyhead.length);
               System.arraycopy(datahead, 0, pkage, bodyhead.length, datahead.length);
               System.arraycopy(body, 0, pkage, bodyhead.length+datahead.length, body.length);

               return pkage;
          }
     }
     public static String bytesToHexString(byte[] src){
          StringBuilder stringBuilder = new StringBuilder("");
          if (src == null || src.length <= 0) {
               return null;
          }
          for (int i = 0; i < src.length; i++) {
               int v = src[i] & 0xFF;
               String hv = Integer.toHexString(v);
               if (hv.length() < 2) {
                    stringBuilder.append(0);
               }
               stringBuilder.append(hv);
          }
          return stringBuilder.toString();
     }
     public static byte[] hexStringToBytes(String hexString) {
          if (hexString == null || hexString.equals("")) {
               return null;
          }
          hexString = hexString.toUpperCase();
          int length = hexString.length() / 2;
          char[] hexChars = hexString.toCharArray();
          byte[] d = new byte[length];
          for (int i = 0; i < length; i++) {
               int pos = i * 2;
               d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
          }
          return d;
     }
     private static byte charToByte(char c) {
          return (byte) "0123456789ABCDEF".indexOf(c);
     }
     static String key="131eae23die2eda184487453dert";
}
