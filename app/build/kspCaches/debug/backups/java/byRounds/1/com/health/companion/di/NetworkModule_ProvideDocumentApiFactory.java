package com.health.companion.di;

import com.health.companion.data.remote.api.DocumentApi;
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
public final class NetworkModule_ProvideDocumentApiFactory implements Factory<DocumentApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideDocumentApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public DocumentApi get() {
    return provideDocumentApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideDocumentApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideDocumentApiFactory(retrofitProvider);
  }

  public static DocumentApi provideDocumentApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideDocumentApi(retrofit));
  }
}
