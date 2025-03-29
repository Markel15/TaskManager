package com.example.proyecto;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

public class UbicacionActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 2001;
    private FusedLocationProviderClient proveedordelocalizacion ;
    private Marker markerUserLocation;  // Marcador fijo para la ubicación del usuario
    private MapTouchReceiver touchReceiver;

    private MapView map;

    private GeoPoint ultimaPosicion = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Cargar configuración de osmdroid. Código extraído del laboratorio 09 de eGela: OpenStreetMaps
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_ubicacion);

        // Inicializar el proveedordelocalizacion
        proveedordelocalizacion  = LocationServices.getFusedLocationProviderClient(this);

        map = findViewById(R.id.map);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // El siguiente código ha sido ligeramente adaptado del laboratorio 09 de eGela: OpenStreetMaps

        // Inicializa el receptor de toques y añade el overlay al mapa
        MapTouchReceiver touchReceiver = new MapTouchReceiver(this, map) {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                ultimaPosicion = p;  // Actualizamos la variable con la posición seleccionada
                return super.singleTapConfirmedHelper(p);
            }
        };

        MapEventsOverlay eventsOverlay = new MapEventsOverlay(touchReceiver);
        map.getOverlays().add(eventsOverlay);

        // Configurar el MapView
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        map.setMultiTouchControls(true);

        // Se elige un punto como inicial por si no están los permisos aceptados
        GeoPoint startPoint = new GeoPoint(42.8467, -2.6731);
        map.getController().setZoom(12.0);
        map.getController().setCenter(startPoint);

        // Refrescar el mapa
        map.invalidate();

        // Si se han recibido coordenadas, simula un toque para colocar el marcador inicial
        if (getIntent().hasExtra("latitud") && getIntent().hasExtra("longitud")) {
            double lat = getIntent().getDoubleExtra("latitud", 0);
            double lon = getIntent().getDoubleExtra("longitud", 0);
            GeoPoint initialPoint = new GeoPoint(lat, lon);
            // Simulamos el tap para que el receptor coloque el marcador
            touchReceiver.singleTapConfirmedHelper(initialPoint);
        }

        requestUserLocationMarker(map);

        // Configurar el botón flotante para confirmar la selección
        FloatingActionButton fabConfirmar = findViewById(R.id.fabConfirmar);
        fabConfirmar.setOnClickListener(v -> {
            if (ultimaPosicion != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitud", ultimaPosicion.getLatitude());
                resultIntent.putExtra("longitud", ultimaPosicion.getLongitude());
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(UbicacionActivity.this, R.string.selec_ubi, Toast.LENGTH_SHORT).show();
            }
        });
    }
    // Solicita la ubicación actual y añade el marcador fijo para la ubicación del usuario
    private void requestUserLocationMarker(MapView map) {
        // Verificar permisos de ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }
        // Obtener la última ubicación conocida
        // Código adaptado de la diapositiva Nº5 del tema de Geolocalizacion en eGela
        proveedordelocalizacion.getLastLocation()
            .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    GeoPoint currentPoint;
                    if (location != null) {
                        currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        // Centrar el mapa en currentPoint
                        map.getController().setZoom(12.0);
                        map.getController().setCenter(currentPoint);

                        // Añadir el marcador fijo con la ubicación del usuario
                        markerUserLocation = new Marker(map);
                        markerUserLocation.setPosition(currentPoint);
                        markerUserLocation.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        markerUserLocation.setTitle("Tu ubicación actual");
                        markerUserLocation.setIcon(getResources().getDrawable(R.drawable.map_marker_radius, null));
                        map.getOverlays().add(markerUserLocation);
                        map.invalidate();
                    } else {
                        Toast.makeText(UbicacionActivity.this, R.string.no_ubi, Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(UbicacionActivity.this, R.string.error_ubi + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
    // Maneja el resultado de la solicitud de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Si se concedió el permiso, volvemos a solicitar la ubicación
                requestUserLocationMarker(map);
            } else {
                Toast.makeText(this, R.string.ubi_denegado, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
