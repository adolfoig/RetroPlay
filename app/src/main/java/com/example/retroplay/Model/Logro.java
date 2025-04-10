package com.example.retroplay.Model;

import java.io.Serializable;

public class Logro implements Serializable {
    private String id;
    private  String descripcion;
    private  int imagenMedalla;
    private int puntuacion;

    private boolean obtenido;


    public Logro(String id,String descripcion, int imagenMedalla, int puntuacion, boolean obtenido){
        this.id = id;
        this.descripcion = descripcion;
        this.imagenMedalla = imagenMedalla;
        this.puntuacion = puntuacion;
        this.obtenido = obtenido;
    }
     public Logro() {
        // Vacio para FireStore
     }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isObtenido() {
        return obtenido;
    }

    public void setObtenido(boolean obtenido) {
        this.obtenido = obtenido;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getImagenMedalla() {
        return imagenMedalla;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setImagenMedalla(int imagenMedalla) {
        this.imagenMedalla = imagenMedalla;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }
}
