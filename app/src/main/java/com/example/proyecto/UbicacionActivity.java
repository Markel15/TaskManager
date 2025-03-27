package com.example.proyecto;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;

public class UbicacionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Cargar configuración de osmdroid. Código extraído del laboratorio 09 de eGela: OpenStreetMaps
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_ubicacion);
        MapView map = findViewById(R.id.map);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // El siguiente código ha sido ligeramente adaptado del laboratorio 09 de eGela: OpenStreetMaps

        // Inicializa el receptor de toques y añade el overlay al mapa
        MapTouchReceiver touchReceiver = new MapTouchReceiver(this, map);
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(touchReceiver);
        map.getOverlays().add(eventsOverlay);

        // Configurar el MapView
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        map.setMultiTouchControls(true);

        // Establecer un punto inicial
        GeoPoint startPoint = new GeoPoint(42.8467, -2.6731);
        map.getController().setZoom(12.0);
        map.getController().setCenter(startPoint);

        // Refrescar el mapa
        map.invalidate();
    }
}
