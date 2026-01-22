package com.health.companion.presentation.screens.dashboard;

import com.health.companion.data.repositories.DashboardRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<DashboardRepository> dashboardRepositoryProvider;

  public DashboardViewModel_Factory(Provider<DashboardRepository> dashboardRepositoryProvider) {
    this.dashboardRepositoryProvider = dashboardRepositoryProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(dashboardRepositoryProvider.get());
  }

  public static DashboardViewModel_Factory create(
      Provider<DashboardRepository> dashboardRepositoryProvider) {
    return new DashboardViewModel_Factory(dashboardRepositoryProvider);
  }

  public static DashboardViewModel newInstance(DashboardRepository dashboardRepository) {
    return new DashboardViewModel(dashboardRepository);
  }
}
