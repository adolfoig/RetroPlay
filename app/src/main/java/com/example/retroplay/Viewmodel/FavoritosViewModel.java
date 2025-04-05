package com.example.retroplay.Viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.retroplay.Repository.FavoritosRepository;
import com.example.retroplay.clases.Juego;

import java.util.List;

public class FavoritosViewModel extends ViewModel {
    private final FavoritosRepository repository = new FavoritosRepository();
    private final MutableLiveData<List<Juego>> favoritosLiveData = new MutableLiveData<>();
    private final MutableLiveData<Juego> juegoSeleccionado = new MutableLiveData<>();
    private final MutableLiveData<String> idJuegoParaJugar = new MutableLiveData<>();
    public final MutableLiveData<String> juegoEliminado = new MutableLiveData<>();
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public FavoritosViewModel() {
        cargarFavoritos();
    }

    public void cargarFavoritos() {
        repository.cargarFavoritos();
    }

    public void quitarFavorito(String idJuego) {
        repository.quitarFavorito(idJuego);
    }

    public void seleccionarJuego(Juego juego) {
        juegoSeleccionado.setValue(juego);
    }

    public void prepararJuegoParaJugar(String idJuego) {
        idJuegoParaJugar.setValue(idJuego);
    }

    // Getters para LiveData
    public LiveData<List<Juego>> getFavoritos() { return repository.getFavoritosLiveData(); }
    public LiveData<Juego> getJuegoSeleccionado() { return juegoSeleccionado; }
    public LiveData<String> getIdJuegoParaJugar() { return idJuegoParaJugar; }
    public LiveData<String> getJuegoEliminado() { return juegoEliminado; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
}