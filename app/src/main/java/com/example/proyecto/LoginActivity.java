package com.example.proyecto;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.mindrot.jbcrypt.BCrypt;

public class LoginActivity extends BaseActivity {
    private EditText etUsername, etPassword;  // editTexts
    private Button btnLogin, btnRegister;

    miBD miDb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        miDb = miBD.getMiBD(this); // Se mueve aquí porque sino no tiene acceso a this
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
            int usuId = loginUser(usuario, contraseña);
            if (usuId!=-1) {
                // Guardar el ID en SharedPreferences
                SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
                prefs.edit().putInt("idDeUsuario", usuId).apply();

                // Usuario autenticado: inicia MainActivity
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, R.string.credenciales_incorrectas, Toast.LENGTH_SHORT).show();
            }
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            // No se utiliza finish porque el usuario va a volver a esta pestaña al registrarse
        });
    }

    private int loginUser(String username, String password) {
        SQLiteDatabase bd = miDb.getReadableDatabase();
        Cursor cursor = bd.rawQuery("SELECT * FROM usuarios WHERE username = ?", new String[]{username});
        int usuId = -1;  // Valor por defecto en caso de fallo
        if (cursor.moveToFirst()) {
            int indexPassword = cursor.getColumnIndex("password");
            if (indexPassword != -1) {
                // Obtener el hash de la contraseña guardada
                String hashGuardado = cursor.getString(indexPassword);
                if (BCrypt.checkpw(password, hashGuardado)) {
                    // Extraer el id del usuario
                    int indexId = cursor.getColumnIndex("id");
                    if (indexId != -1) {
                        usuId = cursor.getInt(indexId);  // Obtener el id de usuario que ha iniciado sesión
                    }
                }
            } else {
                Log.e("Database Error", "La columna 'password' no existe en la tabla 'usuarios'.");
            }
        }
        cursor.close();
        bd.close();
        return usuId;  // Credenciales incorrectas
    }

}
