package com.example.retroplay.clases;

public class Logro {
    private final String descripcion;
    private final int imagenMedalla;

    public Logro(String descripcion, int imagenMedalla) {
        this.descripcion = descripcion;
        this.imagenMedalla = imagenMedalla;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getImagenMedalla() {
        return imagenMedalla;
    }
}
