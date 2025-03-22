package com.example.retroplay.clases;

import java.io.Serializable;

public class Logro implements Serializable {
    private  String descripcion;
    private  int imagenMedalla;

    private int puntuacion;

    public Logro(String descripcion, int imagenMedalla, int puntuacion){

        this.descripcion = descripcion;
        this.imagenMedalla = imagenMedalla;
        this.puntuacion = puntuacion;
    }
     public Logro() {

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
