package com.example.proyecto;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ListaTareasFragment extends Fragment {

    private static final String ARG_USER_ID = "userId";
    private int userId;
    private long comienzoDia;
    private long finDia;
    private RecyclerView recyclerView;
    private TareaAdapter adapter;
    private TextView tvVacio;
    private List<Tarea> listaTareas = new ArrayList<>();
    private boolean viewCreada = false;
    private Long pendingComienzoDia = null, pendingFinDia = null;

    public static ListaTareasFragment newInstance(int userId) {
        ListaTareasFragment fragment = new ListaTareasFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_tareas, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewTareasCal);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TareaAdapter(getContext(), listaTareas, null);
        recyclerView.setAdapter(adapter);
        tvVacio = view.findViewById(R.id.tvVacio);

        viewCreada = true;
        // Si había una actualización pendiente, ejecútala
        if (pendingComienzoDia != null && pendingFinDia != null) {
            actualizarTareas(pendingComienzoDia, pendingFinDia);
            pendingComienzoDia = null;
            pendingFinDia = null;
        }
        return view;
    }


    // Método para actualizar las tareas según el intervalo de tiempo
    public void actualizarTareas(long comienzoDia, long finDia) {
        if (!viewCreada) {
            pendingComienzoDia = comienzoDia;
            pendingFinDia = finDia;
            return;
        }
        this.comienzoDia = comienzoDia;
        this.finDia = finDia;
        listaTareas.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        cargarTareas();
    }

    @SuppressLint("Range")
    private void cargarTareas() {
        listaTareas.clear();
        SQLiteDatabase db = miBD.getMiBD(getContext()).getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM tareas WHERE usuarioId = ? AND completado = 0 AND FechaFinalizacion BETWEEN ? AND ? ORDER BY FechaFinalizacion ASC",
                new String[]{
                        String.valueOf(userId),
                        String.valueOf(comienzoDia),
                        String.valueOf(finDia)
                });
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
                listaTareas.add(tarea);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        adapter.notifyDataSetChanged();

        // Mostrar u ocultar el mensaje según si hay tareas
        if (listaTareas.isEmpty()) {
            tvVacio.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvVacio.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
