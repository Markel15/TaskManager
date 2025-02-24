package com.example.proyecto;

public class Tarea {
    private int id;
    private int usuId;  // ID del usuario
    private String titulo;
    private String descripcion;
    private long fechaCreacion;   // Guardado como timestamp
    private long fechaFinalizacion;     // Guardado como timestamp
    private boolean completado;
    private int prioridad;     // 0 = baja, 1 = media, 2 = alta

    public Tarea(){}

    // Constructor completo
    public Tarea(int id, int usuId, String titulo, String descripcion, long fechaCreacion, long fechaFinalizacion, boolean completado, int prioridad) {
        this.id = id;
        this.usuId = usuId;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fechaCreacion = fechaCreacion;
        this.fechaFinalizacion = fechaFinalizacion;
        this.completado = completado;
        this.prioridad = prioridad;
    }

    // Getters y setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUsuId() { return usuId; }
    public void setUsuId(int usuId) { this.usuId = usuId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public long getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(long fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public long getFechaFinalizacion() { return fechaFinalizacion; }
    public void setFechaFinalizacion(long fechaFinalizacion) { this.fechaFinalizacion = fechaFinalizacion; }

    public boolean isCompletado() { return completado; }
    public void setCompletado(boolean completado) { this.completado = completado; }

    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }
}
