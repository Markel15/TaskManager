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

public class RegisterWorker extends Worker {

    public RegisterWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String username = getInputData().getString("username");
        String password = getInputData().getString("password");

        try {
            // Código adaptado del tema 14 de eGela,
            URL destino = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/mhernandez141/WEB/register_login.php");
            HttpURLConnection connection = (HttpURLConnection) destino.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Usamos PrintWriter para crear el cuerpo de la solicitud
            String parametros = "accion=register&username=" + username + "&password=" + password;
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            out.print(parametros);
            out.close();

            // Recoger la respuesta
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
                    int userId = jsonResponse.getInt("id");
                    Data output = new Data.Builder().putInt("id", userId).build();
                    return Result.success(output);
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
