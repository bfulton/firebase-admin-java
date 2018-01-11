package com.google.firebase.auth;

import android.os.Process;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ThreadManager;
import com.google.firebase.database.core.AndroidPlatform;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class AndroidThreadManager extends ThreadManager implements Executor {
  private static final AtomicInteger THREAD_NUM = new AtomicInteger(0);

  private static final String THREAD_PREFIX = "com.google.firebase.auth.AndroidThreadManager";

  private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
    @Override
    public Thread newThread(final Runnable r) {
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
          r.run();
        }
      });
      setName(t);
      return t;
    }

    private void setName(Thread t) {
      String nameSuffix = t.getName();
      if (nameSuffix == null) {
        nameSuffix = "";
      } else {
        nameSuffix = ":" + nameSuffix;
      }
      t.setName(THREAD_PREFIX + "[" + THREAD_NUM.incrementAndGet() + "]" + nameSuffix);

    }
  };

  private AndroidThreadManager() {
  }

  private static AndroidThreadManager instance = null;

  public static synchronized AndroidThreadManager instance() {
    if (instance == null) {
      instance = new AndroidThreadManager();
    }
    return instance;
  }

  @Override
  protected ExecutorService getExecutor(FirebaseApp app) {
    return AndroidPlatform.ANDROID_EXEC_SVC;
  }

  @Override
  protected void releaseExecutor(FirebaseApp app, ExecutorService executor) {
  }

  @Override
  public ThreadFactory getThreadFactory() {
    return THREAD_FACTORY;
  }

  @Override
  public void execute(Runnable command) {
    AndroidPlatform.ANDROID_EXEC_SVC.execute(command);
  }
}
