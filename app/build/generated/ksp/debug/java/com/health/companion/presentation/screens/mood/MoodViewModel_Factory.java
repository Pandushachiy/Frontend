package com.health.companion.presentation.screens.mood;

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
public final class MoodViewModel_Factory implements Factory<MoodViewModel> {
  private final Provider<HealthRepository> healthRepositoryProvider;

  public MoodViewModel_Factory(Provider<HealthRepository> healthRepositoryProvider) {
    this.healthRepositoryProvider = healthRepositoryProvider;
  }

  @Override
  public MoodViewModel get() {
    return newInstance(healthRepositoryProvider.get());
  }

  public static MoodViewModel_Factory create(Provider<HealthRepository> healthRepositoryProvider) {
    return new MoodViewModel_Factory(healthRepositoryProvider);
  }

  public static MoodViewModel newInstance(HealthRepository healthRepository) {
    return new MoodViewModel(healthRepository);
  }
}
