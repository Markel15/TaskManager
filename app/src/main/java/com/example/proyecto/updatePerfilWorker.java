package com.example.proyecto;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class updatePerfilWorker extends Worker {

    public updatePerfilWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        int userId = getInputData().getInt("userId", -1);
        String imagePath = getInputData().getString("imagePath");
        if (userId == -1 || imagePath == null) {
            return Result.failure();
        }

        try {
            // Leer imagen desde archivo y convertir a Base64 eliminando algún metadato si lo hubiese
            Bitmap originalBitmap = BitmapFactory.decodeFile(imagePath);
            int maxSize = 800; // tamaño máximo para ancho o alto

            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();
            float ratio = (float) width / (float) height;

            if (width > height) {
                width = maxSize;
                height = (int) (width / ratio);
            } else {
                height = maxSize;
                width = (int) (height * ratio);
            }

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true);

            // Comprimir a un ByteArrayOutputStream para convertir a base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // Usar menor calidad para reducir tamaño
            byte[] imageBytes = baos.toByteArray();

            String imagenBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            URL destino = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/mhernandez141/WEB/updateProfile.php");
            HttpURLConnection connection = (HttpURLConnection) destino.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Solo mandamos userId e imagen
            String parametros = "accion=updateProfileImage"
                    + "&userId=" + userId
                    + "&imagen=" + imagenBase64;

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

                JSONObject json = new JSONObject(response.toString());
                if (json.has("success")) {
                    // procedemos a actualizar la base de datos local.
                    miBD dbHelper = miBD.getMiBD(getApplicationContext());
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put("imagenPerfil", imageBytes);  // La columna es de tipo BLOB
                    int rows = db.update("usuarios", values, "id = ?", new String[]{String.valueOf(userId)});
                    if (rows > 0) {
                        return Result.success();
                    } else {
                        return Result.failure();
                    }
                } else {
                    return Result.failure();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.failure();
    }
}

