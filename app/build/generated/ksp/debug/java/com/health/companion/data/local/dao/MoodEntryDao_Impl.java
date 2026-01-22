package com.health.companion.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.health.companion.data.local.database.MoodEntryEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
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
public final class MoodEntryDao_Impl implements MoodEntryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MoodEntryEntity> __insertionAdapterOfMoodEntryEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public MoodEntryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMoodEntryEntity = new EntityInsertionAdapter<MoodEntryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `mood_entries` (`id`,`moodLevel`,`symptoms`,`journalText`,`stressLevel`,`createdAt`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MoodEntryEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getMoodLevel());
        statement.bindString(3, entity.getSymptoms());
        statement.bindString(4, entity.getJournalText());
        statement.bindLong(5, entity.getStressLevel());
        statement.bindLong(6, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM mood_entries WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final MoodEntryEntity entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMoodEntryEntity.insert(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String entryId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, entryId);
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
  public Flow<List<MoodEntryEntity>> getRecentEntriesFlow(final int limit) {
    final String _sql = "SELECT * FROM mood_entries ORDER BY createdAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"mood_entries"}, new Callable<List<MoodEntryEntity>>() {
      @Override
      @NonNull
      public List<MoodEntryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMoodLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "moodLevel");
          final int _cursorIndexOfSymptoms = CursorUtil.getColumnIndexOrThrow(_cursor, "symptoms");
          final int _cursorIndexOfJournalText = CursorUtil.getColumnIndexOrThrow(_cursor, "journalText");
          final int _cursorIndexOfStressLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "stressLevel");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<MoodEntryEntity> _result = new ArrayList<MoodEntryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MoodEntryEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpMoodLevel;
            _tmpMoodLevel = _cursor.getInt(_cursorIndexOfMoodLevel);
            final String _tmpSymptoms;
            _tmpSymptoms = _cursor.getString(_cursorIndexOfSymptoms);
            final String _tmpJournalText;
            _tmpJournalText = _cursor.getString(_cursorIndexOfJournalText);
            final int _tmpStressLevel;
            _tmpStressLevel = _cursor.getInt(_cursorIndexOfStressLevel);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MoodEntryEntity(_tmpId,_tmpMoodLevel,_tmpSymptoms,_tmpJournalText,_tmpStressLevel,_tmpCreatedAt);
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
  public Object getRecentEntries(final int limit,
      final Continuation<? super List<MoodEntryEntity>> $completion) {
    final String _sql = "SELECT * FROM mood_entries ORDER BY createdAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MoodEntryEntity>>() {
      @Override
      @NonNull
      public List<MoodEntryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMoodLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "moodLevel");
          final int _cursorIndexOfSymptoms = CursorUtil.getColumnIndexOrThrow(_cursor, "symptoms");
          final int _cursorIndexOfJournalText = CursorUtil.getColumnIndexOrThrow(_cursor, "journalText");
          final int _cursorIndexOfStressLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "stressLevel");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<MoodEntryEntity> _result = new ArrayList<MoodEntryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MoodEntryEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpMoodLevel;
            _tmpMoodLevel = _cursor.getInt(_cursorIndexOfMoodLevel);
            final String _tmpSymptoms;
            _tmpSymptoms = _cursor.getString(_cursorIndexOfSymptoms);
            final String _tmpJournalText;
            _tmpJournalText = _cursor.getString(_cursorIndexOfJournalText);
            final int _tmpStressLevel;
            _tmpStressLevel = _cursor.getInt(_cursorIndexOfStressLevel);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MoodEntryEntity(_tmpId,_tmpMoodLevel,_tmpSymptoms,_tmpJournalText,_tmpStressLevel,_tmpCreatedAt);
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
  public Object getEntriesInRange(final long startTime, final long endTime,
      final Continuation<? super List<MoodEntryEntity>> $completion) {
    final String _sql = "SELECT * FROM mood_entries WHERE createdAt BETWEEN ? AND ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startTime);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endTime);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MoodEntryEntity>>() {
      @Override
      @NonNull
      public List<MoodEntryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMoodLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "moodLevel");
          final int _cursorIndexOfSymptoms = CursorUtil.getColumnIndexOrThrow(_cursor, "symptoms");
          final int _cursorIndexOfJournalText = CursorUtil.getColumnIndexOrThrow(_cursor, "journalText");
          final int _cursorIndexOfStressLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "stressLevel");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<MoodEntryEntity> _result = new ArrayList<MoodEntryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MoodEntryEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpMoodLevel;
            _tmpMoodLevel = _cursor.getInt(_cursorIndexOfMoodLevel);
            final String _tmpSymptoms;
            _tmpSymptoms = _cursor.getString(_cursorIndexOfSymptoms);
            final String _tmpJournalText;
            _tmpJournalText = _cursor.getString(_cursorIndexOfJournalText);
            final int _tmpStressLevel;
            _tmpStressLevel = _cursor.getInt(_cursorIndexOfStressLevel);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MoodEntryEntity(_tmpId,_tmpMoodLevel,_tmpSymptoms,_tmpJournalText,_tmpStressLevel,_tmpCreatedAt);
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
  public Object getAverageMoodSince(final long sinceTime,
      final Continuation<? super Float> $completion) {
    final String _sql = "SELECT AVG(moodLevel) FROM mood_entries WHERE createdAt > ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, sinceTime);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Float>() {
      @Override
      @Nullable
      public Float call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Float _result;
          if (_cursor.moveToFirst()) {
            final Float _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getFloat(0);
            }
            _result = _tmp;
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
