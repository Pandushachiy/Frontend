package com.health.companion.di;

import com.health.companion.data.remote.api.LifeContextApi;
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
public final class NetworkModule_ProvideLifeContextApiFactory implements Factory<LifeContextApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideLifeContextApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public LifeContextApi get() {
    return provideLifeContextApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideLifeContextApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideLifeContextApiFactory(retrofitProvider);
  }

  public static LifeContextApi provideLifeContextApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideLifeContextApi(retrofit));
  }
}
