package com.health.companion.services;

import com.health.companion.utils.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class WebSocketManager_Factory implements Factory<WebSocketManager> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public WebSocketManager_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public WebSocketManager get() {
    return newInstance(okHttpClientProvider.get(), tokenManagerProvider.get());
  }

  public static WebSocketManager_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new WebSocketManager_Factory(okHttpClientProvider, tokenManagerProvider);
  }

  public static WebSocketManager newInstance(OkHttpClient okHttpClient, TokenManager tokenManager) {
    return new WebSocketManager(okHttpClient, tokenManager);
  }
}
