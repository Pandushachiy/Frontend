package com.health.companion.presentation.screens.wellness;

import com.health.companion.data.repositories.WellnessRepository;
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
public final class WellnessViewModel_Factory implements Factory<WellnessViewModel> {
  private final Provider<WellnessRepository> wellnessRepositoryProvider;

  public WellnessViewModel_Factory(Provider<WellnessRepository> wellnessRepositoryProvider) {
    this.wellnessRepositoryProvider = wellnessRepositoryProvider;
  }

  @Override
  public WellnessViewModel get() {
    return newInstance(wellnessRepositoryProvider.get());
  }

  public static WellnessViewModel_Factory create(
      Provider<WellnessRepository> wellnessRepositoryProvider) {
    return new WellnessViewModel_Factory(wellnessRepositoryProvider);
  }

  public static WellnessViewModel newInstance(WellnessRepository wellnessRepository) {
    return new WellnessViewModel(wellnessRepository);
  }
}
