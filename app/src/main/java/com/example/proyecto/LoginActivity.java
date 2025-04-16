package com.example.proyecto;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.mindrot.jbcrypt.BCrypt;


public class LoginActivity extends BaseActivity {
    private EditText etUsername, etPassword;  // editTexts
    private Button btnLogin, btnRegister;

    miBD miDb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        miDb = miBD.getMiBD(this);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // Comprobar si el usuario tiene la sesión iniciada para no tener que logearse
        SharedPreferences preferencias = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        int userId = preferencias.getInt("idDeUsuario", -1);
        if (userId != -1) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        btnLogin.setOnClickListener(v -> {
            String usuario = etUsername.getText().toString().trim();
            String contraseña = etPassword.getText().toString().trim();

            if (usuario.isEmpty() || contraseña.isEmpty()) {
                Toast.makeText(this, R.string.credenciales_incorrectas, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!hayInternet()) {
                Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                return;
            }
            // Llamar al método que realiza el login remoto
            loginUserRemote(usuario, contraseña);
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            // No se utiliza finish porque el usuario va a volver a esta pestaña al registrarse
        });
    }

    private void loginUserRemote(String username, String password) {
        // Crear un objeto Data para enviar los parámetros al worker
        Data data = new Data.Builder()
                .putString("username", username)
                .putString("password", password)
                .build();

        // Crear la solicitud del worker
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(LoginWorker.class)
                .setInputData(data)
                .build();

        // Observar el estado del worker
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(workRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            // Obtener el id del usuario desde el Data resultante
                            int userId = workInfo.getOutputData().getInt("userId", -1);
                            if (userId != -1) {
                                if(registerUserLocal(userId, username, password)){
                                    // Guardar el id del usuario en SharedPreferences
                                    SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
                                    prefs.edit().putInt("idDeUsuario", userId).apply();

                                    // Usuario autenticado: inicia MainActivity
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                }
                                else {
                                    Toast.makeText(LoginActivity.this, R.string.err_bd_local, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, R.string.err_obten_usu, Toast.LENGTH_SHORT).show();
                            }
                        } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                            Toast.makeText(LoginActivity.this, R.string.credenciales_incorrectas, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Encolar el worker
        WorkManager.getInstance(this).enqueue(workRequest);
    }

    private boolean hayInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                return capabilities != null && (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        }
        return false;
    }

    // Para registrar al usuario en la base de datos local en caso de que sea la primera vez que inicia sesión en este dispositivo.
    private boolean registerUserLocal(int userId, String username, String password) {
        SQLiteDatabase bd = miDb.getWritableDatabase();

        // Verificar si el usuario ya existe
        Cursor c = bd.rawQuery("SELECT * FROM usuarios WHERE id = ?", new String[]{String.valueOf(userId)});
        boolean usuarioExiste = c.moveToFirst();
        c.close();

        if (usuarioExiste) {
            bd.close();
            return true; // Usuario ya existe
        }

        // Cifrar la contraseña antes de almacenarla
        String sal = BCrypt.gensalt();
        String hashedPassword = BCrypt.hashpw(password, sal);

        ContentValues values = new ContentValues();
        values.put("id", userId);
        values.put("username", username);
        values.put("password", hashedPassword);

        long result = bd.insert("usuarios", null, values);
        bd.close();
        return result != -1; // Devuelve true si la inserción fue exitosa
    }

}
