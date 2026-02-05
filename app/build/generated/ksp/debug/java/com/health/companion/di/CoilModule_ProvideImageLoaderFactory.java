package com.health.companion.di;

import android.content.Context;
import coil.ImageLoader;
import com.health.companion.utils.TokenManager;
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
public final class CoilModule_ProvideImageLoaderFactory implements Factory<ImageLoader> {
  private final Provider<Context> contextProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public CoilModule_ProvideImageLoaderFactory(Provider<Context> contextProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.contextProvider = contextProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public ImageLoader get() {
    return provideImageLoader(contextProvider.get(), tokenManagerProvider.get());
  }

  public static CoilModule_ProvideImageLoaderFactory create(Provider<Context> contextProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new CoilModule_ProvideImageLoaderFactory(contextProvider, tokenManagerProvider);
  }

  public static ImageLoader provideImageLoader(Context context, TokenManager tokenManager) {
    return Preconditions.checkNotNullFromProvides(CoilModule.INSTANCE.provideImageLoader(context, tokenManager));
  }
}
