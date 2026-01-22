package com.health.companion.presentation.screens.settings;

import android.content.Context;
import com.health.companion.data.repositories.AuthRepository;
import com.health.companion.utils.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<Context> contextProvider;

  public SettingsViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<TokenManager> tokenManagerProvider, Provider<Context> contextProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.tokenManagerProvider = tokenManagerProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(authRepositoryProvider.get(), tokenManagerProvider.get(), contextProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<TokenManager> tokenManagerProvider, Provider<Context> contextProvider) {
    return new SettingsViewModel_Factory(authRepositoryProvider, tokenManagerProvider, contextProvider);
  }

  public static SettingsViewModel newInstance(AuthRepository authRepository,
      TokenManager tokenManager, Context context) {
    return new SettingsViewModel(authRepository, tokenManager, context);
  }
}
