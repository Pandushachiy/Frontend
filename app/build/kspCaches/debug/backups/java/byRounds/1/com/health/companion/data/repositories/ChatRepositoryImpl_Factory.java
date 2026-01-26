package com.health.companion.data.repositories;

import com.health.companion.data.local.dao.ChatMessageDao;
import com.health.companion.data.local.dao.ConversationDao;
import com.health.companion.data.remote.api.ChatApi;
import com.health.companion.services.WebSocketManager;
import com.health.companion.utils.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class ChatRepositoryImpl_Factory implements Factory<ChatRepositoryImpl> {
  private final Provider<ChatApi> chatApiProvider;

  private final Provider<ChatMessageDao> chatMessageDaoProvider;

  private final Provider<ConversationDao> conversationDaoProvider;

  private final Provider<WebSocketManager> webSocketManagerProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<OkHttpClient> okHttpClientProvider;

  public ChatRepositoryImpl_Factory(Provider<ChatApi> chatApiProvider,
      Provider<ChatMessageDao> chatMessageDaoProvider,
      Provider<ConversationDao> conversationDaoProvider,
      Provider<WebSocketManager> webSocketManagerProvider,
      Provider<TokenManager> tokenManagerProvider, Provider<OkHttpClient> okHttpClientProvider) {
    this.chatApiProvider = chatApiProvider;
    this.chatMessageDaoProvider = chatMessageDaoProvider;
    this.conversationDaoProvider = conversationDaoProvider;
    this.webSocketManagerProvider = webSocketManagerProvider;
    this.tokenManagerProvider = tokenManagerProvider;
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public ChatRepositoryImpl get() {
    return newInstance(chatApiProvider.get(), chatMessageDaoProvider.get(), conversationDaoProvider.get(), webSocketManagerProvider.get(), tokenManagerProvider.get(), okHttpClientProvider.get());
  }

  public static ChatRepositoryImpl_Factory create(Provider<ChatApi> chatApiProvider,
      Provider<ChatMessageDao> chatMessageDaoProvider,
      Provider<ConversationDao> conversationDaoProvider,
      Provider<WebSocketManager> webSocketManagerProvider,
      Provider<TokenManager> tokenManagerProvider, Provider<OkHttpClient> okHttpClientProvider) {
    return new ChatRepositoryImpl_Factory(chatApiProvider, chatMessageDaoProvider, conversationDaoProvider, webSocketManagerProvider, tokenManagerProvider, okHttpClientProvider);
  }

  public static ChatRepositoryImpl newInstance(ChatApi chatApi, ChatMessageDao chatMessageDao,
      ConversationDao conversationDao, WebSocketManager webSocketManager, TokenManager tokenManager,
      OkHttpClient okHttpClient) {
    return new ChatRepositoryImpl(chatApi, chatMessageDao, conversationDao, webSocketManager, tokenManager, okHttpClient);
  }
}
