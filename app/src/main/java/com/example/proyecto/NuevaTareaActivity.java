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
            // Suponiendo que 'tarea' es el objeto que acabas de crear y guardado
            long fechaFinalizacion = tarea.getFechaFinalizacion();
            scheduleDeadlineNotification(this, tarea.getId(), tarea.getTitulo(), fechaFinalizacion);
            finish(); // Regresa a la MainActivity
        } else {
            Toast.makeText(this, R.string.err_tarea_añadida, Toast.LENGTH_SHORT).show();
        }
    }
    // Programar la notificación que funciona aunque la aplicación no está activa
    public void scheduleDeadlineNotification(Context context, int tareaId, String tareaTitulo, long fechaFinalizacion) {
        long oneDayInMillis = 24 * 60 * 60 * 1000;
        // Calculamos el momento de notificar: 24 horas antes de la fecha de finalización
        long fechaDisparo = fechaFinalizacion - oneDayInMillis;
        long now = System.currentTimeMillis();

        // Si la fecha para disparar la notificacion ya pasó, por ejemplo, la tarea se crea con menos de 24h de antelación, esperamos 1 segundo para notificar
        if (fechaDisparo < now) {
            fechaDisparo = now + 1000;  // notifica en 1 segundo
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DeadlineReceiver.class);
        intent.putExtra("tareaId", tareaId);
        intent.putExtra("tareaTitulo", tareaTitulo);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, tareaId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            // Comprobación de permisos extraída de https://developer.android.com/about/versions/14/changes/schedule-exact-alarms?hl=es-419
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fechaDisparo, pendingIntent);
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, fechaDisparo, pendingIntent);
                    }
                } else {
                    Intent intentAjustes = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intentAjustes);
                }
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fechaDisparo, pendingIntent);
            }
            else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, fechaDisparo, pendingIntent);
            }

        }
    }

}
