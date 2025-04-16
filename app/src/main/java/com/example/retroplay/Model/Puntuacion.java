package com.example.retroplay.Model;

public class Puntuacion {

    String idUsuario;
    int puntuacion;

    public Puntuacion(String idUsuario, int puntuacion) {
        this.idUsuario = idUsuario;
        this.puntuacion = puntuacion;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }
}
