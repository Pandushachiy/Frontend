package com.health.companion.presentation.screens.health;

import com.health.companion.data.repositories.HealthRepository;
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
public final class HealthViewModel_Factory implements Factory<HealthViewModel> {
  private final Provider<HealthRepository> healthRepositoryProvider;

  public HealthViewModel_Factory(Provider<HealthRepository> healthRepositoryProvider) {
    this.healthRepositoryProvider = healthRepositoryProvider;
  }

  @Override
  public HealthViewModel get() {
    return newInstance(healthRepositoryProvider.get());
  }

  public static HealthViewModel_Factory create(
      Provider<HealthRepository> healthRepositoryProvider) {
    return new HealthViewModel_Factory(healthRepositoryProvider);
  }

  public static HealthViewModel newInstance(HealthRepository healthRepository) {
    return new HealthViewModel(healthRepository);
  }
}
