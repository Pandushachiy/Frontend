package com.health.companion.di;

import com.health.companion.data.remote.api.IntelligenceApi;
import com.health.companion.data.repositories.IntelligenceRepository;
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
public final class RepositoryModule_ProvideIntelligenceRepositoryFactory implements Factory<IntelligenceRepository> {
  private final Provider<IntelligenceApi> intelligenceApiProvider;

  public RepositoryModule_ProvideIntelligenceRepositoryFactory(
      Provider<IntelligenceApi> intelligenceApiProvider) {
    this.intelligenceApiProvider = intelligenceApiProvider;
  }

  @Override
  public IntelligenceRepository get() {
    return provideIntelligenceRepository(intelligenceApiProvider.get());
  }

  public static RepositoryModule_ProvideIntelligenceRepositoryFactory create(
      Provider<IntelligenceApi> intelligenceApiProvider) {
    return new RepositoryModule_ProvideIntelligenceRepositoryFactory(intelligenceApiProvider);
  }

  public static IntelligenceRepository provideIntelligenceRepository(
      IntelligenceApi intelligenceApi) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideIntelligenceRepository(intelligenceApi));
  }
}
