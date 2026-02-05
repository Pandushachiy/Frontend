package com.health.companion.data.repositories;

import com.health.companion.data.remote.api.WellnessApi;
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
public final class WellnessRepositoryImpl_Factory implements Factory<WellnessRepositoryImpl> {
  private final Provider<WellnessApi> wellnessApiProvider;

  public WellnessRepositoryImpl_Factory(Provider<WellnessApi> wellnessApiProvider) {
    this.wellnessApiProvider = wellnessApiProvider;
  }

  @Override
  public WellnessRepositoryImpl get() {
    return newInstance(wellnessApiProvider.get());
  }

  public static WellnessRepositoryImpl_Factory create(Provider<WellnessApi> wellnessApiProvider) {
    return new WellnessRepositoryImpl_Factory(wellnessApiProvider);
  }

  public static WellnessRepositoryImpl newInstance(WellnessApi wellnessApi) {
    return new WellnessRepositoryImpl(wellnessApi);
  }
}
