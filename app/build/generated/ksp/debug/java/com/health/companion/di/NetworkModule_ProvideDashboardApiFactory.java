package com.health.companion.di;

import com.health.companion.data.remote.api.DashboardApi;
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
public final class NetworkModule_ProvideDashboardApiFactory implements Factory<DashboardApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideDashboardApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public DashboardApi get() {
    return provideDashboardApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideDashboardApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideDashboardApiFactory(retrofitProvider);
  }

  public static DashboardApi provideDashboardApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideDashboardApi(retrofit));
  }
}
