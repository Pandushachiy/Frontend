package com.health.companion.di;

import com.health.companion.data.remote.TokenAuthenticator;
import com.health.companion.utils.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class NetworkModule_ProvideTokenAuthenticatorFactory implements Factory<TokenAuthenticator> {
  private final Provider<TokenManager> tokenManagerProvider;

  public NetworkModule_ProvideTokenAuthenticatorFactory(
      Provider<TokenManager> tokenManagerProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public TokenAuthenticator get() {
    return provideTokenAuthenticator(tokenManagerProvider.get());
  }

  public static NetworkModule_ProvideTokenAuthenticatorFactory create(
      Provider<TokenManager> tokenManagerProvider) {
    return new NetworkModule_ProvideTokenAuthenticatorFactory(tokenManagerProvider);
  }

  public static TokenAuthenticator provideTokenAuthenticator(TokenManager tokenManager) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideTokenAuthenticator(tokenManager));
  }
}
