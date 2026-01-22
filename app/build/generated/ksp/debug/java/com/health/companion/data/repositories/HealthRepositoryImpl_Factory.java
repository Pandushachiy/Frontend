package com.health.companion.data.repositories;

import com.health.companion.data.local.dao.HealthMetricDao;
import com.health.companion.data.local.dao.MoodEntryDao;
import com.health.companion.data.remote.api.HealthApi;
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
public final class HealthRepositoryImpl_Factory implements Factory<HealthRepositoryImpl> {
  private final Provider<HealthApi> healthApiProvider;

  private final Provider<HealthMetricDao> healthMetricDaoProvider;

  private final Provider<MoodEntryDao> moodEntryDaoProvider;

  public HealthRepositoryImpl_Factory(Provider<HealthApi> healthApiProvider,
      Provider<HealthMetricDao> healthMetricDaoProvider,
      Provider<MoodEntryDao> moodEntryDaoProvider) {
    this.healthApiProvider = healthApiProvider;
    this.healthMetricDaoProvider = healthMetricDaoProvider;
    this.moodEntryDaoProvider = moodEntryDaoProvider;
  }

  @Override
  public HealthRepositoryImpl get() {
    return newInstance(healthApiProvider.get(), healthMetricDaoProvider.get(), moodEntryDaoProvider.get());
  }

  public static HealthRepositoryImpl_Factory create(Provider<HealthApi> healthApiProvider,
      Provider<HealthMetricDao> healthMetricDaoProvider,
      Provider<MoodEntryDao> moodEntryDaoProvider) {
    return new HealthRepositoryImpl_Factory(healthApiProvider, healthMetricDaoProvider, moodEntryDaoProvider);
  }

  public static HealthRepositoryImpl newInstance(HealthApi healthApi,
      HealthMetricDao healthMetricDao, MoodEntryDao moodEntryDao) {
    return new HealthRepositoryImpl(healthApi, healthMetricDao, moodEntryDao);
  }
}
