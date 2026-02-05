package com.health.companion.data.repositories;

import android.content.Context;
import com.health.companion.data.remote.api.AttachmentsApi;
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
public final class AttachmentsRepositoryImpl_Factory implements Factory<AttachmentsRepositoryImpl> {
  private final Provider<AttachmentsApi> apiProvider;

  private final Provider<Context> contextProvider;

  public AttachmentsRepositoryImpl_Factory(Provider<AttachmentsApi> apiProvider,
      Provider<Context> contextProvider) {
    this.apiProvider = apiProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public AttachmentsRepositoryImpl get() {
    return newInstance(apiProvider.get(), contextProvider.get());
  }

  public static AttachmentsRepositoryImpl_Factory create(Provider<AttachmentsApi> apiProvider,
      Provider<Context> contextProvider) {
    return new AttachmentsRepositoryImpl_Factory(apiProvider, contextProvider);
  }

  public static AttachmentsRepositoryImpl newInstance(AttachmentsApi api, Context context) {
    return new AttachmentsRepositoryImpl(api, context);
  }
}
