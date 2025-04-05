package com.example.retroplay.Model;

public class LogrosDisponibles {

    private String idJuego;
    private String descripcion;
    private int puntuacion;

    public LogrosDisponibles(){}

    public LogrosDisponibles(String idJuego, String descripcion, int puntuacion){
        this.idJuego=idJuego;
        this.descripcion=descripcion;
        this.puntuacion=puntuacion;
    }

    public String getIdJuego() {
        return idJuego;
    }

    public void setIdJuego(String idJuego) {
        this.idJuego = idJuego;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }
}
