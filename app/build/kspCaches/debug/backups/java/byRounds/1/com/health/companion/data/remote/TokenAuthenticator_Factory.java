package com.health.companion.data.remote;

import com.health.companion.utils.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class TokenAuthenticator_Factory implements Factory<TokenAuthenticator> {
  private final Provider<TokenManager> tokenManagerProvider;

  public TokenAuthenticator_Factory(Provider<TokenManager> tokenManagerProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public TokenAuthenticator get() {
    return newInstance(tokenManagerProvider.get());
  }

  public static TokenAuthenticator_Factory create(Provider<TokenManager> tokenManagerProvider) {
    return new TokenAuthenticator_Factory(tokenManagerProvider);
  }

  public static TokenAuthenticator newInstance(TokenManager tokenManager) {
    return new TokenAuthenticator(tokenManager);
  }
}
