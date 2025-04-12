package com.example.proyecto;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

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
        int syncId = getInputData().getInt("syncId", 0); // Si no es 0 es porque se está intentando hacer una sincronizacion con la base de datos

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
                    borrarSincronizacionLocal(syncId);  // Por si se está usando el worker para sincronizar con el servidor, se borra de la base de datos local si se ha sincornizado con exito
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
            guardarSincronizcaionEnLocal();
            return Result.failure();
        }
        // Guardar cambios en base de datos para futuros intentos de sincronizaciones cuando vuelva a haber conexion
        guardarSincronizcaionEnLocal();
        return Result.failure();
    }
    private void guardarSincronizcaionEnLocal() {
        // Recoger todos los parámetros del InputData
        Data data = getInputData();
        int localId = data.getInt("localId", 0);
        String accion = data.getString("accion");
        String titulo = data.getString("titulo");
        String descripcion = data.getString("descripcion");
        long fechaCreacion = data.getLong("fechaCreacion", 0);
        long fechaFinalizacion = data.getLong("fechaFinalizacion", 0);
        int completado = data.getInt("completado", 0);
        int prioridad = data.getInt("prioridad", 0);
        int usuarioId = data.getInt("usuarioId", 0);
        String coordenadas = data.getString("coordenadas");

        // Abrir la base de datos local
        miBD dbHelper = miBD.getMiBD(getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("localId", localId);
        cv.put("accion", accion);
        cv.put("titulo", titulo);
        cv.put("descripcion", descripcion);
        cv.put("fechaCreacion", fechaCreacion);
        cv.put("FechaFinalizacion", fechaFinalizacion);
        cv.put("completado", completado);
        cv.put("prioridad", prioridad);
        cv.put("usuarioId", usuarioId);
        cv.put("localizacion", coordenadas);

        long code = db.insert("sync_operations", null, cv);
        Log.d("SyncTareaWorker",String.valueOf(code));
        db.close();
    }
    private void borrarSincronizacionLocal(int syncId) {
        miBD dbHelper = miBD.getMiBD(getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("sync_operations", "id=?", new String[]{String.valueOf(syncId)});
        db.close();
    }
}
