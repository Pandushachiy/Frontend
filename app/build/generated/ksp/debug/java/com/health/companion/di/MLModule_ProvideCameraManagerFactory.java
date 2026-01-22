package com.health.companion.di;

import android.content.Context;
import com.health.companion.ml.camera.CameraManager;
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
public final class MLModule_ProvideCameraManagerFactory implements Factory<CameraManager> {
  private final Provider<Context> contextProvider;

  public MLModule_ProvideCameraManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public CameraManager get() {
    return provideCameraManager(contextProvider.get());
  }

  public static MLModule_ProvideCameraManagerFactory create(Provider<Context> contextProvider) {
    return new MLModule_ProvideCameraManagerFactory(contextProvider);
  }

  public static CameraManager provideCameraManager(Context context) {
    return Preconditions.checkNotNullFromProvides(MLModule.INSTANCE.provideCameraManager(context));
  }
}
