package com.jiangdg.common.crash;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.widget.Toast;

import com.jiangdg.common.utils.AppManagerUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 异常捕获处理
 * 将异常信息保存到本地，并上传到服务器
 * Created by jianddongguo on 2018/2/23.
 */

public class CrashManager implements Thread.UncaughtExceptionHandler {
    private static final String ROOTPATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static CrashManager mCrashHandler;
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private Context mCtx;
    private String mLogPath;
    private String mURLPath;

    private CrashManager(Context ctx) {
        this.mCtx = ctx;
    }

    public static CrashManager getInstance(Context ctx) {
        if (mCrashHandler == null) {
            mCrashHandler = new CrashManager(ctx);
        }
        return mCrashHandler;
    }

    public void initCrashHandler() {
        String appName = AppManagerUtil.getAppName(mCtx);
        File pathFile = new File(ROOTPATH+File.separator+appName);
        if(! pathFile.exists()) {
            pathFile.mkdirs();
        }
        initCrashHandler(pathFile+"/logs.txt",null);
    }

    public void initCrashHandler(String logPath,String urlPath) {
        this.mLogPath = logPath;
        this.mURLPath = urlPath;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (!handleException(e) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(t, e);
        } else {
            try {
                Thread.sleep(2 * 1000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            AppManagerUtil.removeAllActivity();
            AppManagerUtil.restartApp(mCtx);
            AppManagerUtil.releaseAppResource();
        }
    }

    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mCtx, "程序异常终止，重启中...", Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }).start();

        uploadErrorInfo(saveErrorInfo(ex));

        return true;
    }

    private void uploadErrorInfo(File file) {
        if (file == null || !file.exists())
            return;
        // 将文件序列化

        // 长传到服务器
    }

    private File saveErrorInfo(Throwable ex) {
        File logFile = new File(mLogPath);
        FileWriter fw = null;
        PrintWriter pw = null;
        try {
            fw = new FileWriter(logFile, true);
            pw = new PrintWriter(fw);
            PackageManager pm = mCtx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(mCtx.getPackageName(), PackageManager.GET_ACTIVITIES);
            pw.println();
            pw.println("Time：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            pw.println("VersionInfo：versionCode=" + pi.versionCode + " versionName：" + pi.versionName);
            // 通过反射获取手机参数
            pw.println("PhoneInfo  ：manufacture=" + Build.MANUFACTURER + " model=" + Build.MODEL);
            pw.println("SystemInfo ：version=" + Build.VERSION.RELEASE);
            // 打印堆栈信息
            ex.printStackTrace(pw);
            pw.println("End=====================================");
            pw.flush();
            fw.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return logFile;
    }
}
