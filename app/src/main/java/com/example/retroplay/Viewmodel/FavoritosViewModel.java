package com.example.retroplay.Viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.retroplay.Repository.FavoritosRepository;
import com.example.retroplay.Model.Juego;

import java.util.List;

public class FavoritosViewModel extends ViewModel {
    private final FavoritosRepository favoritosRepository = new FavoritosRepository();
    private final MutableLiveData<Juego> juegoSeleccionado = new MutableLiveData<>();
    public final MutableLiveData<String> juegoEliminado = new MutableLiveData<>();
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public final MutableLiveData<String> favoritoAgregado = new MutableLiveData<>();


    public FavoritosViewModel() {
        cargarFavoritos();
    }

    public void cargarFavoritos() {
        favoritosRepository.cargarFavoritos();
    }

    public void eliminarFavorito(Juego juego) {
        favoritosRepository.eliminarFavorito(juego.getId(), success -> {
            if (success) {
                juego.setFavorito(false);
                juegoEliminado.postValue("Juego eliminado de favoritos"); // Esto activará el Toast en el Fragment
            } else {
                errorMessage.postValue("Error al eliminar favorito");
            }
        });
    }

    public void alternarFavorito(Juego juego) {
        if (juego.isFavorito()) {
            favoritosRepository.eliminarFavorito(juego.getId(), success -> {
                if (success) {
                    juego.setFavorito(false);
                    juegoEliminado.postValue("Juego eliminado de favoritos");
                } else {
                    errorMessage.postValue("Error al eliminar favorito");
                }
            });
        } else {
            favoritosRepository.agregarFavorito(juego.getId(), success -> {
                if (success) {
                    juego.setFavorito(true);
                    favoritoAgregado.postValue("Juego añadido a favoritos");
                } else {
                    errorMessage.postValue("Error al agregar favorito");
                }
            });
        }
    }

    public void verificarEstadoFavorito(Juego juego, FavoritosRepository.RepositoryCallback<Boolean> callback) {
        favoritosRepository.verificarEstadoFavorito(juego, callback);
    }

    public void seleccionarJuego(Juego juego) {
        juegoSeleccionado.setValue(juego);
    }

    // Getters para LiveData
    public LiveData<List<Juego>> getFavoritos() { return favoritosRepository.getFavoritosLiveData(); }
    public LiveData<Juego> getJuegoSeleccionado() { return juegoSeleccionado; }
    public LiveData<String> getJuegoEliminado() { return juegoEliminado; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getFavoritoAgregado() { return favoritoAgregado; }
}