package com.health.companion.presentation.screens.chat;

import androidx.lifecycle.SavedStateHandle;
import com.health.companion.data.repositories.AuthRepository;
import com.health.companion.data.repositories.ChatRepository;
import com.health.companion.data.repositories.DocumentRepository;
import com.health.companion.data.repositories.VoiceRepository;
import com.health.companion.ml.voice.VoiceInputManager;
import com.health.companion.utils.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<DocumentRepository> documentRepositoryProvider;

  private final Provider<VoiceInputManager> voiceInputManagerProvider;

  private final Provider<VoiceRepository> voiceRepositoryProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public ChatViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<DocumentRepository> documentRepositoryProvider,
      Provider<VoiceInputManager> voiceInputManagerProvider,
      Provider<VoiceRepository> voiceRepositoryProvider,
      Provider<TokenManager> tokenManagerProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.documentRepositoryProvider = documentRepositoryProvider;
    this.voiceInputManagerProvider = voiceInputManagerProvider;
    this.voiceRepositoryProvider = voiceRepositoryProvider;
    this.tokenManagerProvider = tokenManagerProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(chatRepositoryProvider.get(), authRepositoryProvider.get(), documentRepositoryProvider.get(), voiceInputManagerProvider.get(), voiceRepositoryProvider.get(), tokenManagerProvider.get(), savedStateHandleProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<ChatRepository> chatRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<DocumentRepository> documentRepositoryProvider,
      Provider<VoiceInputManager> voiceInputManagerProvider,
      Provider<VoiceRepository> voiceRepositoryProvider,
      Provider<TokenManager> tokenManagerProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new ChatViewModel_Factory(chatRepositoryProvider, authRepositoryProvider, documentRepositoryProvider, voiceInputManagerProvider, voiceRepositoryProvider, tokenManagerProvider, savedStateHandleProvider);
  }

  public static ChatViewModel newInstance(ChatRepository chatRepository,
      AuthRepository authRepository, DocumentRepository documentRepository,
      VoiceInputManager voiceInputManager, VoiceRepository voiceRepository,
      TokenManager tokenManager, SavedStateHandle savedStateHandle) {
    return new ChatViewModel(chatRepository, authRepository, documentRepository, voiceInputManager, voiceRepository, tokenManager, savedStateHandle);
  }
}
