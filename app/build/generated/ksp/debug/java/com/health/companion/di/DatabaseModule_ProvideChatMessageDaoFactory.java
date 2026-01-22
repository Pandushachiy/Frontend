package com.health.companion.di;

import com.health.companion.data.local.dao.ChatMessageDao;
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
public final class DatabaseModule_ProvideChatMessageDaoFactory implements Factory<ChatMessageDao> {
  private final Provider<HealthCompanionDatabase> databaseProvider;

  public DatabaseModule_ProvideChatMessageDaoFactory(
      Provider<HealthCompanionDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ChatMessageDao get() {
    return provideChatMessageDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideChatMessageDaoFactory create(
      Provider<HealthCompanionDatabase> databaseProvider) {
    return new DatabaseModule_ProvideChatMessageDaoFactory(databaseProvider);
  }

  public static ChatMessageDao provideChatMessageDao(HealthCompanionDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideChatMessageDao(database));
  }
}
