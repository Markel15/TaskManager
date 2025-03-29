package com.example.proyecto;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class RegisterActivity extends BaseActivity {
    private EditText etUsername, etPassword;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

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
            // Llamar al método que registra el usuario de forma remota
            registerUserRemote(username, password);
        });
    }

    private void registerUserRemote(String username, String password) {
        // Crear un objeto de datos para pasar los parámetros
        Data data = new Data.Builder()
                .putString("username", username)
                .putString("password", password)
                .build();

        // Crear la solicitud de trabajo
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(RegisterWorker.class)
                .setInputData(data)
                .build();

        // Observar el estado de la tarea
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(workRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            // La tarea fue exitosa
                            Toast.makeText(RegisterActivity.this, R.string.register_2, Toast.LENGTH_SHORT).show();
                            finish(); // Vuelve a LoginActivity
                        } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                            // La tarea falló
                            Toast.makeText(RegisterActivity.this, R.string.register_3, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Encolar
        WorkManager.getInstance(this).enqueue(workRequest);
    }
}
