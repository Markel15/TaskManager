package com.example.proyecto;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;  // editTexts
    private Button btnLogin, btnRegister;

    miBD miDb = miBD.getMiBD(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (loginUser(username, password)) {
                // Usuario autenticado: inicia la MainActivity
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean loginUser(String username, String password) {
        SQLiteDatabase db = miDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM usuarios WHERE username = ? AND password = ?",
                new String[]{username, password});
        boolean resultado = cursor.moveToFirst();
        cursor.close();
        return resultado;
    }

}
