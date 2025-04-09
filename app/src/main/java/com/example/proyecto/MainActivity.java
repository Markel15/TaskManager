package com.example.proyecto;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {
    miBD miDb;
    List<Tarea> listaTareas;
    TareaAdapter adapter;
    private List<Tarea> filtroLista;  // Variable de apoyo que es una copia de la original para poder filtrar tareas
    DrawerLayout elMenuDesplegable;
    NavigationView navigationView;
    ImageView imageView;
    int codigo = 101;

    private ActivityResultLauncher<Void> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;


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
        filtroLista = new ArrayList<>(listaTareas);
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
        View headerView = navigationView.getHeaderView(0);
        imageView = headerView.findViewById(R.id.mi_perfil);
        if (userId != -1) {
            String username = obtenerNombreUsuario(userId);

            // Actualizar el TextView en la cabecera
            headerView = navigationView.getHeaderView(0);
            TextView tvUsername = headerView.findViewById(R.id.tvUsername);
            if (username != null) {
                tvUsername.setText(username);
            } else {
                tvUsername.setText(R.string.err_usu_noauth);
            }
        }
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
                   // Cancelar las notificaciones programadas del usuario
                   for (Tarea tarea : listaTareas) {
                       NotificacionAux.cancelarNotificacion(MainActivity.this, tarea.getId());
                   }
                   // Redirigir a LoginActivity
                   Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                   startActivity(intent);
                   finish();
               }
               else if (item.getItemId() == R.id.nav_fecha) {
                   dialogoFecha();
               }
               else if (item.getItemId() == R.id.nav_calendario) {
                   Intent intent = new Intent(MainActivity.this, CalendarioActivity.class);
                   startActivity(intent);
               }
               elMenuDesplegable.closeDrawers();
               return false;
           }
        });
        // Accion al pulsar la imagen que muestra la foto de perfil del usuario
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inflar el layout del diálogo personalizado
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                View dialogView = inflater.inflate(R.layout.dialogo_cambio_imagen, null);

                // Referenciar la imagen y los botones del layout
                ImageView dialogImage = dialogView.findViewById(R.id.dialog_image);
                Button btnFoto = dialogView.findViewById(R.id.btn_tomar_foto);
                Button btnGaleria = dialogView.findViewById(R.id.btn_galeria);

                // Asignar la imagen actual del ImageView principal al diálogo
                dialogImage.setImageDrawable(imageView.getDrawable());

                // Construir el diálogo
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this,R.style.MaterialAlertDialog_Proyecto)
                        .setView(dialogView)
                        .setCancelable(true);  // Permite cerrar al pulsar fuera

                // Crear y mostrar el diálogo
                AlertDialog dialog = builder.create();
                dialog.show();

                // Asignar listeners a los botones
                btnFoto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        takePictureLauncher.launch(null);
                        dialog.dismiss();
                    }
                });

                btnGaleria.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pickImageLauncher.launch("image/*");
                        dialog.dismiss();
                    }
                });

                // Mostrar el diálogo
                dialog.show();
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

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (!hayInternet()) {
                Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
            }
            else if (bitmap != null) {
                actualizarImagenPerfil(bitmap);
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (!hayInternet()) {
                Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
            }
            else if (uri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    actualizarImagenPerfil(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        obtenerYActualizarPerfil();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Al volver de la actividad de otra tarea (normalmente de añadir una nueva tarea) se ejecuta esto
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("idDeUsuario", -1);
        listaTareas.clear();
        listaTareas.addAll(obtenerTarasParaUsu(userId));
        filtroLista = new ArrayList<>(listaTareas);
        adapter.notifyDataSetChanged();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.definicion_menu,menu);
        // Obtener el ítem de búsqueda y configurar su SearchView
        MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.buscar_tareas));
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filtrarTareas(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filtrarTareas(newText);
                return true;
            }
        });
        return true;
    }
    // Método que filtra las tareas según el query
    private void filtrarTareas(String query) {
        query = query.toLowerCase().trim();
        listaTareas.clear();
        if (query.isEmpty()) { // Si no se ha escrito nada no aplica filtro, devuelve la lista original
            listaTareas.addAll(filtroLista);
        } else {
            for (Tarea tarea : filtroLista) {
                if (tarea.getTitulo().toLowerCase().contains(query) || tarea.getDescripcion().toLowerCase().contains(query)) {
                    listaTareas.add(tarea);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (item.getItemId() == android.R.id.home) {
            elMenuDesplegable.openDrawer(GravityCompat.START);
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
    @SuppressLint("Range") // Omitir advertencias sobre el uso de columnas que no existan
    public String obtenerNombreUsuario(int userId) {
        SQLiteDatabase db = miDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT username FROM usuarios WHERE id = ?", new String[]{String.valueOf(userId)});

        String username = null;
        if (cursor.moveToFirst()) {
            username = cursor.getString(cursor.getColumnIndex("username"));
        }
        cursor.close();

        return username;
    }
    public void eliminarTareaDeLista(Tarea tarea) {
        listaTareas.remove(tarea);
        filtroLista.remove(tarea);
    }
    private void actualizarImagenPerfil(Bitmap bitmap) {
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("idDeUsuario", -1);

        if (userId != -1) {
            try {
                // Guardar el bitmap en archivo temporal
                File file = new File(getCacheDir(), "perfil_temp.jpg");
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
                fos.close();

                // Pasar la ruta al Worker
                Data data = new Data.Builder()
                        .putInt("userId", userId)
                        .putString("imagePath", file.getAbsolutePath())
                        .build();

                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(updatePerfilWorker.class)
                        .setInputData(data)
                        .build();

                WorkManager.getInstance(this).enqueue(request);

                // Observar el resultado del Worker updatePerfilWorker
                WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.getId())
                        .observe(this, workInfo -> {
                            if (workInfo != null && workInfo.getState().isFinished()) {
                                if (workInfo.getState() == androidx.work.WorkInfo.State.SUCCEEDED) {
                                    // Asigna el bitmap al ImageView
                                    imageView.setImageBitmap(bitmap);
                                } else {
                                    String errorType = workInfo.getOutputData().getString("error");
                                    String mensaje;
                                    if ("tamaño_excedido".equals(errorType)) {
                                        mensaje = getString(R.string.err_tamaño);
                                    } else if ("db_local_fallo".equals(errorType)) {
                                        mensaje = getString(R.string.err_bd_foto);
                                    } else if ("error_server".equals(errorType)) {
                                        mensaje = getString(R.string.err_foto);
                                    } else if (errorType != null && errorType.startsWith("error_http")) {
                                        mensaje = getString(R.string.err_http) + errorType;
                                    } else {
                                        mensaje = getString(R.string.err_foto);
                                    }
                                    Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.err_guard_imagen, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean hayInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                return capabilities != null && (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        }
        return false;
    }

    private void obtenerYActualizarPerfil() {
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("idDeUsuario", -1);
        if (userId == -1) return;

        androidx.work.Data inputData = new androidx.work.Data.Builder()
                .putInt("userId", userId)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(getPerfilWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(request);

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == androidx.work.WorkInfo.State.SUCCEEDED) {
                            // Recuperamos el username de la salida (si lo necesitas)
                            String username = workInfo.getOutputData().getString("username");
                            // Actualizamos la cabecera (esto es solo un ejemplo)
                            View headerView = navigationView.getHeaderView(0);
                            TextView tvUsername = headerView.findViewById(R.id.tvUsername);
                            tvUsername.setText(username);

                            byte[] imagenBytes = obtenerImagenPerfilLocal(userId);
                            if (imagenBytes != null) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(imagenBytes, 0, imagenBytes.length);
                                ImageView imageView = headerView.findViewById(R.id.mi_perfil);
                                imageView.setImageBitmap(bitmap);
                            }
                        } else {
                            Toast.makeText(this, R.string.err_des_foto, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    @SuppressLint("Range")
    private byte[] obtenerImagenPerfilLocal(int userId) {
        SQLiteDatabase db = miDb.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT imagenPerfil FROM usuarios WHERE id = ?", new String[]{String.valueOf(userId)});
        byte[] imagenBytes = null;
        if (cursor.moveToFirst()) {
            imagenBytes = cursor.getBlob(cursor.getColumnIndex("imagenPerfil"));
        }
        cursor.close();
        return imagenBytes;
    }

}