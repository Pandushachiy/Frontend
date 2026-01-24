package com.health.companion.data.repositories;

import com.health.companion.data.remote.api.DashboardApi;
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
public final class DashboardRepositoryImpl_Factory implements Factory<DashboardRepositoryImpl> {
  private final Provider<DashboardApi> dashboardApiProvider;

  public DashboardRepositoryImpl_Factory(Provider<DashboardApi> dashboardApiProvider) {
    this.dashboardApiProvider = dashboardApiProvider;
  }

  @Override
  public DashboardRepositoryImpl get() {
    return newInstance(dashboardApiProvider.get());
  }

  public static DashboardRepositoryImpl_Factory create(
      Provider<DashboardApi> dashboardApiProvider) {
    return new DashboardRepositoryImpl_Factory(dashboardApiProvider);
  }

  public static DashboardRepositoryImpl newInstance(DashboardApi dashboardApi) {
    return new DashboardRepositoryImpl(dashboardApi);
  }
}
