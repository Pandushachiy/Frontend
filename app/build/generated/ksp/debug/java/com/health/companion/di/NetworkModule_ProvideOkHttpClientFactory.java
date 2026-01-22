package com.health.companion.di;

import com.health.companion.data.remote.TokenAuthenticator;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

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
public final class NetworkModule_ProvideOkHttpClientFactory implements Factory<OkHttpClient> {
  private final Provider<HttpLoggingInterceptor> loggingInterceptorProvider;

  private final Provider<Interceptor> authInterceptorProvider;

  private final Provider<TokenAuthenticator> tokenAuthenticatorProvider;

  public NetworkModule_ProvideOkHttpClientFactory(
      Provider<HttpLoggingInterceptor> loggingInterceptorProvider,
      Provider<Interceptor> authInterceptorProvider,
      Provider<TokenAuthenticator> tokenAuthenticatorProvider) {
    this.loggingInterceptorProvider = loggingInterceptorProvider;
    this.authInterceptorProvider = authInterceptorProvider;
    this.tokenAuthenticatorProvider = tokenAuthenticatorProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideOkHttpClient(loggingInterceptorProvider.get(), authInterceptorProvider.get(), tokenAuthenticatorProvider.get());
  }

  public static NetworkModule_ProvideOkHttpClientFactory create(
      Provider<HttpLoggingInterceptor> loggingInterceptorProvider,
      Provider<Interceptor> authInterceptorProvider,
      Provider<TokenAuthenticator> tokenAuthenticatorProvider) {
    return new NetworkModule_ProvideOkHttpClientFactory(loggingInterceptorProvider, authInterceptorProvider, tokenAuthenticatorProvider);
  }

  public static OkHttpClient provideOkHttpClient(HttpLoggingInterceptor loggingInterceptor,
      Interceptor authInterceptor, TokenAuthenticator tokenAuthenticator) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideOkHttpClient(loggingInterceptor, authInterceptor, tokenAuthenticator));
  }
}
