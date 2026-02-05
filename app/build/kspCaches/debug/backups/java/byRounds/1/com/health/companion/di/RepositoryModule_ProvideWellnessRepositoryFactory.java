package com.health.companion.di;

import com.health.companion.data.remote.api.WellnessApi;
import com.health.companion.data.repositories.WellnessRepository;
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
public final class RepositoryModule_ProvideWellnessRepositoryFactory implements Factory<WellnessRepository> {
  private final Provider<WellnessApi> wellnessApiProvider;

  public RepositoryModule_ProvideWellnessRepositoryFactory(
      Provider<WellnessApi> wellnessApiProvider) {
    this.wellnessApiProvider = wellnessApiProvider;
  }

  @Override
  public WellnessRepository get() {
    return provideWellnessRepository(wellnessApiProvider.get());
  }

  public static RepositoryModule_ProvideWellnessRepositoryFactory create(
      Provider<WellnessApi> wellnessApiProvider) {
    return new RepositoryModule_ProvideWellnessRepositoryFactory(wellnessApiProvider);
  }

  public static WellnessRepository provideWellnessRepository(WellnessApi wellnessApi) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideWellnessRepository(wellnessApi));
  }
}
