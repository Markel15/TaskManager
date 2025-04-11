package com.example.proyecto;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class EditTareaWorker extends Worker {

    public EditTareaWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Recoger los datos necesarios
        int localId = getInputData().getInt("localId", 0);
        String titulo = getInputData().getString("titulo");
        String descripcion = getInputData().getString("descripcion");
        long fechaCreacion = getInputData().getLong("fechaCreacion", 0);
        long fechaFinalizacion = getInputData().getLong("fechaFinalizacion", 0);
        int prioridad = getInputData().getInt("prioridad", 0);
        int usuarioId = getInputData().getInt("usuarioId", 0);
        String coordenadas = getInputData().getString("coordenadas");

        try {
            // URL del script PHP (la misma o una nueva opción en el mismo archivo)
            URL destino = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/mhernandez141/WEB/tareas.php");
            HttpURLConnection connection = (HttpURLConnection) destino.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Construir la cadena de parámetros para la actualización.
            // Se envía accion=update y se incluye el localId para identificar el registro a actualizar.
            String parametros = "accion=editar"
                    + "&localId=" + localId
                    + "&titulo=" + titulo
                    + "&descripcion=" + descripcion
                    + "&fechaCreacion=" + fechaCreacion
                    + "&fechaFinalizacion=" + fechaFinalizacion
                    + "&prioridad=" + prioridad
                    + "&usuarioId=" + usuarioId
                    + "&coordenadas=" + coordenadas;

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

                // Procesar la respuesta JSON
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.has("success")) {
                    return Result.success();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.failure();
    }

    @Override
    public void onStopped() {
        super.onStopped();
    }
}
