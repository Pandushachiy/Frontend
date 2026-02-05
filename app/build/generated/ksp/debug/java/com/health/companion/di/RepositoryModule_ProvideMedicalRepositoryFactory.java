package com.health.companion.di;

import com.health.companion.data.remote.api.MedicalApi;
import com.health.companion.data.repositories.MedicalRepository;
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
public final class RepositoryModule_ProvideMedicalRepositoryFactory implements Factory<MedicalRepository> {
  private final Provider<MedicalApi> medicalApiProvider;

  public RepositoryModule_ProvideMedicalRepositoryFactory(Provider<MedicalApi> medicalApiProvider) {
    this.medicalApiProvider = medicalApiProvider;
  }

  @Override
  public MedicalRepository get() {
    return provideMedicalRepository(medicalApiProvider.get());
  }

  public static RepositoryModule_ProvideMedicalRepositoryFactory create(
      Provider<MedicalApi> medicalApiProvider) {
    return new RepositoryModule_ProvideMedicalRepositoryFactory(medicalApiProvider);
  }

  public static MedicalRepository provideMedicalRepository(MedicalApi medicalApi) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideMedicalRepository(medicalApi));
  }
}
