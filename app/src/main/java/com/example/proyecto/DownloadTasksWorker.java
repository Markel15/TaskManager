package com.example.proyecto;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.Data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadTasksWorker extends Worker {

    public DownloadTasksWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        int usuarioId = getInputData().getInt("usuarioId", -1);
        if (usuarioId == -1) {
            return Result.failure();
        }

        try {
            URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/mhernandez141/WEB/downloadTasks.php");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Enviar el usuarioId
            String parametros = "usuarioId=" + usuarioId;
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            out.print(parametros);
            out.close();

            int statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.has("success")) {
                    JSONArray tareasArray = jsonResponse.getJSONArray("tareas");

                    // Abrir la base de datos local y actualizar la tabla de tareas
                    miBD dbHelper = miBD.getMiBD(getApplicationContext());
                    SQLiteDatabase db = dbHelper.getWritableDatabase();

                    // Eliminar todas las tareas del usuario para evitar duplicados
                    db.delete("tareas", "usuarioId=?", new String[]{String.valueOf(usuarioId)});

                    for (int i = 0; i < tareasArray.length(); i++) {
                        JSONObject tareaJson = tareasArray.getJSONObject(i);
                        ContentValues cv = new ContentValues();
                        // Supone que "localId" es el valor que vamos a usar en la BD local
                        cv.put("id", tareaJson.getInt("localId"));
                        cv.put("titulo", tareaJson.getString("titulo"));
                        cv.put("descripcion", tareaJson.getString("descripcion"));
                        cv.put("fechaCreacion", tareaJson.getLong("fechaCreacion"));
                        cv.put("FechaFinalizacion", tareaJson.getLong("FechaFinalizacion"));
                        cv.put("completado", tareaJson.getInt("completado"));
                        cv.put("prioridad", tareaJson.getInt("prioridad"));
                        cv.put("usuarioId", tareaJson.getInt("usuarioId"));
                        cv.put("localizacion", tareaJson.getString("localizacion"));

                        // Insertar la tarea en la base local
                        db.insert("tareas", null, cv);
                    }
                    db.close();

                    return Result.success();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("DownloadTasksWorker", "Error: " + e.getMessage());
            return Result.failure();
        }
        return Result.failure();
    }
}
