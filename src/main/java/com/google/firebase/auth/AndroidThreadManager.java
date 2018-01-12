package com.google.firebase.auth;

import com.google.firebase.FirebaseApp;
import com.google.firebase.ThreadManager;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class AndroidThreadManager extends ThreadManager implements Executor {

  private static final AtomicInteger THREAD_NUM = new AtomicInteger(0);

  private static final String THREAD_PREFIX = "Firebase.AndroidThreadManager";

  private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
      String name = THREAD_PREFIX + "[" + THREAD_NUM.incrementAndGet() + "]";
      Thread t = new Thread(r, name);
      t.setPriority(Thread.MIN_PRIORITY);
      return t;
    }
  };

  private final ExecutorService executorService;

  private AndroidThreadManager() {
    executorService = Executors.newSingleThreadExecutor(THREAD_FACTORY);
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
    executorService.shutdown();
  }

  @Override
  protected ThreadFactory getThreadFactory() {
    return THREAD_FACTORY;
  }

  @Override
  public void execute(Runnable command) {
    executorService.execute(command);
  }
}
