package com.google.firebase.database.core;

import com.google.firebase.FirebaseApp;

class AndroidPlatform extends JvmPlatform {

  public AndroidPlatform(FirebaseApp firebaseApp) {
    super(firebaseApp);
  }

  @Override
  public EventTarget newEventTarget(final Context ctx) {
    // To guarantee that all calls happen on a single thread, use the current run-loop
    return new EventTarget() {
      @Override
      public void postEvent(Runnable r) {
        // Use a delay to prevent us from stealing time from Main/UI threads
        ctx.runLoop.schedule(r, 500);
      }

      @Override
      public void shutdown() {

      }

      @Override
      public void restart() {

      }
    };
  }
}
