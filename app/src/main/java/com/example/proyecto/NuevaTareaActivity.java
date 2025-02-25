package com.example.proyecto;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class NuevaTareaActivity extends AppCompatActivity {
    private EditText etTitulo, etDescripcion, etFechaFinalizacion;
    private Spinner spPrioridad;
    private Button btnGuardar;
    private miBD miDb;

    private long fechaFinalSeleccionada = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nueva_tarea);

        miDb = miBD.getMiBD(this);

        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etFechaFinalizacion = findViewById(R.id.etFechaFinalizacion);
        spPrioridad = findViewById(R.id.spPrioridad);
        btnGuardar = findViewById(R.id.btnGuardar);

        etFechaFinalizacion.setFocusable(false);  // Para que no abra el teclado
        etFechaFinalizacion.setOnClickListener(v -> {
            // Obtener la fecha actual
            Calendar calendar = Calendar.getInstance();
            int año = calendar.get(Calendar.YEAR);
            int mes = calendar.get(Calendar.MONTH);
            int dia = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    NuevaTareaActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        calendar.set(selectedYear, selectedMonth, selectedDay);
                        fechaFinalSeleccionada = calendar.getTimeInMillis();

                        // Formatear la fecha y mostrarla en el EditText
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        etFechaFinalizacion.setText(sdf.format(calendar.getTime()));
                    },
                    año, mes, dia);
            datePickerDialog.show();
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.prioridad_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPrioridad.setAdapter(adapter);

        btnGuardar.setOnClickListener(v -> guardarTarea());
    }

    private void guardarTarea() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();

        if(titulo.isEmpty()) {
            Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        if(fechaFinalSeleccionada == 0) {
            Toast.makeText(this, "Debes seleccionar una fecha final", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener la prioridad según la posición seleccionada (0 = baja, 1 = media, 2 = alta)
        int prioridad = spPrioridad.getSelectedItemPosition();

        // Recuperar el ID del usuario desde las SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("idDeUsuario", -1);
        if(userId == -1) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear la tarea
        Tarea tarea = new Tarea();
        tarea.setTitulo(titulo);
        tarea.setDescripcion(descripcion);
        tarea.setUsuId(userId);
        tarea.setFechaCreacion(System.currentTimeMillis());
        tarea.setFechaFinalizacion(fechaFinalSeleccionada);
        tarea.setPrioridad(prioridad);
        tarea.setCompletado(false);

        // Insertar la nueva tarea en la base de datos
        SQLiteDatabase db = miDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("titulo", tarea.getTitulo());
        values.put("descripcion", tarea.getDescripcion());
        values.put("fechaCreacion", tarea.getFechaCreacion());
        values.put("FechaFinalizacion", tarea.getFechaFinalizacion());
        values.put("completado", tarea.isCompletado() ? 1 : 0);
        values.put("prioridad", tarea.getPrioridad());
        values.put("usuarioId", tarea.getUsuId());

        long newRowId = db.insert("tareas", null, values);
        db.close();

        if(newRowId != -1) {
            Toast.makeText(this, "Tarea añadida", Toast.LENGTH_SHORT).show();
            finish(); // Regresa a la MainActivity
        } else {
            Toast.makeText(this, "Error al añadir la tarea", Toast.LENGTH_SHORT).show();
        }
    }

}
