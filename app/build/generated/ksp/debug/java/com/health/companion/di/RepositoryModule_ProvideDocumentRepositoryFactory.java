package com.health.companion.di;

import android.content.Context;
import com.health.companion.data.local.dao.DocumentDao;
import com.health.companion.data.remote.api.DocumentApi;
import com.health.companion.data.repositories.DocumentRepository;
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
public final class RepositoryModule_ProvideDocumentRepositoryFactory implements Factory<DocumentRepository> {
  private final Provider<DocumentApi> documentApiProvider;

  private final Provider<DocumentDao> documentDaoProvider;

  private final Provider<Context> contextProvider;

  public RepositoryModule_ProvideDocumentRepositoryFactory(
      Provider<DocumentApi> documentApiProvider, Provider<DocumentDao> documentDaoProvider,
      Provider<Context> contextProvider) {
    this.documentApiProvider = documentApiProvider;
    this.documentDaoProvider = documentDaoProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public DocumentRepository get() {
    return provideDocumentRepository(documentApiProvider.get(), documentDaoProvider.get(), contextProvider.get());
  }

  public static RepositoryModule_ProvideDocumentRepositoryFactory create(
      Provider<DocumentApi> documentApiProvider, Provider<DocumentDao> documentDaoProvider,
      Provider<Context> contextProvider) {
    return new RepositoryModule_ProvideDocumentRepositoryFactory(documentApiProvider, documentDaoProvider, contextProvider);
  }

  public static DocumentRepository provideDocumentRepository(DocumentApi documentApi,
      DocumentDao documentDao, Context context) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideDocumentRepository(documentApi, documentDao, context));
  }
}
