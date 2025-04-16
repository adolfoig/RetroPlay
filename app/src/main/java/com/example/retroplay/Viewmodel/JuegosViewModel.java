package com.example.retroplay.Viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.retroplay.Model.Juego;
import com.example.retroplay.Repository.JuegosRepository;

import java.util.List;

public class JuegosViewModel extends ViewModel {
    private final JuegosRepository repository = new JuegosRepository();
    private final MutableLiveData<List<Juego>> juegosLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> scoreLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveScoreLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

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
    public void fetchScore(String gameId) {
        repository.fetchScore(gameId, scoreLiveData, errorLiveData);
    }

    public LiveData<Integer> getScore() {
        return scoreLiveData;
    }

    public LiveData<Boolean> getSaveScoreResult() {
        return saveScoreLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanup();
    }
}