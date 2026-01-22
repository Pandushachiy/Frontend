package com.health.companion.di;

import com.health.companion.data.remote.api.HealthApi;
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
public final class NetworkModule_ProvideHealthApiFactory implements Factory<HealthApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideHealthApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public HealthApi get() {
    return provideHealthApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideHealthApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideHealthApiFactory(retrofitProvider);
  }

  public static HealthApi provideHealthApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideHealthApi(retrofit));
  }
}
