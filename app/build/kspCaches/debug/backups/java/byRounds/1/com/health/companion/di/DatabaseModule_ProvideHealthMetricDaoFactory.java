package com.health.companion.di;

import com.health.companion.data.local.dao.HealthMetricDao;
import com.health.companion.data.local.database.HealthCompanionDatabase;
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
public final class DatabaseModule_ProvideHealthMetricDaoFactory implements Factory<HealthMetricDao> {
  private final Provider<HealthCompanionDatabase> databaseProvider;

  public DatabaseModule_ProvideHealthMetricDaoFactory(
      Provider<HealthCompanionDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public HealthMetricDao get() {
    return provideHealthMetricDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideHealthMetricDaoFactory create(
      Provider<HealthCompanionDatabase> databaseProvider) {
    return new DatabaseModule_ProvideHealthMetricDaoFactory(databaseProvider);
  }

  public static HealthMetricDao provideHealthMetricDao(HealthCompanionDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideHealthMetricDao(database));
  }
}
