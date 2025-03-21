package com.example.retroplay.clases;

import java.io.Serializable;

public class Logro implements Serializable {
    private  String descripcion;
    private  int imagenMedalla;

    private String puntuacion;

    public Logro(String descripcion, int imagenMedalla, String puntuacion){

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

    public String getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(String puntuacion) {
        this.puntuacion = puntuacion;
    }
}
