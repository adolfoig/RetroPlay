package com.example.retroplay.clases;

import java.io.Serializable;
import java.util.Objects;

public class Usuario implements Serializable {

    private String id;
    private String nombre;
    private String email;
    private String password;
    private double puntuacionGlobal;

    public Usuario() {
    }

    public Usuario(String id, String nombre, String email, String password, double puntuacionGlobal) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.puntuacionGlobal = puntuacionGlobal;
    }

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public double getPuntuacionGlobal() {
        return puntuacionGlobal;
    }

    public void setPuntuacionGlobal(double puntuacionGlobal) {
        this.puntuacionGlobal = puntuacionGlobal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return id == usuario.id && Double.compare(puntuacionGlobal, usuario.puntuacionGlobal) == 0 && Objects.equals(nombre, usuario.nombre) && Objects.equals(email, usuario.email) && Objects.equals(password, usuario.password);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", puntuacionGlobal=" + puntuacionGlobal +
                '}';
    }
}
