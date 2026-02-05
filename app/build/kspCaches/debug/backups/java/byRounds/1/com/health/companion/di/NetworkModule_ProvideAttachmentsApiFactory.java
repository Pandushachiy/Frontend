package com.health.companion.di;

import com.health.companion.data.remote.api.AttachmentsApi;
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
public final class NetworkModule_ProvideAttachmentsApiFactory implements Factory<AttachmentsApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideAttachmentsApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public AttachmentsApi get() {
    return provideAttachmentsApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideAttachmentsApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideAttachmentsApiFactory(retrofitProvider);
  }

  public static AttachmentsApi provideAttachmentsApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideAttachmentsApi(retrofit));
  }
}
