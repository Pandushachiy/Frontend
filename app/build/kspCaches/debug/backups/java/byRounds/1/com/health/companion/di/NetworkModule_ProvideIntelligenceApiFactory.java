package com.health.companion.di;

import com.health.companion.data.remote.api.IntelligenceApi;
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
public final class NetworkModule_ProvideIntelligenceApiFactory implements Factory<IntelligenceApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideIntelligenceApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public IntelligenceApi get() {
    return provideIntelligenceApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideIntelligenceApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideIntelligenceApiFactory(retrofitProvider);
  }

  public static IntelligenceApi provideIntelligenceApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideIntelligenceApi(retrofit));
  }
}
