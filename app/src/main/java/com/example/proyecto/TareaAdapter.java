package com.example.proyecto;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TareaAdapter extends RecyclerView.Adapter<TareaAdapter.TareaViewHolder> {
    private List<Tarea> listaTareas;

    public TareaAdapter(List<Tarea> listaTareas) {
        this.listaTareas = listaTareas;
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
    }

    @Override
    public int getItemCount() {
        return listaTareas.size();
    }

    public static class TareaViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDescripcion;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
        }
    }
}
