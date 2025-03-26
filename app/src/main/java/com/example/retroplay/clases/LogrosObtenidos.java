package com.example.retroplay.clases;

public class LogrosObtenidos {

    private String idUsuario;
    private String idLogrosDisponibles;

    public LogrosObtenidos(){}

    public LogrosObtenidos(String idUsuario, String idLogrosDisponibles) {
        this.idUsuario = idUsuario;
        this.idLogrosDisponibles = idLogrosDisponibles;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getIdLogrosDisponibles() {
        return idLogrosDisponibles;
    }

    public void setIdLogrosDisponibles(String idLogrosDisponibles) {
        this.idLogrosDisponibles = idLogrosDisponibles;
    }
}
