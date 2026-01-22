package com.health.companion.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.health.companion.data.local.database.DocumentEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class DocumentDao_Impl implements DocumentDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DocumentEntity> __insertionAdapterOfDocumentEntity;

  private final EntityDeletionOrUpdateAdapter<DocumentEntity> __updateAdapterOfDocumentEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateExtractedText;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public DocumentDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDocumentEntity = new EntityInsertionAdapter<DocumentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `documents` (`id`,`filename`,`documentType`,`status`,`extractedText`,`filePath`,`uploadedAt`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DocumentEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getFilename());
        statement.bindString(3, entity.getDocumentType());
        statement.bindString(4, entity.getStatus());
        if (entity.getExtractedText() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getExtractedText());
        }
        statement.bindString(6, entity.getFilePath());
        statement.bindLong(7, entity.getUploadedAt());
      }
    };
    this.__updateAdapterOfDocumentEntity = new EntityDeletionOrUpdateAdapter<DocumentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `documents` SET `id` = ?,`filename` = ?,`documentType` = ?,`status` = ?,`extractedText` = ?,`filePath` = ?,`uploadedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DocumentEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getFilename());
        statement.bindString(3, entity.getDocumentType());
        statement.bindString(4, entity.getStatus());
        if (entity.getExtractedText() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getExtractedText());
        }
        statement.bindString(6, entity.getFilePath());
        statement.bindLong(7, entity.getUploadedAt());
        statement.bindString(8, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE documents SET status = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateExtractedText = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE documents SET extractedText = ?, status = 'processed' WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM documents WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final DocumentEntity document,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDocumentEntity.insert(document);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final DocumentEntity document,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDocumentEntity.handle(document);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateStatus(final String documentId, final String status,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindString(_argIndex, documentId);
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
          __preparedStmtOfUpdateStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateExtractedText(final String documentId, final String text,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateExtractedText.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, text);
        _argIndex = 2;
        _stmt.bindString(_argIndex, documentId);
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
          __preparedStmtOfUpdateExtractedText.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String documentId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, documentId);
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
  public Flow<List<DocumentEntity>> getAllDocumentsFlow() {
    final String _sql = "SELECT * FROM documents ORDER BY uploadedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"documents"}, new Callable<List<DocumentEntity>>() {
      @Override
      @NonNull
      public List<DocumentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFilename = CursorUtil.getColumnIndexOrThrow(_cursor, "filename");
          final int _cursorIndexOfDocumentType = CursorUtil.getColumnIndexOrThrow(_cursor, "documentType");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfExtractedText = CursorUtil.getColumnIndexOrThrow(_cursor, "extractedText");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfUploadedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "uploadedAt");
          final List<DocumentEntity> _result = new ArrayList<DocumentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DocumentEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpFilename;
            _tmpFilename = _cursor.getString(_cursorIndexOfFilename);
            final String _tmpDocumentType;
            _tmpDocumentType = _cursor.getString(_cursorIndexOfDocumentType);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpExtractedText;
            if (_cursor.isNull(_cursorIndexOfExtractedText)) {
              _tmpExtractedText = null;
            } else {
              _tmpExtractedText = _cursor.getString(_cursorIndexOfExtractedText);
            }
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final long _tmpUploadedAt;
            _tmpUploadedAt = _cursor.getLong(_cursorIndexOfUploadedAt);
            _item = new DocumentEntity(_tmpId,_tmpFilename,_tmpDocumentType,_tmpStatus,_tmpExtractedText,_tmpFilePath,_tmpUploadedAt);
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
  public Object getAllDocuments(final Continuation<? super List<DocumentEntity>> $completion) {
    final String _sql = "SELECT * FROM documents ORDER BY uploadedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DocumentEntity>>() {
      @Override
      @NonNull
      public List<DocumentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFilename = CursorUtil.getColumnIndexOrThrow(_cursor, "filename");
          final int _cursorIndexOfDocumentType = CursorUtil.getColumnIndexOrThrow(_cursor, "documentType");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfExtractedText = CursorUtil.getColumnIndexOrThrow(_cursor, "extractedText");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfUploadedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "uploadedAt");
          final List<DocumentEntity> _result = new ArrayList<DocumentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DocumentEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpFilename;
            _tmpFilename = _cursor.getString(_cursorIndexOfFilename);
            final String _tmpDocumentType;
            _tmpDocumentType = _cursor.getString(_cursorIndexOfDocumentType);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpExtractedText;
            if (_cursor.isNull(_cursorIndexOfExtractedText)) {
              _tmpExtractedText = null;
            } else {
              _tmpExtractedText = _cursor.getString(_cursorIndexOfExtractedText);
            }
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final long _tmpUploadedAt;
            _tmpUploadedAt = _cursor.getLong(_cursorIndexOfUploadedAt);
            _item = new DocumentEntity(_tmpId,_tmpFilename,_tmpDocumentType,_tmpStatus,_tmpExtractedText,_tmpFilePath,_tmpUploadedAt);
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

  @Override
  public Object getDocumentById(final String documentId,
      final Continuation<? super DocumentEntity> $completion) {
    final String _sql = "SELECT * FROM documents WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, documentId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DocumentEntity>() {
      @Override
      @Nullable
      public DocumentEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFilename = CursorUtil.getColumnIndexOrThrow(_cursor, "filename");
          final int _cursorIndexOfDocumentType = CursorUtil.getColumnIndexOrThrow(_cursor, "documentType");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfExtractedText = CursorUtil.getColumnIndexOrThrow(_cursor, "extractedText");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfUploadedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "uploadedAt");
          final DocumentEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpFilename;
            _tmpFilename = _cursor.getString(_cursorIndexOfFilename);
            final String _tmpDocumentType;
            _tmpDocumentType = _cursor.getString(_cursorIndexOfDocumentType);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpExtractedText;
            if (_cursor.isNull(_cursorIndexOfExtractedText)) {
              _tmpExtractedText = null;
            } else {
              _tmpExtractedText = _cursor.getString(_cursorIndexOfExtractedText);
            }
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final long _tmpUploadedAt;
            _tmpUploadedAt = _cursor.getLong(_cursorIndexOfUploadedAt);
            _result = new DocumentEntity(_tmpId,_tmpFilename,_tmpDocumentType,_tmpStatus,_tmpExtractedText,_tmpFilePath,_tmpUploadedAt);
          } else {
            _result = null;
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
