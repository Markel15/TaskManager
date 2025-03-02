package com.example.proyecto;

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
import androidx.annotation.NonNull;
import android.widget.RadioButton;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TareaAdapter extends RecyclerView.Adapter<TareaAdapter.TareaViewHolder> {
    private List<Tarea> listaTareas;
    private Context context;

    public TareaAdapter(Context context, List<Tarea> listaTareas) {
        this.listaTareas = listaTareas;
        this.context = context;
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

        // Configura el RadioButton para marcar la tarea como completada
        holder.rbCompletado.setOnClickListener(v -> {
            // Actualiza la base de datos: marca la tarea como completada
            miBD miDb = miBD.getMiBD(context);
            SQLiteDatabase db = miDb.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("completado", 1);
            db.update("tareas", values, "id=?", new String[]{String.valueOf(tarea.getId())});
            db.close();

            // Elimina la tarea de la lista y notifica al adapter
            int pos = holder.getBindingAdapterPosition();
            listaTareas.remove(pos);
            notifyItemRemoved(pos);
        });

        holder.ivOpciones.setOnClickListener(v -> {
            // Crear un PopupMenu asociado al view
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
                        if (filasBorradas > 0) {
                            // Eliminar el elemento de la lista y notificar el cambio
                            int pos = holder.getBindingAdapterPosition();
                            listaTareas.remove(pos);
                            notifyItemRemoved(pos);
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
        TextView tvTitulo, tvDescripcion;
        RadioButton rbCompletado;

        ImageView ivOpciones;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            rbCompletado = itemView.findViewById(R.id.rbCompletado);
            ivOpciones = itemView.findViewById(R.id.ivOptions);
        }
    }
}
