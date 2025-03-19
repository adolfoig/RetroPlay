package com.example.retroplay.clases;

import java.io.Serializable;

public class Juego implements Serializable {
    private String id;
    private String nombre;
    private String descripcion;
    private String rutaImagen;
    private String rutaArchivo;
    private boolean favorito;

    public Juego() {
        // Constructor vacío necesario para Firestore
    }

    public Juego(String id, String nombre, String descripcion, String rutaImagen, String rutaArchivo, boolean favorito) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.rutaImagen = rutaImagen;
        this.rutaArchivo = rutaArchivo;
        this.favorito = favorito;
    }

    // Getters y setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getRutaImagen() {
        return rutaImagen;
    }

    public void setRutaImagen(String rutaImagen) {
        this.rutaImagen = rutaImagen;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public boolean isFavorito() {
        return favorito;
    }

    public void setFavorito(boolean favorito) {
        this.favorito = favorito;
    }
}
