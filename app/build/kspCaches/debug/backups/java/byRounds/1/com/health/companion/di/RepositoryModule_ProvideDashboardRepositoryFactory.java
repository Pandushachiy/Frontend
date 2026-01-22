package com.health.companion.di;

import com.health.companion.data.remote.api.DashboardApi;
import com.health.companion.data.repositories.DashboardRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class RepositoryModule_ProvideDashboardRepositoryFactory implements Factory<DashboardRepository> {
  private final Provider<DashboardApi> dashboardApiProvider;

  public RepositoryModule_ProvideDashboardRepositoryFactory(
      Provider<DashboardApi> dashboardApiProvider) {
    this.dashboardApiProvider = dashboardApiProvider;
  }

  @Override
  public DashboardRepository get() {
    return provideDashboardRepository(dashboardApiProvider.get());
  }

  public static RepositoryModule_ProvideDashboardRepositoryFactory create(
      Provider<DashboardApi> dashboardApiProvider) {
    return new RepositoryModule_ProvideDashboardRepositoryFactory(dashboardApiProvider);
  }

  public static DashboardRepository provideDashboardRepository(DashboardApi dashboardApi) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideDashboardRepository(dashboardApi));
  }
}
