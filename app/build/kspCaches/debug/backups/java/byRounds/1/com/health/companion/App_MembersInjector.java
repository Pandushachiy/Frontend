package com.health.companion;

import coil.ImageLoader;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class App_MembersInjector implements MembersInjector<App> {
  private final Provider<ImageLoader> imageLoaderProvider;

  public App_MembersInjector(Provider<ImageLoader> imageLoaderProvider) {
    this.imageLoaderProvider = imageLoaderProvider;
  }

  public static MembersInjector<App> create(Provider<ImageLoader> imageLoaderProvider) {
    return new App_MembersInjector(imageLoaderProvider);
  }

  @Override
  public void injectMembers(App instance) {
    injectImageLoader(instance, imageLoaderProvider.get());
  }

  @InjectedFieldSignature("com.health.companion.App.imageLoader")
  public static void injectImageLoader(App instance, ImageLoader imageLoader) {
    instance.imageLoader = imageLoader;
  }
}
