package com.example.proyecto;

import android.content.Context;
import android.widget.Toast;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapTouchReceiver implements MapEventsReceiver {

    // Código adaptado del laboratorio 09 de eGela: OpenStreetMaps
    private Context context;
    private MapView map;
    private Marker lastMarker; // Guarda el último marcador añadido

    public MapTouchReceiver(Context context, MapView map) {
        this.context = context;
        this.map = map;
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        // Si ya existe un marcador, eliminarlo
        if (lastMarker != null) {
            map.getOverlays().remove(lastMarker);
        }

        // Crear un nuevo marcador
        Marker marker = new Marker(map);
        marker.setPosition(p);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(context.getString(R.string.descr_selec_ubi));
        marker.setIcon(context.getResources().getDrawable(R.drawable.map_marker, null));

        // Guardar la referencia del nuevo marcador para poder eliminarlo en el futuro
        lastMarker = marker;

        // Añadir el marcador al mapa y refrescar
        map.getOverlays().add(marker);
        map.invalidate();

        Toast.makeText(context, "Ubicación seleccionada: " + p.getLatitude() + ", " + p.getLongitude(), Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;  // No hace nada, pero, hay que implementarlo, sino, da error
    }
}

