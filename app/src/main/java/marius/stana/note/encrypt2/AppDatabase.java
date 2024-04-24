package marius.stana.note.encrypt2;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
@Database(entities = {Note.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract NoteDao getNoteDao();
}
