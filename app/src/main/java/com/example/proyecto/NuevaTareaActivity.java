package com.example.proyecto;

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

public class NuevaTareaActivity extends BaseActivity implements OnFechaSelectedListener {
    private EditText etTitulo, etDescripcion, etFechaFinalizacion;
    private Spinner spPrioridad;
    private Button btnGuardar;
    private miBD miDb;
    private long fechaFinalSeleccionada = 0; // fecha en milisegundos

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

        long newRowId = db.insert("tareas", null, values);
        db.close();

        if (newRowId != -1) {
            Toast.makeText(this, R.string.tarea_añadida, Toast.LENGTH_SHORT).show();
            finish(); // Regresa a la MainActivity
        } else {
            Toast.makeText(this, R.string.err_tarea_añadida, Toast.LENGTH_SHORT).show();
        }
    }
}
