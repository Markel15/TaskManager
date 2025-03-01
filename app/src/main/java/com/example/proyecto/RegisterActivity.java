package com.example.proyecto;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.mindrot.jbcrypt.BCrypt;

public class RegisterActivity extends BaseActivity {
    private EditText etUsername, etPassword;
    private Button btnRegister;
    miBD miDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        miDb = miBD.getMiBD(this);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.register_1, Toast.LENGTH_SHORT).show();
                return;
            }
            if (registerUser(username, password)) {
                Toast.makeText(this, R.string.register_2, Toast.LENGTH_SHORT).show();
                finish(); // Vuelve a LoginActivity
            } else {
                Toast.makeText(this, R.string.register_3, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean registerUser(String username, String password) {
        SQLiteDatabase bd = miDb.getWritableDatabase();
        // Comprobar si el usuario ya existe
        Cursor c = bd.rawQuery("SELECT * FROM usuarios WHERE username = ?", new String[]{username});
        if (c.moveToFirst()) {
            c.close();
            return false; // Usuario existente
        }
        c.close();
        // Como el cursor devuelve false si no se ha encontrado el usuario seguiría por aquí para crearlo

        // Cifrar la contraseña antes de almacenarla
        String sal = BCrypt.gensalt();
        String hashedPassword = BCrypt.hashpw(password, sal);
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", hashedPassword);
        long result = bd.insert("usuarios", null, values);
        bd.close();
        return result != -1; // Devuelve true si no han habido errores, sino, insert devuelve -1 y esta comprobación devolvería false
    }
}

