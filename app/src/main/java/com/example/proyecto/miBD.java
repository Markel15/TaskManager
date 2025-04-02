package com.example.proyecto;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class miBD extends SQLiteOpenHelper {

    private static final String NOMBRE = "tareas.db";
    private static final int VERSION = 2;

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
                "completado INTEGER DEFAULT 0, " +
                "prioridad INTEGER DEFAULT 0," +
                "usuarioId INTEGER NOT NULL," +
                "localizacion TEXT, " +
                "FOREIGN KEY (usuarioId) REFERENCES usuarios(id) ON DELETE CASCADE"+
                ");";
        String comando2 = "CREATE TABLE usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL, " +
                "imagenPerfil BLOB" +
                ");";
        db.execSQL(comando1);
        db.execSQL(comando2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) { //Añadir el nuevo campo en caso de actualización de la aplicación
            db.execSQL("ALTER TABLE tareas ADD COLUMN localizacion TEXT;");
        }
        else {
            db.execSQL("DROP TABLE IF EXISTS tareas");
            onCreate(db);
        }
    }
}
