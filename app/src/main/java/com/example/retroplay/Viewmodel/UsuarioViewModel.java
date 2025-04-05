package com.example.retroplay.Viewmodel;

import androidx.lifecycle.ViewModel;

import com.example.retroplay.Repository.UsuarioRepository;

import java.util.Map;

public class UsuarioViewModel extends ViewModel {
    private final UsuarioRepository usuarioRepository;


    public UsuarioViewModel() {
        this.usuarioRepository = new UsuarioRepository();
    }

    public UsuarioRepository getUsuarioRepository() {
        return usuarioRepository;
    }

    public String getUserEmail(Map<String, String> userData) {
        if (userData != null && userData.containsKey("email")) {
            return userData.get("email");
        }
        return null;
    }
}
