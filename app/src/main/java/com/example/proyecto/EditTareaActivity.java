package com.example.proyecto;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditTareaActivity extends BaseActivity implements OnFechaSelectedListener {
    private EditText etTitulo, etDescripcion, etFechaFinalizacion;
    private Spinner spPrioridad;
    private Button btnGuardar;
    private miBD miDb;
    private long fechaFinalSeleccionada = 0;
    private int tareaId;
    private long fechaCreacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_tarea);

        miDb = miBD.getMiBD(this);

        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etFechaFinalizacion = findViewById(R.id.etFechaFinalizacion);
        spPrioridad = findViewById(R.id.spPrioridad);
        btnGuardar = findViewById(R.id.btnGuardar);

        // Recuperar el id de la tarea desde los extras
        tareaId = getIntent().getIntExtra("tarea_id", -1);
        if (tareaId == -1) {
            Toast.makeText(this, R.string.err_tarea_no_encontrada, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Cargar los datos de la tarea
        loadTaskData(tareaId);

        // Configurar el EditText para lanzar el DatePickerDialog personalizado
        etFechaFinalizacion.setFocusable(false);
        etFechaFinalizacion.setOnClickListener(v -> {
            ClaseDialogoFecha dialogoFecha = new ClaseDialogoFecha();
            dialogoFecha.show(getSupportFragmentManager(), "datePicker");
        });

        // Configurar el Spinner con el array de prioridades
        ArrayAdapter<CharSequence> adapterSpinner = ArrayAdapter.createFromResource(this,
                R.array.prioridad_array, android.R.layout.simple_spinner_item);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPrioridad.setAdapter(adapterSpinner);

        btnGuardar.setOnClickListener(v -> updateTask());
    }

    // Método para cargar los datos de la tarea desde la base de datos
    @SuppressLint("Range")
    private void loadTaskData(int tareaId) {
        SQLiteDatabase db = miDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM tareas WHERE id = ?", new String[]{String.valueOf(tareaId)});
        if (cursor.moveToFirst()) {
            String titulo = cursor.getString(cursor.getColumnIndex("titulo"));
            String descripcion = cursor.getString(cursor.getColumnIndex("descripcion"));
            fechaCreacion = cursor.getLong(cursor.getColumnIndex("fechaCreacion"));
            long fechaFinal = cursor.getLong(cursor.getColumnIndex("FechaFinalizacion"));
            int prioridad = cursor.getInt(cursor.getColumnIndex("prioridad"));
            // Rellenar los campos
            etTitulo.setText(titulo);
            etDescripcion.setText(descripcion);
            fechaFinalSeleccionada = fechaFinal;
            // Mostrar la fecha en formato dd/MM/yyyy
            Calendar calendario = Calendar.getInstance();
            calendario.setTimeInMillis(fechaFinal);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etFechaFinalizacion.setText(sdf.format(calendario.getTime()));
            spPrioridad.setSelection(prioridad);
        } else {
            Toast.makeText(this, R.string.err_tarea_no_encontrada, Toast.LENGTH_SHORT).show();
            finish();
        }
        cursor.close();
        db.close();
    }

    @Override
    public void onFechaSeleccionada(long fecha) {
        fechaFinalSeleccionada = fecha;
        Calendar calendario = Calendar.getInstance();
        calendario.setTimeInMillis(fecha);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etFechaFinalizacion.setText(sdf.format(calendario.getTime()));
    }

    // Método para actualizar la tarea en la base de datos
    private void updateTask() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();

        if (titulo.isEmpty()) {
            Toast.makeText(this, R.string.titulo_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (fechaFinalSeleccionada == 0) {
            Toast.makeText(this, R.string.fecha_required, Toast.LENGTH_SHORT).show();
            return;
        }

        int prioridad = spPrioridad.getSelectedItemPosition();
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("idDeUsuario", -1);
        if (userId == -1) {
            Toast.makeText(this, R.string.err_usu_noauth, Toast.LENGTH_SHORT).show();
            return;
        }

        // Preparar los nuevos valores
        SQLiteDatabase db = miDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("titulo", titulo);
        values.put("descripcion", descripcion);
        // Conservamos la fecha de creación original
        values.put("fechaCreacion", fechaCreacion);
        values.put("FechaFinalizacion", fechaFinalSeleccionada);
        values.put("prioridad", prioridad);
        values.put("usuarioId", userId);

        int rowsUpdated = db.update("tareas", values, "id=?", new String[]{String.valueOf(tareaId)});
        db.close();

        if (rowsUpdated > 0) {
            Toast.makeText(this, R.string.tarea_actualizada, Toast.LENGTH_SHORT).show();
            // Cancelar la notificación programada anteriormente para esta tarea
            NotificacionAux.cancelarNotificacion(this, tareaId);
            // Programar una nueva notificación con la fecha actualizada
            NotificacionAux.programarNotificacion(this, tareaId, titulo, fechaFinalSeleccionada);
            finish(); // Regresa a la actividad anterior (MainActivity)
        } else {
            Toast.makeText(this, R.string.err_tarea_actualizada, Toast.LENGTH_SHORT).show();
        }
    }

}
