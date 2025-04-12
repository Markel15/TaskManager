package com.example.proyecto;

public class SyncOperation {
    private int id;
    private int localId;
    private String accion;
    private String titulo;
    private String descripcion;
    private long fechaCreacion;
    private long fechaFinalizacion;
    private int completado;
    private int prioridad;
    private int usuarioId;
    private String localizacion;

    // Constructor vacío
    public SyncOperation() {}

    // Getters y Setters para cada atributo
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getLocalId() {
        return localId;
    }
    public void setLocalId(int localId) {
        this.localId = localId;
    }
    public String getAccion() {
        return accion;
    }
    public void setAccion(String accion) {
        this.accion = accion;
    }
    public String getTitulo() {
        return titulo;
    }
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    public long getFechaCreacion() {
        return fechaCreacion;
    }
    public void setFechaCreacion(long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    public long getFechaFinalizacion() {
        return fechaFinalizacion;
    }
    public void setFechaFinalizacion(long fechaFinalizacion) {
        this.fechaFinalizacion = fechaFinalizacion;
    }
    public int getCompletado() {
        return completado;
    }
    public void setCompletado(int completado) {
        this.completado = completado;
    }
    public int getPrioridad() {
        return prioridad;
    }
    public void setPrioridad(int prioridad) {
        this.prioridad = prioridad;
    }
    public int getUsuarioId() {
        return usuarioId;
    }
    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }
    public String getLocalizacion() {
        return localizacion;
    }
    public void setLocalizacion(String localizacion) {
        this.localizacion = localizacion;
    }
}
