package com.health.companion.data.repositories;

import com.health.companion.data.remote.api.LifeContextApi;
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
public final class LifeContextRepositoryImpl_Factory implements Factory<LifeContextRepositoryImpl> {
  private final Provider<LifeContextApi> apiProvider;

  public LifeContextRepositoryImpl_Factory(Provider<LifeContextApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public LifeContextRepositoryImpl get() {
    return newInstance(apiProvider.get());
  }

  public static LifeContextRepositoryImpl_Factory create(Provider<LifeContextApi> apiProvider) {
    return new LifeContextRepositoryImpl_Factory(apiProvider);
  }

  public static LifeContextRepositoryImpl newInstance(LifeContextApi api) {
    return new LifeContextRepositoryImpl(api);
  }
}
