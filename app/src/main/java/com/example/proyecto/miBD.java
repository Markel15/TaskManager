package com.example.proyecto;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class miBD extends SQLiteOpenHelper {

    private static final String NOMBRE = "tareas.db";
    private static final int VERSION = 1;

    private static miBD miBD = null;

    private miBD(@Nullable Context context) {
        super(context, NOMBRE, null, VERSION);
    }

    public static miBD getMiBD(Context context) {
        if (miBD == null) {
            miBD = new miBD(context.getApplicationContext());
        }
        return miBD;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String comando1 = "CREATE TABLE tareas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "titulo TEXT NOT NULL, " +
                "descripcion TEXT, " +
                "fechaCreacion INTEGER, " +
                "FechaFinalizacion INTEGER, " +
                "completeado INTEGER DEFAULT 0, " +
                "prioridad INTEGER DEFAULT 0," +
                "usuarioId INTEGER NOT NULL," +
                "FOREIGN KEY (id) REFERENCES usuarios(id) ON DELETE CASCADE"+
                ");";
        String comando2 = "CREATE TABLE usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL" +
                ");";
        db.execSQL(comando1);
        db.execSQL(comando2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS tareas");
        onCreate(db);
    }
}
