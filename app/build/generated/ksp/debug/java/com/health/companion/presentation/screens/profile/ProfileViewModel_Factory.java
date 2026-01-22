package com.health.companion.presentation.screens.profile;

import com.health.companion.data.repositories.ProfileRepository;
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
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<ProfileRepository> profileRepositoryProvider;

  public ProfileViewModel_Factory(Provider<ProfileRepository> profileRepositoryProvider) {
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(profileRepositoryProvider.get());
  }

  public static ProfileViewModel_Factory create(
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new ProfileViewModel_Factory(profileRepositoryProvider);
  }

  public static ProfileViewModel newInstance(ProfileRepository profileRepository) {
    return new ProfileViewModel(profileRepository);
  }
}
