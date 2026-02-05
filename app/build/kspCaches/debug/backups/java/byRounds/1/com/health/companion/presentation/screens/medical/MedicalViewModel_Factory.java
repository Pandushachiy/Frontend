package com.health.companion.presentation.screens.medical;

import com.health.companion.data.repositories.MedicalRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class MedicalViewModel_Factory implements Factory<MedicalViewModel> {
  private final Provider<MedicalRepository> repositoryProvider;

  public MedicalViewModel_Factory(Provider<MedicalRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public MedicalViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static MedicalViewModel_Factory create(Provider<MedicalRepository> repositoryProvider) {
    return new MedicalViewModel_Factory(repositoryProvider);
  }

  public static MedicalViewModel newInstance(MedicalRepository repository) {
    return new MedicalViewModel(repository);
  }
}
