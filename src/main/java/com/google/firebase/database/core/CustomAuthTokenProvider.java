package com.google.firebase.database.core;

import com.google.auth.oauth2.AccessToken;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.CustomAuthUtils;

import java.util.concurrent.Executor;

public class CustomAuthTokenProvider extends JvmAuthTokenProvider {
  private static final int REFRESH_GRACE_PERIOD_MILLIS = 60 * 5 * 1000; // 5m

  private final String apiKey;
  private final String refreshToken;

  private AccessToken accessToken = null;

  public CustomAuthTokenProvider(FirebaseApp firebaseApp, Executor executor, String apiKey,
                                 String refreshToken) {
    super(firebaseApp, executor, true);
    this.apiKey = apiKey;
    this.refreshToken = refreshToken;
  }

  @Override
  public void getToken(boolean forceRefresh, final GetTokenCompletionListener listener) {
    try {
      if (accessToken == null || forceRefresh || isAfterGracePeriodStart(accessToken)) {
        accessToken = CustomAuthUtils.signInWithCustomToken(apiKey, refreshToken);
      }
      listener.onSuccess(accessToken.getTokenValue());
    } catch (Exception e) {
      listener.onError(e.toString());
    }
  }

  private static boolean isAfterGracePeriodStart(AccessToken accessToken) {
    return accessToken.getExpirationTime().getTime() - REFRESH_GRACE_PERIOD_MILLIS
        > System.currentTimeMillis();
  }
}
