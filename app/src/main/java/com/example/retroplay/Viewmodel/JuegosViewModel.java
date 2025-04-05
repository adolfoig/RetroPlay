package com.example.retroplay.Viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.retroplay.Repository.JuegosRepository;
import com.example.retroplay.Model.Juego;

import java.util.List;

public class JuegosViewModel extends ViewModel {
    private final JuegosRepository juegosRepository = new JuegosRepository();
    private final MutableLiveData<String> mensajeError = new MutableLiveData<>();
    private final MutableLiveData<List<Juego>> juegos = new MutableLiveData<>();


    public LiveData<List<Juego>> getJuegos() {
        if (juegos.getValue() == null) {
            cargarJuegos();
        }
        return juegos;
    }

    public LiveData<String> getMensajeError() {
        return mensajeError;
    }

    private void cargarJuegos() {
        juegosRepository.getJuegos().observeForever(juegos::setValue);
    }
}