package com.health.companion.di;

import com.health.companion.data.remote.api.LifeContextApi;
import com.health.companion.data.repositories.LifeContextRepository;
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
public final class RepositoryModule_ProvideLifeContextRepositoryFactory implements Factory<LifeContextRepository> {
  private final Provider<LifeContextApi> lifeContextApiProvider;

  public RepositoryModule_ProvideLifeContextRepositoryFactory(
      Provider<LifeContextApi> lifeContextApiProvider) {
    this.lifeContextApiProvider = lifeContextApiProvider;
  }

  @Override
  public LifeContextRepository get() {
    return provideLifeContextRepository(lifeContextApiProvider.get());
  }

  public static RepositoryModule_ProvideLifeContextRepositoryFactory create(
      Provider<LifeContextApi> lifeContextApiProvider) {
    return new RepositoryModule_ProvideLifeContextRepositoryFactory(lifeContextApiProvider);
  }

  public static LifeContextRepository provideLifeContextRepository(LifeContextApi lifeContextApi) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideLifeContextRepository(lifeContextApi));
  }
}
