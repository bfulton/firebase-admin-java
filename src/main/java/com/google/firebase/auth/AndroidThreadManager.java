package com.google.firebase.auth;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ThreadManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class AndroidThreadManager extends ThreadManager implements Executor {
  private static final int DEFAULT_QUEUE_CAPACITY = 20;

  private static final AtomicInteger THREAD_NUM = new AtomicInteger(0);

  private static final String THREAD_PREFIX = "com.google.firebase.auth.AndroidThreadManager";

  private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
      Thread t = new Thread(r);
      t.setPriority(Thread.MIN_PRIORITY);
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

  private final HandlerThread handlerThread;
  private final Handler handler;
  private final HandlerExecutorService executorService;

  private AndroidThreadManager() {
    handlerThread = new HandlerThread("com.google.firebase.auth.AndroidThreadManager",
        Thread.MIN_PRIORITY);
    handlerThread.start();
    Looper looper = handlerThread.getLooper();
    handler = new Handler(looper);
    executorService = new HandlerExecutorService(DEFAULT_QUEUE_CAPACITY);
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
    return executorService;
  }

  @Override
  protected void releaseExecutor(FirebaseApp app, ExecutorService executor) {
    handlerThread.quit();
  }

  @Override
  protected ThreadFactory getThreadFactory() {
    return THREAD_FACTORY;
  }

  @Override
  public void execute(Runnable command) {
    handler.post(command);
  }

  private class HandlerExecutorService extends AbstractExecutorService {
    private final BlockingQueue<Runnable> queue;
    private boolean isShutdown = false;

    HandlerExecutorService(int queueCapacity) {
      queue = new ArrayBlockingQueue<Runnable>(queueCapacity);
    }

    @Override
    public void shutdown() {
      isShutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
      isShutdown = true;
      List<Runnable> unexecuted = new ArrayList<Runnable>();
      queue.drainTo(unexecuted);
      return unexecuted;
    }

    @Override
    public boolean isShutdown() {
      return isShutdown;
    }

    @Override
    public boolean isTerminated() {
      return isShutdown && queue.isEmpty();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      while (!queue.isEmpty()) {
        Thread.sleep(100);
      }
      return isTerminated();
    }

    @Override
    public void execute(Runnable command) {
      handler.post(command);
    }
  }
}
