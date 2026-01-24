package com.health.companion.di;

import com.health.companion.data.remote.api.PushApi;
import com.health.companion.data.repositories.PushRepository;
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
public final class RepositoryModule_ProvidePushRepositoryFactory implements Factory<PushRepository> {
  private final Provider<PushApi> pushApiProvider;

  public RepositoryModule_ProvidePushRepositoryFactory(Provider<PushApi> pushApiProvider) {
    this.pushApiProvider = pushApiProvider;
  }

  @Override
  public PushRepository get() {
    return providePushRepository(pushApiProvider.get());
  }

  public static RepositoryModule_ProvidePushRepositoryFactory create(
      Provider<PushApi> pushApiProvider) {
    return new RepositoryModule_ProvidePushRepositoryFactory(pushApiProvider);
  }

  public static PushRepository providePushRepository(PushApi pushApi) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.providePushRepository(pushApi));
  }
}
