package com.health.companion.di;

import com.health.companion.data.local.dao.DocumentDao;
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
public final class DatabaseModule_ProvideDocumentDaoFactory implements Factory<DocumentDao> {
  private final Provider<HealthCompanionDatabase> databaseProvider;

  public DatabaseModule_ProvideDocumentDaoFactory(
      Provider<HealthCompanionDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public DocumentDao get() {
    return provideDocumentDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideDocumentDaoFactory create(
      Provider<HealthCompanionDatabase> databaseProvider) {
    return new DatabaseModule_ProvideDocumentDaoFactory(databaseProvider);
  }

  public static DocumentDao provideDocumentDao(HealthCompanionDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDocumentDao(database));
  }
}
