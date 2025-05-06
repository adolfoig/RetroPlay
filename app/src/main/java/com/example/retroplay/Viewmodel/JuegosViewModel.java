package com.example.retroplay.Viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.retroplay.Model.Juego;
import com.example.retroplay.Repository.JuegosRepository;

import java.util.List;

public class JuegosViewModel extends AndroidViewModel {
    private final JuegosRepository repository = new JuegosRepository();
    private final MutableLiveData<List<Juego>> juegosLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> puntuacion = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public JuegosViewModel(@NonNull Application application) {
        super(application);
    }

    // Métodos existentes para juegos
    public LiveData<List<Juego>> getJuegos() {
        if (juegosLiveData.getValue() == null) {
            loadJuegos();
        }
        return juegosLiveData;
    }

    private void loadJuegos() {
        repository.getJuegos().observeForever(juegosLiveData::setValue);
    }

    // Nuevos métodos para puntuaciones
    public void fetchScore(String idJuego) {
        repository.obtenerPuntuacion(idJuego, puntuacion, errorLiveData);
    }

    public LiveData<Integer> getScore() {
        return puntuacion;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    @Override
    public void onCleared() {
        super.onCleared();
        repository.cleanup();
    }
}