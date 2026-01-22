package com.health.companion.di;

import com.health.companion.data.remote.api.AuthApi;
import com.health.companion.data.repositories.AuthRepository;
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
public final class RepositoryModule_ProvideAuthRepositoryFactory implements Factory<AuthRepository> {
  private final Provider<AuthApi> authApiProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public RepositoryModule_ProvideAuthRepositoryFactory(Provider<AuthApi> authApiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.authApiProvider = authApiProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public AuthRepository get() {
    return provideAuthRepository(authApiProvider.get(), tokenManagerProvider.get());
  }

  public static RepositoryModule_ProvideAuthRepositoryFactory create(
      Provider<AuthApi> authApiProvider, Provider<TokenManager> tokenManagerProvider) {
    return new RepositoryModule_ProvideAuthRepositoryFactory(authApiProvider, tokenManagerProvider);
  }

  public static AuthRepository provideAuthRepository(AuthApi authApi, TokenManager tokenManager) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideAuthRepository(authApi, tokenManager));
  }
}
