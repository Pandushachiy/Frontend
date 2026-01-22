package com.health.companion.di;

import com.health.companion.data.remote.api.ProfileApi;
import com.health.companion.data.repositories.ProfileRepository;
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
public final class RepositoryModule_ProvideProfileRepositoryFactory implements Factory<ProfileRepository> {
  private final Provider<ProfileApi> profileApiProvider;

  public RepositoryModule_ProvideProfileRepositoryFactory(Provider<ProfileApi> profileApiProvider) {
    this.profileApiProvider = profileApiProvider;
  }

  @Override
  public ProfileRepository get() {
    return provideProfileRepository(profileApiProvider.get());
  }

  public static RepositoryModule_ProvideProfileRepositoryFactory create(
      Provider<ProfileApi> profileApiProvider) {
    return new RepositoryModule_ProvideProfileRepositoryFactory(profileApiProvider);
  }

  public static ProfileRepository provideProfileRepository(ProfileApi profileApi) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideProfileRepository(profileApi));
  }
}
