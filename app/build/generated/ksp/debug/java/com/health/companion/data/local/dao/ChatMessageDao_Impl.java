package com.health.companion.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.health.companion.data.local.database.ChatMessageEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ChatMessageDao_Impl implements ChatMessageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChatMessageEntity> __insertionAdapterOfChatMessageEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByConversation;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public ChatMessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChatMessageEntity = new EntityInsertionAdapter<ChatMessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `chat_messages` (`id`,`conversationId`,`content`,`role`,`agentName`,`confidence`,`sources`,`provider`,`providerColor`,`modelUsed`,`tokensUsed`,`processingTime`,`createdAt`,`imageUrl`,`images`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChatMessageEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getConversationId());
        statement.bindString(3, entity.getContent());
        statement.bindString(4, entity.getRole());
        if (entity.getAgentName() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getAgentName());
        }
        if (entity.getConfidence() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getConfidence());
        }
        if (entity.getSources() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getSources());
        }
        if (entity.getProvider() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getProvider());
        }
        if (entity.getProviderColor() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getProviderColor());
        }
        if (entity.getModelUsed() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getModelUsed());
        }
        if (entity.getTokensUsed() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getTokensUsed());
        }
        if (entity.getProcessingTime() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getProcessingTime());
        }
        statement.bindLong(13, entity.getCreatedAt());
        if (entity.getImageUrl() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getImageUrl());
        }
        if (entity.getImages() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getImages());
        }
      }
    };
    this.__preparedStmtOfDeleteByConversation = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM chat_messages WHERE conversationId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM chat_messages WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM chat_messages";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final ChatMessageEntity message,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChatMessageEntity.insert(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<ChatMessageEntity> messages,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChatMessageEntity.insert(messages);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByConversation(final String conversationId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByConversation.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, conversationId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByConversation.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String messageId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, messageId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ChatMessageEntity>> getMessagesFlow(final String conversationId) {
    final String _sql = "SELECT * FROM chat_messages WHERE conversationId = ? ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, conversationId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"chat_messages"}, new Callable<List<ChatMessageEntity>>() {
      @Override
      @NonNull
      public List<ChatMessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfConversationId = CursorUtil.getColumnIndexOrThrow(_cursor, "conversationId");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfAgentName = CursorUtil.getColumnIndexOrThrow(_cursor, "agentName");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfSources = CursorUtil.getColumnIndexOrThrow(_cursor, "sources");
          final int _cursorIndexOfProvider = CursorUtil.getColumnIndexOrThrow(_cursor, "provider");
          final int _cursorIndexOfProviderColor = CursorUtil.getColumnIndexOrThrow(_cursor, "providerColor");
          final int _cursorIndexOfModelUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "modelUsed");
          final int _cursorIndexOfTokensUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "tokensUsed");
          final int _cursorIndexOfProcessingTime = CursorUtil.getColumnIndexOrThrow(_cursor, "processingTime");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfImages = CursorUtil.getColumnIndexOrThrow(_cursor, "images");
          final List<ChatMessageEntity> _result = new ArrayList<ChatMessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChatMessageEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpConversationId;
            _tmpConversationId = _cursor.getString(_cursorIndexOfConversationId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpAgentName;
            if (_cursor.isNull(_cursorIndexOfAgentName)) {
              _tmpAgentName = null;
            } else {
              _tmpAgentName = _cursor.getString(_cursorIndexOfAgentName);
            }
            final Float _tmpConfidence;
            if (_cursor.isNull(_cursorIndexOfConfidence)) {
              _tmpConfidence = null;
            } else {
              _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            }
            final String _tmpSources;
            if (_cursor.isNull(_cursorIndexOfSources)) {
              _tmpSources = null;
            } else {
              _tmpSources = _cursor.getString(_cursorIndexOfSources);
            }
            final String _tmpProvider;
            if (_cursor.isNull(_cursorIndexOfProvider)) {
              _tmpProvider = null;
            } else {
              _tmpProvider = _cursor.getString(_cursorIndexOfProvider);
            }
            final String _tmpProviderColor;
            if (_cursor.isNull(_cursorIndexOfProviderColor)) {
              _tmpProviderColor = null;
            } else {
              _tmpProviderColor = _cursor.getString(_cursorIndexOfProviderColor);
            }
            final String _tmpModelUsed;
            if (_cursor.isNull(_cursorIndexOfModelUsed)) {
              _tmpModelUsed = null;
            } else {
              _tmpModelUsed = _cursor.getString(_cursorIndexOfModelUsed);
            }
            final Integer _tmpTokensUsed;
            if (_cursor.isNull(_cursorIndexOfTokensUsed)) {
              _tmpTokensUsed = null;
            } else {
              _tmpTokensUsed = _cursor.getInt(_cursorIndexOfTokensUsed);
            }
            final Integer _tmpProcessingTime;
            if (_cursor.isNull(_cursorIndexOfProcessingTime)) {
              _tmpProcessingTime = null;
            } else {
              _tmpProcessingTime = _cursor.getInt(_cursorIndexOfProcessingTime);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpImages;
            if (_cursor.isNull(_cursorIndexOfImages)) {
              _tmpImages = null;
            } else {
              _tmpImages = _cursor.getString(_cursorIndexOfImages);
            }
            _item = new ChatMessageEntity(_tmpId,_tmpConversationId,_tmpContent,_tmpRole,_tmpAgentName,_tmpConfidence,_tmpSources,_tmpProvider,_tmpProviderColor,_tmpModelUsed,_tmpTokensUsed,_tmpProcessingTime,_tmpCreatedAt,_tmpImageUrl,_tmpImages);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getMessages(final String conversationId,
      final Continuation<? super List<ChatMessageEntity>> $completion) {
    final String _sql = "SELECT * FROM chat_messages WHERE conversationId = ? ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, conversationId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ChatMessageEntity>>() {
      @Override
      @NonNull
      public List<ChatMessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfConversationId = CursorUtil.getColumnIndexOrThrow(_cursor, "conversationId");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfAgentName = CursorUtil.getColumnIndexOrThrow(_cursor, "agentName");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfSources = CursorUtil.getColumnIndexOrThrow(_cursor, "sources");
          final int _cursorIndexOfProvider = CursorUtil.getColumnIndexOrThrow(_cursor, "provider");
          final int _cursorIndexOfProviderColor = CursorUtil.getColumnIndexOrThrow(_cursor, "providerColor");
          final int _cursorIndexOfModelUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "modelUsed");
          final int _cursorIndexOfTokensUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "tokensUsed");
          final int _cursorIndexOfProcessingTime = CursorUtil.getColumnIndexOrThrow(_cursor, "processingTime");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfImages = CursorUtil.getColumnIndexOrThrow(_cursor, "images");
          final List<ChatMessageEntity> _result = new ArrayList<ChatMessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChatMessageEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpConversationId;
            _tmpConversationId = _cursor.getString(_cursorIndexOfConversationId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpAgentName;
            if (_cursor.isNull(_cursorIndexOfAgentName)) {
              _tmpAgentName = null;
            } else {
              _tmpAgentName = _cursor.getString(_cursorIndexOfAgentName);
            }
            final Float _tmpConfidence;
            if (_cursor.isNull(_cursorIndexOfConfidence)) {
              _tmpConfidence = null;
            } else {
              _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            }
            final String _tmpSources;
            if (_cursor.isNull(_cursorIndexOfSources)) {
              _tmpSources = null;
            } else {
              _tmpSources = _cursor.getString(_cursorIndexOfSources);
            }
            final String _tmpProvider;
            if (_cursor.isNull(_cursorIndexOfProvider)) {
              _tmpProvider = null;
            } else {
              _tmpProvider = _cursor.getString(_cursorIndexOfProvider);
            }
            final String _tmpProviderColor;
            if (_cursor.isNull(_cursorIndexOfProviderColor)) {
              _tmpProviderColor = null;
            } else {
              _tmpProviderColor = _cursor.getString(_cursorIndexOfProviderColor);
            }
            final String _tmpModelUsed;
            if (_cursor.isNull(_cursorIndexOfModelUsed)) {
              _tmpModelUsed = null;
            } else {
              _tmpModelUsed = _cursor.getString(_cursorIndexOfModelUsed);
            }
            final Integer _tmpTokensUsed;
            if (_cursor.isNull(_cursorIndexOfTokensUsed)) {
              _tmpTokensUsed = null;
            } else {
              _tmpTokensUsed = _cursor.getInt(_cursorIndexOfTokensUsed);
            }
            final Integer _tmpProcessingTime;
            if (_cursor.isNull(_cursorIndexOfProcessingTime)) {
              _tmpProcessingTime = null;
            } else {
              _tmpProcessingTime = _cursor.getInt(_cursorIndexOfProcessingTime);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpImages;
            if (_cursor.isNull(_cursorIndexOfImages)) {
              _tmpImages = null;
            } else {
              _tmpImages = _cursor.getString(_cursorIndexOfImages);
            }
            _item = new ChatMessageEntity(_tmpId,_tmpConversationId,_tmpContent,_tmpRole,_tmpAgentName,_tmpConfidence,_tmpSources,_tmpProvider,_tmpProviderColor,_tmpModelUsed,_tmpTokensUsed,_tmpProcessingTime,_tmpCreatedAt,_tmpImageUrl,_tmpImages);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
