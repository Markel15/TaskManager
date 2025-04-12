package com.example.proyecto;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.RadioButton;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TareaAdapter extends RecyclerView.Adapter<TareaAdapter.TareaViewHolder> {
    private List<Tarea> listaTareas;
    private Context context;
    private OnAllTasksCompletedListener onAllTasksCompletedListener;

    // Interface para notificar cuando ya no quedan tareas pendientes
    public interface OnAllTasksCompletedListener {
        void onAllTasksCompleted();
    }

    public TareaAdapter(Context context, List<Tarea> listaTareas, OnAllTasksCompletedListener listener) {
        this.listaTareas = listaTareas;
        this.context = context;
        this.onAllTasksCompletedListener = listener;
    }

    @NonNull
    @Override
    public TareaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutDeCadaItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tarea, parent, false);
        return new TareaViewHolder(layoutDeCadaItem);
    }

    @Override
    public void onBindViewHolder(@NonNull TareaViewHolder holder, int position) {
        Tarea tarea = listaTareas.get(position);
        holder.tvTitulo.setText(tarea.getTitulo());
        holder.tvDescripcion.setText(tarea.getDescripcion());

        // Actualiza el estado del RadioButton según si la tarea está completada o no
        holder.rbCompletado.setChecked(tarea.isCompletado());

        // Mostrar la fecha de finalización si la preferencia está activada
        SharedPreferences prefs = context.getSharedPreferences("MiAppPrefs", Context.MODE_PRIVATE);
        boolean mostrarFecha = prefs.getBoolean("mostrar_fecha_finalizacion", false);
        if (mostrarFecha) {
            holder.tvFechaFinalizacion.setVisibility(View.VISIBLE);
            // Formatear la fecha de finalización
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String fechaFormateada = sdf.format(new Date(tarea.getFechaFinalizacion()));
            holder.tvFechaFinalizacion.setText(fechaFormateada);
        } else {
            holder.tvFechaFinalizacion.setVisibility(View.GONE);
        }

        // Configura el RadioButton para marcar la tarea como completada
        holder.rbCompletado.setOnClickListener(v -> {
            // Actualiza la base de datos: marca la tarea como completada
            miBD miDb = miBD.getMiBD(context);
            SQLiteDatabase db = miDb.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("completado", 1);
            db.update("tareas", values, "id=?", new String[]{String.valueOf(tarea.getId())});
            db.close();

            NotificacionAux.cancelarNotificacion(context, tarea.getId());

            // Elimina la tarea de la lista y notifica al adapter
            int pos = holder.getBindingAdapterPosition();
            listaTareas.remove(pos);
            notifyItemRemoved(pos);

            Data data = new Data.Builder()
                    .putString("accion", "completar")
                    .putInt("usuarioId", tarea.getUsuId())
                    .putInt("localId", tarea.getId())
                    .build();
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SyncTareaWorker.class)
                    .setInputData(data)
                    .build();
            WorkManager.getInstance(context).enqueue(workRequest);

            // Si ya no quedan tareas, invocar el callback para disparar la notificación
            if (listaTareas.isEmpty() && onAllTasksCompletedListener != null) {
                onAllTasksCompletedListener.onAllTasksCompleted();
            }
        });

        // Opciones (editar, borrar, etc.)
        holder.ivOpciones.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.ivOpciones);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_tarea, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    int id = item.getItemId();
                    if (id == R.id.opcion1) {
                        Intent intent = new Intent(context, EditTareaActivity.class);
                        intent.putExtra("tarea_id", tarea.getId());
                        context.startActivity(intent);
                        return true;
                    } else if (id == R.id.opcion2) {
                        miBD dbHelper = miBD.getMiBD(context);
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        int filasBorradas = db.delete("tareas", "id=?", new String[]{String.valueOf(tarea.getId())});
                        db.close();
                        NotificacionAux.cancelarNotificacion(context, tarea.getId());
                        if (filasBorradas > 0) {
                            int pos = holder.getBindingAdapterPosition();
                            Tarea tareaEliminada = listaTareas.get(pos);
                            listaTareas.remove(pos);
                            notifyItemRemoved(pos);
                            //Actualizar en el servidor
                            Data data = new Data.Builder()
                                    .putString("accion", "eliminar")
                                    .putInt("localId", tarea.getId())
                                    .putInt("usuarioId", tarea.getUsuId())
                                    .build();
                            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SyncTareaWorker.class)
                                    .setInputData(data)
                                    .build();
                            WorkManager.getInstance(context).enqueue(workRequest);
                            // Limpiar lista de la actividad principal para que no haya incongruencias de código
                            if (context instanceof MainActivity) {
                                ((MainActivity) context).eliminarTareaDeLista(tareaEliminada);
                            }
                            // Comprobar si ya no quedan tareas pendientes
                            if (listaTareas.isEmpty() && onAllTasksCompletedListener != null) {
                                onAllTasksCompletedListener.onAllTasksCompleted();
                            }
                        }
                        return true;
                    }
                    return false;
                }
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return listaTareas.size();
    }

    public static class TareaViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDescripcion, tvFechaFinalizacion;;
        RadioButton rbCompletado;
        ImageView ivOpciones;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            rbCompletado = itemView.findViewById(R.id.rbCompletado);
            ivOpciones = itemView.findViewById(R.id.ivOptions);
            tvFechaFinalizacion = itemView.findViewById(R.id.tvFechaFinalizacion);
        }
    }
}
