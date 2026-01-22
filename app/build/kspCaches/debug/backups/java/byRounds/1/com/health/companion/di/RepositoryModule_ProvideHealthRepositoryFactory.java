package com.health.companion.di;

import com.health.companion.data.local.dao.HealthMetricDao;
import com.health.companion.data.local.dao.MoodEntryDao;
import com.health.companion.data.remote.api.HealthApi;
import com.health.companion.data.repositories.HealthRepository;
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
public final class RepositoryModule_ProvideHealthRepositoryFactory implements Factory<HealthRepository> {
  private final Provider<HealthApi> healthApiProvider;

  private final Provider<HealthMetricDao> healthMetricDaoProvider;

  private final Provider<MoodEntryDao> moodEntryDaoProvider;

  public RepositoryModule_ProvideHealthRepositoryFactory(Provider<HealthApi> healthApiProvider,
      Provider<HealthMetricDao> healthMetricDaoProvider,
      Provider<MoodEntryDao> moodEntryDaoProvider) {
    this.healthApiProvider = healthApiProvider;
    this.healthMetricDaoProvider = healthMetricDaoProvider;
    this.moodEntryDaoProvider = moodEntryDaoProvider;
  }

  @Override
  public HealthRepository get() {
    return provideHealthRepository(healthApiProvider.get(), healthMetricDaoProvider.get(), moodEntryDaoProvider.get());
  }

  public static RepositoryModule_ProvideHealthRepositoryFactory create(
      Provider<HealthApi> healthApiProvider, Provider<HealthMetricDao> healthMetricDaoProvider,
      Provider<MoodEntryDao> moodEntryDaoProvider) {
    return new RepositoryModule_ProvideHealthRepositoryFactory(healthApiProvider, healthMetricDaoProvider, moodEntryDaoProvider);
  }

  public static HealthRepository provideHealthRepository(HealthApi healthApi,
      HealthMetricDao healthMetricDao, MoodEntryDao moodEntryDao) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideHealthRepository(healthApi, healthMetricDao, moodEntryDao));
  }
}
