package com.example.guo.smstrans;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.os.Environment.MEDIA_MOUNTED;

/**
 * Created by Zhang on 2017/5/4.
 */

//短信统计
class SmsSendStatistics {
    private Context mContext;
    private File SmsCountFile;

    //构造
    SmsSendStatistics(Context context) {
        mContext = context;
        //方法用于获取/data/data//cache目录
        //内部存储
        String filePath = getCacheDir("SmsCount").getAbsolutePath();
        SmsCountFile=new File(filePath,"SmsCount.txt");
    }

    @Nullable
    public File getCacheDir(String subDirName) {
        File dir;
        if (Environment.getExternalStorageState().equals(MEDIA_MOUNTED)) {
            dir = mContext.getExternalCacheDir();
        } else {
            dir = mContext.getCacheDir();
        }
        if (dir == null) {
            return null;
        }

        if (subDirName != null) {
            File subDir = new File(dir.getAbsolutePath() + "/" + subDirName);
            if (createDir(subDir)) {
                dir = subDir;
            } else {
                dir = null;
            }
        }
        return dir;
    }

    /**
     * 创建指定文件夹
     *
     * @param dir 指定文件夹
     * @return 是否创建成功
     */
    public  boolean createDir(File dir) {
        if (dir == null) {
            return false;
        }

        if (dir.exists() && dir.isDirectory()) {
            return true;
        } else {
            return dir.mkdirs();
        }
    }

    /**
     * 向指定文件写入字符串
     * @param text 要写入的字符串
     * @return 是否写入成功
     */
    public  boolean writeText(String text) {
        FileOutputStream outputStream = null;

        try {
            if (SmsCountFile.exists()) {
                SmsCountFile.delete();
            }

            outputStream = new FileOutputStream(SmsCountFile.getAbsolutePath(), true);
            outputStream.write(text.getBytes());
            outputStream.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * 从文件中读取字符串
     * @return 读取的字符串
     */
    public String readText() {
        if (! SmsCountFile.exists()) {
            return null;
        }

        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream(SmsCountFile.getAbsolutePath());

            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
            StringBuilder stringBuilder = new StringBuilder();

            while ((receiveString = bufferedReader.readLine()) != null) {
                stringBuilder.append(receiveString);
            }

            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

}
