package com.jiangdg.commontest;

import android.app.Application;

import com.jiangdg.common.crash.CrashManager;
import com.jiangdg.common.utils.GlobalDebug;

/**
 * Created by jianddongguo on 2018/3/8.
 */

public class TestApplication extends Application {
    private CrashManager mCrashManager;

    @Override
    public void onCreate() {
        super.onCreate();

        GlobalDebug.ON = true;

        // crash异常全局监控
        mCrashManager = CrashManager.getInstance(this);
        mCrashManager.initCrashHandler();
    }
}
