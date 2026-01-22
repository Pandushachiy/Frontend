package com.health.companion.presentation.screens.documents;

import android.content.Context;
import com.health.companion.data.repositories.DocumentRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DocumentsViewModel_Factory implements Factory<DocumentsViewModel> {
  private final Provider<DocumentRepository> documentRepositoryProvider;

  private final Provider<Context> contextProvider;

  public DocumentsViewModel_Factory(Provider<DocumentRepository> documentRepositoryProvider,
      Provider<Context> contextProvider) {
    this.documentRepositoryProvider = documentRepositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public DocumentsViewModel get() {
    return newInstance(documentRepositoryProvider.get(), contextProvider.get());
  }

  public static DocumentsViewModel_Factory create(
      Provider<DocumentRepository> documentRepositoryProvider, Provider<Context> contextProvider) {
    return new DocumentsViewModel_Factory(documentRepositoryProvider, contextProvider);
  }

  public static DocumentsViewModel newInstance(DocumentRepository documentRepository,
      Context context) {
    return new DocumentsViewModel(documentRepository, context);
  }
}
