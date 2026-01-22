package com.health.companion.data.repositories;

import android.content.Context;
import com.health.companion.data.remote.api.VoiceApi;
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
public final class VoiceRepository_Factory implements Factory<VoiceRepository> {
  private final Provider<VoiceApi> voiceApiProvider;

  private final Provider<Context> contextProvider;

  public VoiceRepository_Factory(Provider<VoiceApi> voiceApiProvider,
      Provider<Context> contextProvider) {
    this.voiceApiProvider = voiceApiProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public VoiceRepository get() {
    return newInstance(voiceApiProvider.get(), contextProvider.get());
  }

  public static VoiceRepository_Factory create(Provider<VoiceApi> voiceApiProvider,
      Provider<Context> contextProvider) {
    return new VoiceRepository_Factory(voiceApiProvider, contextProvider);
  }

  public static VoiceRepository newInstance(VoiceApi voiceApi, Context context) {
    return new VoiceRepository(voiceApi, context);
  }
}
