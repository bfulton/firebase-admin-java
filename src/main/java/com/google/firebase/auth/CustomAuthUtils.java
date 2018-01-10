package com.google.firebase.auth;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.core.AuthTokenProvider;
import com.google.firebase.database.core.Context;
import com.google.firebase.database.core.CustomAuthTokenProvider;
import com.google.firebase.database.core.DatabaseConfig;
import com.google.firebase.database.core.Platform;
import com.google.firebase.database.core.RunLoop;
import com.google.firebase.database.utilities.DefaultRunLoop;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class CustomAuthUtils {
  private static final String SECURE_TOKEN_ENDPOINT = "https://securetoken.googleapis.com/v1/token";

  public static void initializeWithCustomToken(String databaseUrl, String apiKey,
                                               String refreshToken) throws IOException {
    AccessToken accessToken = signInWithCustomToken(apiKey, refreshToken);
    GoogleCredentials creds = GoogleCredentials.of(accessToken);

    FirebaseOptions options = new FirebaseOptions.Builder()
        .setCredentials(creds)
        .setDatabaseUrl(databaseUrl)
        .build();

    FirebaseApp app = FirebaseApp.initializeApp(options);
    setCustomAuthTokenProvider(app, apiKey, refreshToken);
  }

  public static AccessToken signInWithCustomToken(String apiKey, String refreshToken)
      throws IOException {
    HttpTransport transport = new NetHttpTransport();
    try {
      GenericUrl url = new GenericUrl(SECURE_TOKEN_ENDPOINT + "?key=" + apiKey);
      Map<String, Object> content = new HashMap<String, Object>();
      content.put("grant_type", "refresh_token");
      content.put("refresh_token", refreshToken);
      HttpRequest request = transport
          .createRequestFactory()
          .buildPostRequest(url, new UrlEncodedContent(content));
      JsonFactory jsonFactory = new JacksonFactory();
      request.setParser(new JsonObjectParser(jsonFactory));
      HttpResponse response = request.execute();
      try {
        GenericJson json = response.parseAs(GenericJson.class);
        String accessTokenStr = json.get("access_token").toString();
        int expiresIn = Integer.parseInt(json.get("expires_in").toString());
        Date expirationTime = new Date(System.currentTimeMillis() + expiresIn * 1000);
        return new AccessToken(accessTokenStr, expirationTime);
      } finally {
        response.disconnect();
      }
    } finally {
      transport.shutdown();
    }
  }

  private static void setCustomAuthTokenProvider(FirebaseApp app, String apiKey,
                                                 String refreshToken) {
    try {
      FirebaseDatabase db = FirebaseDatabase.getInstance(app);
      Field configField = FirebaseDatabase.class.getDeclaredField("config");
      configField.setAccessible(true);
      DatabaseConfig dbcfg = (DatabaseConfig) configField.get(db);
      dbcfg.getPlatformVersion(); // has useful side-effect
      Field platformField = Context.class.getDeclaredField("platform");
      platformField.setAccessible(true);
      Platform platform = (Platform) platformField.get(dbcfg);
      RunLoop runLoop = platform.newRunLoop(dbcfg);
      dbcfg.setRunLoop(runLoop);
      Executor executor = ((DefaultRunLoop) runLoop).getExecutorService();
      AuthTokenProvider authTokenProvider = new CustomAuthTokenProvider(app, executor, apiKey,
          refreshToken);
      dbcfg.setAuthTokenProvider(authTokenProvider);
    } catch (Exception e) {
      throw new IllegalStateException("trouble with reflection on firebase classes", e);
    }
  }
}
