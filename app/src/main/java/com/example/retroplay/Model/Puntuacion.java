package com.example.retroplay.Model;

public class Puntuacion {

    String nombreUsuario;
    int puntuacion;
    private boolean usuarioActual;


    public Puntuacion(String nombreUsuario, int puntuacion, boolean esUsuarioActual) {
        this.nombreUsuario = nombreUsuario;
        this.puntuacion = puntuacion;
        this.usuarioActual = esUsuarioActual;
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

    public boolean isUsuarioActual() {
        return usuarioActual;
    }

    public void setUsuarioActual(boolean usuarioActual) {
        this.usuarioActual = usuarioActual;
    }
}
