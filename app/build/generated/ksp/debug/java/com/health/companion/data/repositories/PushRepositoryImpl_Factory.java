package com.health.companion.data.repositories;

import com.health.companion.data.remote.api.PushApi;
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
public final class PushRepositoryImpl_Factory implements Factory<PushRepositoryImpl> {
  private final Provider<PushApi> pushApiProvider;

  public PushRepositoryImpl_Factory(Provider<PushApi> pushApiProvider) {
    this.pushApiProvider = pushApiProvider;
  }

  @Override
  public PushRepositoryImpl get() {
    return newInstance(pushApiProvider.get());
  }

  public static PushRepositoryImpl_Factory create(Provider<PushApi> pushApiProvider) {
    return new PushRepositoryImpl_Factory(pushApiProvider);
  }

  public static PushRepositoryImpl newInstance(PushApi pushApi) {
    return new PushRepositoryImpl(pushApi);
  }
}
