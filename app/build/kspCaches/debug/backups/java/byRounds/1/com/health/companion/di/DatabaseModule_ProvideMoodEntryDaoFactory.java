package com.health.companion.di;

import com.health.companion.data.local.dao.MoodEntryDao;
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
public final class DatabaseModule_ProvideMoodEntryDaoFactory implements Factory<MoodEntryDao> {
  private final Provider<HealthCompanionDatabase> databaseProvider;

  public DatabaseModule_ProvideMoodEntryDaoFactory(
      Provider<HealthCompanionDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public MoodEntryDao get() {
    return provideMoodEntryDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideMoodEntryDaoFactory create(
      Provider<HealthCompanionDatabase> databaseProvider) {
    return new DatabaseModule_ProvideMoodEntryDaoFactory(databaseProvider);
  }

  public static MoodEntryDao provideMoodEntryDao(HealthCompanionDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideMoodEntryDao(database));
  }
}
