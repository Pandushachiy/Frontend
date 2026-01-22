package com.health.companion.data.repositories;

import com.health.companion.data.remote.api.AuthApi;
import com.health.companion.utils.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<AuthApi> authApiProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public AuthRepositoryImpl_Factory(Provider<AuthApi> authApiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.authApiProvider = authApiProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(authApiProvider.get(), tokenManagerProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(Provider<AuthApi> authApiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new AuthRepositoryImpl_Factory(authApiProvider, tokenManagerProvider);
  }

  public static AuthRepositoryImpl newInstance(AuthApi authApi, TokenManager tokenManager) {
    return new AuthRepositoryImpl(authApi, tokenManager);
  }
}
