package com.health.companion.di;

import com.health.companion.data.local.dao.ChatMessageDao;
import com.health.companion.data.local.dao.ConversationDao;
import com.health.companion.data.remote.api.ChatApi;
import com.health.companion.data.repositories.ChatRepository;
import com.health.companion.services.WebSocketManager;
import com.health.companion.utils.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class RepositoryModule_ProvideChatRepositoryFactory implements Factory<ChatRepository> {
  private final Provider<ChatApi> chatApiProvider;

  private final Provider<ChatMessageDao> chatMessageDaoProvider;

  private final Provider<ConversationDao> conversationDaoProvider;

  private final Provider<WebSocketManager> webSocketManagerProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public RepositoryModule_ProvideChatRepositoryFactory(Provider<ChatApi> chatApiProvider,
      Provider<ChatMessageDao> chatMessageDaoProvider,
      Provider<ConversationDao> conversationDaoProvider,
      Provider<WebSocketManager> webSocketManagerProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.chatApiProvider = chatApiProvider;
    this.chatMessageDaoProvider = chatMessageDaoProvider;
    this.conversationDaoProvider = conversationDaoProvider;
    this.webSocketManagerProvider = webSocketManagerProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public ChatRepository get() {
    return provideChatRepository(chatApiProvider.get(), chatMessageDaoProvider.get(), conversationDaoProvider.get(), webSocketManagerProvider.get(), tokenManagerProvider.get());
  }

  public static RepositoryModule_ProvideChatRepositoryFactory create(
      Provider<ChatApi> chatApiProvider, Provider<ChatMessageDao> chatMessageDaoProvider,
      Provider<ConversationDao> conversationDaoProvider,
      Provider<WebSocketManager> webSocketManagerProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new RepositoryModule_ProvideChatRepositoryFactory(chatApiProvider, chatMessageDaoProvider, conversationDaoProvider, webSocketManagerProvider, tokenManagerProvider);
  }

  public static ChatRepository provideChatRepository(ChatApi chatApi, ChatMessageDao chatMessageDao,
      ConversationDao conversationDao, WebSocketManager webSocketManager,
      TokenManager tokenManager) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideChatRepository(chatApi, chatMessageDao, conversationDao, webSocketManager, tokenManager));
  }
}
