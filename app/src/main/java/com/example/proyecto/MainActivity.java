package com.example.proyecto;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {
    miBD miDb;
    List<Tarea> listaTareas;
    TareaAdapter adapter;
    DrawerLayout elMenuDesplegable;
    NavigationView navigationView;
    int codigo = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // código del laboratorio 04 (números muertos) para solicitar permiso (necesario en las nuevas versiones de android)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, codigo);
            }
        }

        miDb = miBD.getMiBD(this);
        // Recuperar el ID del usuario autenticado
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("idDeUsuario", -1);
        if(userId == -1){  // Se ha cerrado la sesión, volver a la página de inicio de sesión
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        // Obtener las tareas del usuario
        listaTareas = obtenerTarasParaUsu(userId);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.labarra));
        // Configurar los elementos a mostrar
        RecyclerView lalista= findViewById(R.id.recyclerViewTareas);
        lalista.setLayoutManager(new LinearLayoutManager(this));
        FloatingActionButton butn_nueva_tarea = findViewById(R.id.boton_tarea);
        butn_nueva_tarea.setOnClickListener(v -> {
            Intent intent = new Intent(this, NuevaTareaActivity.class );
            startActivity(intent);
        });
        adapter = new TareaAdapter(this, listaTareas, new TareaAdapter.OnAllTasksCompletedListener() {
            @Override
            public void onAllTasksCompleted() {
                showCompleteNotification();
            }
        });
        lalista.setAdapter(adapter);

        // Configurar el DrawerLayout y NavigationView
        elMenuDesplegable = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.elnavigationview);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
           @Override
           public boolean onNavigationItemSelected(@NonNull MenuItem item) {  // Hecho con if/ else if porque con case daba error al requerir que id sea una constante en ejecución
               if (item.getItemId() == R.id.nav_idioma) {
                   dialogoIdioma();
               }
               /* De momento deshabilitado
               else if (item.getItemId() == R.id.nav_proyectos) {
                   // De momento está vacío
               }*/
               else if (item.getItemId() == R.id.nav_logout) {
                   //Eliminar id del usuario de las preferencias
                   SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
                   SharedPreferences.Editor editor = prefs.edit();
                   editor.remove("idDeUsuario");
                   editor.apply();
                   /*
                   // Cancelar las notificaciones programadas
                   for (Tarea tarea : listaTareas) {
                       NotificacionAux.cancelarNotificacion(MainActivity.this, tarea.getId());
                   }
                   */
                   // Redirigir a LoginActivity
                   Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                   startActivity(intent);
                   finish();
               }
               else if (item.getItemId() == R.id.nav_fecha) {
                   dialogoFecha();
               }
               elMenuDesplegable.closeDrawers();
               return false;
           }
        });

        // Establecer el ícono de "hamburguesa" en la barra de acción
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.menu_24px);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Callback para que el botón Back cierre el Drawer si está abierto
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (elMenuDesplegable.isDrawerOpen(GravityCompat.START)) {
                    elMenuDesplegable.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary)); // Usar el color de la Toolbar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Al volver de la actividad de otra tarea (normalmente de añadir una nueva tarea) se ejecuta esto
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("idDeUsuario", -1);
        listaTareas.clear();
        listaTareas.addAll(obtenerTarasParaUsu(userId));
        adapter.notifyDataSetChanged();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.definicion_menu,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (item.getItemId() == android.R.id.home) {
            elMenuDesplegable.openDrawer(GravityCompat.START);
            return true;
        }
        if (item.getItemId() == R.id.opcion1) {
            // Acción para alguna opción de la Toolbar
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @SuppressLint("Range")  // Omitir los warnings en los que las columnas no existan, la base de datos se va a crear con esos nombres
    public List<Tarea> obtenerTarasParaUsu(int usuId) {
        List<Tarea> tareas = new ArrayList<>();
        SQLiteDatabase bd = miDb.getReadableDatabase();
        Cursor cursor = bd.rawQuery("SELECT * FROM tareas WHERE usuarioId = ? AND completado = 0 ORDER BY FechaFinalizacion ASC",
                new String[]{String.valueOf(usuId)});
        if (cursor.moveToFirst()) {
            do {
                Tarea tarea = new Tarea();
                tarea.setId(cursor.getInt(cursor.getColumnIndex("id")));
                tarea.setTitulo(cursor.getString(cursor.getColumnIndex("titulo")));
                tarea.setDescripcion(cursor.getString(cursor.getColumnIndex("descripcion")));
                tarea.setFechaCreacion(cursor.getLong(cursor.getColumnIndex("fechaCreacion")));
                tarea.setFechaFinalizacion(cursor.getLong(cursor.getColumnIndex("FechaFinalizacion")));
                tarea.setCompletado(cursor.getInt(cursor.getColumnIndex("completado")) == 1);
                tarea.setPrioridad(cursor.getInt(cursor.getColumnIndex("prioridad")));
                tareas.add(tarea);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tareas;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == codigo) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido: se puede notificar cuando sea necesario.
            } else {
                Toast.makeText(this, R.string.noti_deneg, Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void showCompleteNotification() {
        // Código adaptado de eGela diapositiva 20 tema 5 y laboratorio 4
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("complete_channel",
                    "Complete Notifications", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "complete_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.felicidades))
                .setContentText(getString(R.string.tareas_completadas))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    private void dialogoIdioma() {
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        String[] idiomas = {"Español", "English", "Euskara"};
        final String[] codigos = {"es", "en", "eu"};

        // Recuperar el idioma actual
        String idiomaActual = prefs.getString("idioma", "es");
        int selectedIndex = 0;
        for (int i = 0; i < codigos.length; i++) {
            if (codigos[i].equals(idiomaActual)) {
                selectedIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.selec_idioma);
        builder.setSingleChoiceItems(idiomas, selectedIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String idiomaSeleccionado = codigos[which];
                if (!idiomaSeleccionado.equals(idiomaActual)) {
                    SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
                    prefs.edit().putString("idioma", idiomaSeleccionado).apply();

                    // Limpiar flags
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    // Gesion de Locale ya se hace en Base activity al reiniciar la actividad
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancelar, null);
        builder.show();
    }
    private void dialogoFecha() {
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        boolean mostrarFecha = prefs.getBoolean("mostrar_fecha_finalizacion", false);
        String[] opciones = {getString(R.string.mostrar_fecha),getString(R.string.ocultar_fecha) };
        int indice = mostrarFecha ? 0 : 1;  // Si es true el indice es 0, sino 1

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.config_fecha);
        builder.setSingleChoiceItems(opciones, indice, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean opcion = (which == 0); // 0: mostrar, 1: ocultar
                prefs.edit().putBoolean("mostrar_fecha_finalizacion", opcion).apply();
                dialog.dismiss();
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(R.string.cancelar, null);
        builder.show();
    }



}