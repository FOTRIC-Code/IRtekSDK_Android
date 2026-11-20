package com.fotric.sdkdemo.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadManager {

    private static volatile ThreadManager instance;

    private final Handler mMainHandler;

    //cpu 个数
    private static final int CUP_COUNT = Runtime.getRuntime().availableProcessors();

    //核心线程数
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CUP_COUNT - 1, 3));

    //最大线程数
    private static final int MAX_POOL_SIZE = CUP_COUNT * 2 + 20;

    //存货最大时长
    private static final int KEEP_ALIVE = 10;

    private final ExecutorService mNetIOExecutor;

    private ThreadManager() {
        mMainHandler = new Handler(Looper.getMainLooper());
        mNetIOExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), new ThreadFactory() {
            private final AtomicInteger poolNumber = new AtomicInteger(1);
            private final SecurityManager securityManager = System.getSecurityManager();
            private final ThreadGroup group = (securityManager != null) ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            private final String namePrefix = "pool-" + poolNumber.getAndIncrement() + "thread-net-fd-io";

            @Override
            public Thread newThread(Runnable runnable) {
                Thread t = new Thread(group, runnable, namePrefix + threadNumber.getAndIncrement(), 0) {
                    @Override
                    public void run() {
                        try {
                            super.run();
                        } catch (Throwable t) {

                        }
                    }
                };
                if (t.isDaemon()) {
                    t.setDaemon(false);
                }
                if (t.getPriority() != Thread.NORM_PRIORITY) {
                    t.setPriority(Thread.NORM_PRIORITY);
                }
                return t;
            }
        }, new ThreadPoolExecutor.DiscardPolicy());
    }

    public static ThreadManager getInstance() {
        if (instance == null) {
            synchronized (ThreadManager.class) {
                if (instance == null) {
                    instance = new ThreadManager();
                }
            }
        }
        return instance;
    }

    public void postNetIO(Runnable r) {
        if (mNetIOExecutor != null) {
            mNetIOExecutor.execute(r);
        }
    }

    public void shutdownNow() {
        if (mNetIOExecutor != null && !mNetIOExecutor.isShutdown()) {
            mNetIOExecutor.shutdownNow();
        }
    }

    public void postToMainHandler(Runnable runnable) {
        if (mMainHandler != null) {
            mMainHandler.post(runnable);
        }
    }

    public void postToMainHandler(Runnable runnable, long delayMillis) {
        if (mMainHandler != null) {
            mMainHandler.postDelayed(runnable, delayMillis);
        }
    }
    public void removeRunnable(Runnable runnable) {
        if (mMainHandler != null) {
            mMainHandler.removeCallbacks(runnable);
        }
    }
}
