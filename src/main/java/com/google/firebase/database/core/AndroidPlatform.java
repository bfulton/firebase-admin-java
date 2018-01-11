package com.google.firebase.database.core;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AndroidThreadManager;
import com.google.firebase.database.logging.LogWrapper;
import com.google.firebase.database.utilities.DefaultRunLoop;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class AndroidPlatform extends JvmPlatform {
  public AndroidPlatform(FirebaseApp firebaseApp) {
    super(firebaseApp);
  }

  @Override
  public EventTarget newEventTarget(Context ctx) {
    return new EventTarget() {
      @Override
      public void postEvent(Runnable r) {
        AndroidThreadManager.instance().execute(r);
      }

      @Override
      public void shutdown() {
      }

      @Override
      public void restart() {
      }
    };
  }

  @Override
  public RunLoop newRunLoop(Context context) {
    final LogWrapper logger = context.getLogger(RunLoop.class);

    final ThreadFactory threadFactory = new ThreadFactory() {
      @Override
      public Thread newThread(final Runnable r) {
        return new Thread() {
          @Override
          public void run() {
            AndroidThreadManager.instance().execute(r);
          }
        };
      }
    };

    final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(threadFactory);

    return new DefaultRunLoop(AndroidThreadManager.instance().getThreadFactory()) {
      @Override
      public void handleException(Throwable e) {
        logger.error(DefaultRunLoop.messageForException(e), e);
      }

      @Override
      public ScheduledExecutorService getExecutorService() {
        return exec;
      }
    };
  }
}
