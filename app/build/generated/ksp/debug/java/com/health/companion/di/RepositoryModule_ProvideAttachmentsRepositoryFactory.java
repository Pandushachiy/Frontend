package com.health.companion.di;

import android.content.Context;
import com.health.companion.data.remote.api.AttachmentsApi;
import com.health.companion.data.repositories.AttachmentsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class RepositoryModule_ProvideAttachmentsRepositoryFactory implements Factory<AttachmentsRepository> {
  private final Provider<AttachmentsApi> attachmentsApiProvider;

  private final Provider<Context> contextProvider;

  public RepositoryModule_ProvideAttachmentsRepositoryFactory(
      Provider<AttachmentsApi> attachmentsApiProvider, Provider<Context> contextProvider) {
    this.attachmentsApiProvider = attachmentsApiProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public AttachmentsRepository get() {
    return provideAttachmentsRepository(attachmentsApiProvider.get(), contextProvider.get());
  }

  public static RepositoryModule_ProvideAttachmentsRepositoryFactory create(
      Provider<AttachmentsApi> attachmentsApiProvider, Provider<Context> contextProvider) {
    return new RepositoryModule_ProvideAttachmentsRepositoryFactory(attachmentsApiProvider, contextProvider);
  }

  public static AttachmentsRepository provideAttachmentsRepository(AttachmentsApi attachmentsApi,
      Context context) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideAttachmentsRepository(attachmentsApi, context));
  }
}
