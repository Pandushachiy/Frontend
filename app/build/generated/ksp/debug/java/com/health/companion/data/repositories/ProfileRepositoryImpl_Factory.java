package com.health.companion.data.repositories;

import com.health.companion.data.remote.api.ProfileApi;
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
public final class ProfileRepositoryImpl_Factory implements Factory<ProfileRepositoryImpl> {
  private final Provider<ProfileApi> profileApiProvider;

  public ProfileRepositoryImpl_Factory(Provider<ProfileApi> profileApiProvider) {
    this.profileApiProvider = profileApiProvider;
  }

  @Override
  public ProfileRepositoryImpl get() {
    return newInstance(profileApiProvider.get());
  }

  public static ProfileRepositoryImpl_Factory create(Provider<ProfileApi> profileApiProvider) {
    return new ProfileRepositoryImpl_Factory(profileApiProvider);
  }

  public static ProfileRepositoryImpl newInstance(ProfileApi profileApi) {
    return new ProfileRepositoryImpl(profileApi);
  }
}
