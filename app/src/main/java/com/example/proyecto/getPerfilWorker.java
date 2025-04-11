package com.example.proyecto;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class getPerfilWorker extends Worker {

    public getPerfilWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        int userId = getInputData().getInt("userId", -1);
        if (userId == -1) {
            return Result.failure();
        }

        try {
            URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/mhernandez141/WEB/getProfile.php?userId=" + userId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                if (json.has("error")) {
                    return Result.failure();
                }

                String username = json.getString("username");
                String imagenBase64 = json.getString("imagen");
                byte[] imagenBytes = imagenBase64.isEmpty() ? null : Base64.decode(imagenBase64, Base64.NO_WRAP);

                // Actualizamos la base de datos local (en caso de que un usuario no tenga su imagen de usuario por haber eliminado la aplicación o haber borrado los datos)
                miBD dbHelper = miBD.getMiBD(getApplicationContext());
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("username", username);
                if (imagenBytes != null) {
                    values.put("imagenPerfil", imagenBytes);
                }
                int rows = db.update("usuarios", values, "id = ?", new String[]{String.valueOf(userId)});
                if (rows > 0) {
                    // devolver el username para actualizar la vista
                    return Result.success(new androidx.work.Data.Builder().putString("username", username).build());
                }
            }
        } catch (Exception e) {
            Log.e("MyAppTag", "Error al obtener el perfil", e);
        }
        return Result.failure();
    }
}
