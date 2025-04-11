package com.example.proyecto;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class NuevaTareaActivity extends BaseActivity implements OnFechaSelectedListener {
    private static final int REQUEST_UBICACION = 1001;

    private EditText etTitulo, etDescripcion, etFechaFinalizacion, etCoordenadas;
    private Spinner spPrioridad;
    private Button btnGuardar;
    private miBD miDb;
    private long fechaFinalSeleccionada = 0; // fecha en milisegundos

    private String coordenadas = "0 , 0";

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

        // Configura el EditText para que lance el DialogFragment de fecha
        etFechaFinalizacion.setFocusable(false);
        etFechaFinalizacion.setOnClickListener(v -> {
            ClaseDialogoFecha dialogoFecha = new ClaseDialogoFecha();
            dialogoFecha.show(getSupportFragmentManager(), "datePicker");
        });

        // Configura el editText con las coordenadas para abrir al actividad del mapa y obtener el valor
        etCoordenadas = findViewById(R.id.etCoordenadas);
        etCoordenadas.setFocusable(false);
        etCoordenadas.setOnClickListener(v -> {
            Intent intent = new Intent(this, UbicacionActivity.class);
            startActivityForResult(intent, REQUEST_UBICACION);
        });

        // Configura el Spinner con el array de prioridades
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.prioridad_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPrioridad.setAdapter(adapter);

        btnGuardar.setOnClickListener(v -> guardarTarea());
    }

    @Override
    public void onFechaSeleccionada(long fecha) {
        fechaFinalSeleccionada = fecha;
        // Formatea la fecha y la muestra en el EditText
        Calendar calendario = Calendar.getInstance();
        calendario.setTimeInMillis(fecha);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etFechaFinalizacion.setText(sdf.format(calendario.getTime()));
    }

    private void guardarTarea() {
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

        // Crear la tarea
        Tarea tarea = new Tarea();
        tarea.setTitulo(titulo);
        tarea.setDescripcion(descripcion);
        tarea.setUsuId(userId);
        tarea.setFechaCreacion(System.currentTimeMillis());
        tarea.setFechaFinalizacion(fechaFinalSeleccionada);
        tarea.setPrioridad(prioridad);
        tarea.setCompletado(false);
        tarea.setCoordenadas(coordenadas);

        // Insertar la tarea en la base de datos
        SQLiteDatabase db = miDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("titulo", tarea.getTitulo());
        values.put("descripcion", tarea.getDescripcion());
        values.put("fechaCreacion", tarea.getFechaCreacion());
        values.put("FechaFinalizacion", tarea.getFechaFinalizacion());
        values.put("completado", tarea.isCompletado() ? 1 : 0);
        values.put("prioridad", tarea.getPrioridad());
        values.put("usuarioId", tarea.getUsuId());
        values.put("localizacion", tarea.getCoordenadas());

        long newRowId = db.insert("tareas", null, values);
        db.close();

        if (newRowId != -1) {
            // Asignar el ID generado al objeto Tarea
            tarea.setId((int)newRowId);
            Toast.makeText(this, R.string.tarea_añadida, Toast.LENGTH_SHORT).show();
            long fechaFinalizacion = tarea.getFechaFinalizacion();
            NotificacionAux.programarNotificacion(this, tarea.getId(), tarea.getTitulo(), fechaFinalizacion);

            // Encolar el Worker para enviar la tarea al servidor
            enviarTareaRemota(tarea);

            finish(); // Regresa a la MainActivity
        } else {
            Toast.makeText(this, R.string.err_tarea_añadida, Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_UBICACION && resultCode == RESULT_OK) {
            // UbicacionActivity devuelve dos extras: "latitud" y "longitud"
            double latitud = data.getDoubleExtra("latitud", 0);
            double longitud = data.getDoubleExtra("longitud", 0);
            coordenadas = latitud + " , " + longitud;
            etCoordenadas.setText(coordenadas);
        }
    }
    private void enviarTareaRemota(Tarea tarea) {
        Data data = new Data.Builder()
                .putString("titulo", tarea.getTitulo())
                .putString("descripcion", tarea.getDescripcion())
                .putLong("fechaCreacion", tarea.getFechaCreacion())
                .putLong("fechaFinalizacion", tarea.getFechaFinalizacion())
                .putInt("completado", tarea.isCompletado() ? 1 : 0)
                .putInt("prioridad", tarea.getPrioridad())
                .putInt("usuarioId", tarea.getUsuId())
                .putString("coordenadas", tarea.getCoordenadas())
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TareaWorker.class)
                .setInputData(data)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(workRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            int tareaIdRemota = workInfo.getOutputData().getInt("id", -1);
                            if (tareaIdRemota != -1) {
                                Toast.makeText(this, "Tarea registrada remotamente", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Error en el registro remoto", Toast.LENGTH_SHORT).show();
                            }
                        } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                            Toast.makeText(this, "Error en el registro remoto", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(workRequest);
    }

}
