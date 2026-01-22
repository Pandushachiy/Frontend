package com.health.companion.di;

import android.content.Context;
import com.health.companion.ml.voice.VoiceInputManager;
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
public final class MLModule_ProvideVoiceInputManagerFactory implements Factory<VoiceInputManager> {
  private final Provider<Context> contextProvider;

  public MLModule_ProvideVoiceInputManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public VoiceInputManager get() {
    return provideVoiceInputManager(contextProvider.get());
  }

  public static MLModule_ProvideVoiceInputManagerFactory create(Provider<Context> contextProvider) {
    return new MLModule_ProvideVoiceInputManagerFactory(contextProvider);
  }

  public static VoiceInputManager provideVoiceInputManager(Context context) {
    return Preconditions.checkNotNullFromProvides(MLModule.INSTANCE.provideVoiceInputManager(context));
  }
}
