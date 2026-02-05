package com.health.companion.di;

import com.health.companion.data.remote.api.MedicalApi;
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
public final class NetworkModule_ProvideMedicalApiFactory implements Factory<MedicalApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideMedicalApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public MedicalApi get() {
    return provideMedicalApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideMedicalApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideMedicalApiFactory(retrofitProvider);
  }

  public static MedicalApi provideMedicalApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideMedicalApi(retrofit));
  }
}
