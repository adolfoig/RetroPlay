package com.example.retroplay.Viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.retroplay.Repository.UsuarioRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

public class UsuarioViewModel extends ViewModel {
    private final UsuarioRepository usuarioRepository;

    public UsuarioViewModel() {
        this.usuarioRepository = new UsuarioRepository();
    }

        public LiveData<Map<String, String>> getDatosUsuario() {
        return usuarioRepository.getDatosUsuario();
    }

    public LiveData<String> getResultadoActualizacion() {
        return usuarioRepository.getResultadoActualizacionUsuario();
    }

    public LiveData<Boolean> getRegistroExitoso() {
        return usuarioRepository.getRegistroExitoso();
    }

    public LiveData<String> getErrorRegistro() {
        return usuarioRepository.getErrorRegistro();
    }

    public void cargarDatosUsuario() {
        usuarioRepository.cargarDatosUsuario();
    }

    public void actualizarUsuario(String contrasenaActual, String nuevoNombre,
                                  String email, String nuevaContrasena, String urlImagenPerfil) {
        usuarioRepository.actualizarUsuario(
                contrasenaActual,
                nuevoNombre,
                email,
                nuevaContrasena,
                urlImagenPerfil
        );
    }

    public void registrarUsuario(String nombre, String email, String password, String UrlImagenPerfil) {
        usuarioRepository.registrarUsuarioFirebase(email, password);

        getRegistroExitoso().observeForever(success -> {
            if (success) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    usuarioRepository.guardarDatosUsuarioFirestore(
                            user.getUid(),
                            nombre,
                            email,
                            UrlImagenPerfil
                    );
                }
            }
        });
    }


    public void cerrarSesion() {
        usuarioRepository.cerrarSesion();
    }

    public String getEmailUsuario(Map<String, String> userData) {
        if (userData != null && userData.containsKey("email")) {
            return userData.get("email");
        }
        return null;
    }

    public String getNombreUsuario(Map<String, String> userData) {
        if (userData != null && userData.containsKey("nombre")) {
            return userData.get("nombre");
        }
        return null;
    }

    public String getUrlImagenPerfilUsuario(Map<String, String> userData) {
        if (userData != null && userData.containsKey("urlImagen")) {
            return userData.get("urlImagen");
        }
        return null;
    }
}