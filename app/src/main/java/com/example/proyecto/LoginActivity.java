package com.example.proyecto;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.mindrot.jbcrypt.BCrypt;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;  // editTexts
    private Button btnLogin, btnRegister;

    miBD miDb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        miDb = miBD.getMiBD(this); // Se mueve aqí porque sino no tiene acceso a this
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (loginUser(username, password)) {
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

    private boolean loginUser(String username, String password) {
        SQLiteDatabase bd = miDb.getReadableDatabase();
        Cursor cursor = bd.rawQuery("SELECT * FROM usuarios WHERE username = ?", new String[]{username});
        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex("password");
            if (columnIndex != -1) {
                // Obtener el hash de la contraseña guardada
                String hashGuardado = cursor.getString(columnIndex);
                cursor.close();
                bd.close();

                // Verificar la contraseña con el hash guardado
                if (BCrypt.checkpw(password, hashGuardado)) {
                    return true;
                }
            } else {
                Log.e("Database Error", "La columna 'password' no existe en la tabla 'usuarios'.");
            }
        }
        cursor.close();
        bd.close();
        return false;  // Credenciales incorrectas
    }

}
