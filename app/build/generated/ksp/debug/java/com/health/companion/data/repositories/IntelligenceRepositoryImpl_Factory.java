package com.health.companion.data.repositories;

import com.health.companion.data.remote.api.IntelligenceApi;
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
public final class IntelligenceRepositoryImpl_Factory implements Factory<IntelligenceRepositoryImpl> {
  private final Provider<IntelligenceApi> intelligenceApiProvider;

  public IntelligenceRepositoryImpl_Factory(Provider<IntelligenceApi> intelligenceApiProvider) {
    this.intelligenceApiProvider = intelligenceApiProvider;
  }

  @Override
  public IntelligenceRepositoryImpl get() {
    return newInstance(intelligenceApiProvider.get());
  }

  public static IntelligenceRepositoryImpl_Factory create(
      Provider<IntelligenceApi> intelligenceApiProvider) {
    return new IntelligenceRepositoryImpl_Factory(intelligenceApiProvider);
  }

  public static IntelligenceRepositoryImpl newInstance(IntelligenceApi intelligenceApi) {
    return new IntelligenceRepositoryImpl(intelligenceApi);
  }
}
