package com.health.companion.di;

import com.health.companion.data.remote.api.WellnessApi;
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
public final class NetworkModule_ProvideWellnessApiFactory implements Factory<WellnessApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideWellnessApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public WellnessApi get() {
    return provideWellnessApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideWellnessApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideWellnessApiFactory(retrofitProvider);
  }

  public static WellnessApi provideWellnessApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideWellnessApi(retrofit));
  }
}
