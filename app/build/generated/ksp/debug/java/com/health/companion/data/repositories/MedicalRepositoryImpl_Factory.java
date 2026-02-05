package com.health.companion.data.repositories;

import com.health.companion.data.remote.api.MedicalApi;
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
public final class MedicalRepositoryImpl_Factory implements Factory<MedicalRepositoryImpl> {
  private final Provider<MedicalApi> apiProvider;

  public MedicalRepositoryImpl_Factory(Provider<MedicalApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public MedicalRepositoryImpl get() {
    return newInstance(apiProvider.get());
  }

  public static MedicalRepositoryImpl_Factory create(Provider<MedicalApi> apiProvider) {
    return new MedicalRepositoryImpl_Factory(apiProvider);
  }

  public static MedicalRepositoryImpl newInstance(MedicalApi api) {
    return new MedicalRepositoryImpl(api);
  }
}
