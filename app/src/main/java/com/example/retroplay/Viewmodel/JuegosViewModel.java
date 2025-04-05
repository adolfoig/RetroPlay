package com.example.retroplay.Viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.retroplay.Repository.JuegosRepository;
import com.example.retroplay.Model.Juego;

import java.util.ArrayList;
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

    public void verificarFavorito(Juego juego, JuegosRepository.RepositoryCallback<Boolean> callback) {
        juegosRepository.verificarEstadoFavorito(juego, callback);
    }

    // JuegosViewModel.java
    public void alternarFavorito(Juego juego) {
        if (juego.isFavorito()) {
            juegosRepository.eliminarFavorito(juego.getId(), success -> {
                if (success) {
                    juego.setFavorito(false);
                    // Notificar cambios
                    List<Juego> currentList = juegos.getValue();
                    if (currentList != null) {
                        juegos.postValue(new ArrayList<>(currentList));
                    }
                } else {
                    mensajeError.postValue("Error al eliminar favorito");
                }
            });
        } else {
            juegosRepository.agregarFavorito(juego.getId(), success -> {
                if (success) {
                    juego.setFavorito(true);
                    // Notificar cambios
                    List<Juego> currentList = juegos.getValue();
                    if (currentList != null) {
                        juegos.postValue(new ArrayList<>(currentList));
                    }
                } else {
                    mensajeError.postValue("Error al agregar favorito");
                }
            });
        }
    }
}