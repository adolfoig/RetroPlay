package com.example.retroplay.Model;

public class Puntuacion {

    String nombreUsuario;
    int puntuacion;
    private boolean esUsuarioActual;


    public Puntuacion(String nombreUsuario, int puntuacion, boolean esUsuarioActual) {
        this.nombreUsuario = nombreUsuario;
        this.puntuacion = puntuacion;
        this.esUsuarioActual = esUsuarioActual;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String idUsuario) {
        this.nombreUsuario = idUsuario;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }

    public boolean isEsUsuarioActual() {
        return esUsuarioActual;
    }

    public void setEsUsuarioActual(boolean esUsuarioActual) {
        this.esUsuarioActual = esUsuarioActual;
    }
}
