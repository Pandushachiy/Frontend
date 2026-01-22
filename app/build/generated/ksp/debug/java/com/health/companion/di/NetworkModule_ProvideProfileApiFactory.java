package com.health.companion.di;

import com.health.companion.data.remote.api.ProfileApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideProfileApiFactory implements Factory<ProfileApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideProfileApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public ProfileApi get() {
    return provideProfileApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideProfileApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideProfileApiFactory(retrofitProvider);
  }

  public static ProfileApi provideProfileApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideProfileApi(retrofit));
  }
}
