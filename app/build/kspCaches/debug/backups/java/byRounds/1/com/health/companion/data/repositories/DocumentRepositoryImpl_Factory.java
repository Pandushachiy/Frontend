package com.health.companion.data.repositories;

import android.content.Context;
import com.health.companion.data.local.dao.DocumentDao;
import com.health.companion.data.remote.api.DocumentApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class DocumentRepositoryImpl_Factory implements Factory<DocumentRepositoryImpl> {
  private final Provider<DocumentApi> documentApiProvider;

  private final Provider<DocumentDao> documentDaoProvider;

  private final Provider<Context> contextProvider;

  public DocumentRepositoryImpl_Factory(Provider<DocumentApi> documentApiProvider,
      Provider<DocumentDao> documentDaoProvider, Provider<Context> contextProvider) {
    this.documentApiProvider = documentApiProvider;
    this.documentDaoProvider = documentDaoProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public DocumentRepositoryImpl get() {
    return newInstance(documentApiProvider.get(), documentDaoProvider.get(), contextProvider.get());
  }

  public static DocumentRepositoryImpl_Factory create(Provider<DocumentApi> documentApiProvider,
      Provider<DocumentDao> documentDaoProvider, Provider<Context> contextProvider) {
    return new DocumentRepositoryImpl_Factory(documentApiProvider, documentDaoProvider, contextProvider);
  }

  public static DocumentRepositoryImpl newInstance(DocumentApi documentApi, DocumentDao documentDao,
      Context context) {
    return new DocumentRepositoryImpl(documentApi, documentDao, context);
  }
}
