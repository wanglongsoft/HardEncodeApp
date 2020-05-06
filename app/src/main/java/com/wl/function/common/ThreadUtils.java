package com.wl.function.common;

import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadUtils {
    private final String TAG = "ThreadUtils";
    private static volatile ThreadUtils instance = null;

    private ExecutorService executorService = null;
    private ThreadUtils() {
        executorService = Executors.newFixedThreadPool(10);
    }

    public static ThreadUtils getInstance() {
        if(instance == null) {
            synchronized (ThreadUtils.class) {
                if (instance == null) {
                    instance = new ThreadUtils();
                }
            }
        }
        return instance;
    }

    public void addTasks(Runnable runnable) {
        Log.d(TAG, "addTasks: ");
        this.instance.executorService.execute(runnable);
    }

    public void showDownAllThread() {
        Log.d(TAG, "showDownAllThread: ");
        this.instance.executorService.shutdownNow();
    }
}
