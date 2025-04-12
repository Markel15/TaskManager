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

public class SyncTareaWorker extends Worker {

    public SyncTareaWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Recoger el parámetro de acción: "completar", "eliminar", "editar" o "crear" si se necesitase
        String accion = getInputData().getString("accion");
        int localId = getInputData().getInt("localId", 0);

        try {
            URL destino = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/mhernandez141/WEB/tareas.php");
            HttpURLConnection connection = (HttpURLConnection) destino.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Comenzamos a construir la cadena de parámetros comunes (acción y localId)
            String parametros = "accion=" + accion + "&localId=" + localId;

            // Para estar preparado para todas las acciones, se añaden todos los datos posibles
            if ("editar".equals(accion) || "crear".equals(accion) || "eliminar".equals(accion) || "completar".equals(accion)) {
                String titulo = getInputData().getString("titulo");
                String descripcion = getInputData().getString("descripcion");
                long fechaCreacion = getInputData().getLong("fechaCreacion", 0);
                long fechaFinalizacion = getInputData().getLong("fechaFinalizacion", 0);
                int completado = getInputData().getInt("completado", 0);
                int prioridad = getInputData().getInt("prioridad", 0);
                int usuarioId = getInputData().getInt("usuarioId", 0);
                String coordenadas = getInputData().getString("coordenadas");

                parametros += "&titulo=" + titulo
                        + "&descripcion=" + descripcion
                        + "&fechaCreacion=" + fechaCreacion
                        + "&fechaFinalizacion=" + fechaFinalizacion
                        + "&completado=" + completado
                        + "&prioridad=" + prioridad
                        + "&usuarioId=" + usuarioId
                        + "&coordenadas=" + (coordenadas != null ? coordenadas : "");
            }

            // Enviar la solicitud POST
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            out.print(parametros);
            out.close();

            int statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.has("success")) {
                    if( jsonResponse.getString("accion").equals("crear")) {
                        int tareaId = jsonResponse.getInt("id");
                        Data output = new Data.Builder().putInt("id", tareaId).build();
                        return Result.success(output);
                    }
                    else{
                        return Result.success();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
        return Result.failure();
    }
}
