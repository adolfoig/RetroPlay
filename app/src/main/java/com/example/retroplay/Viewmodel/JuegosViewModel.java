package com.example.retroplay.Viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.retroplay.Model.Juego;
import com.example.retroplay.Repository.JuegosRepository;
import com.example.retroplay.SingleLiveEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JuegosViewModel extends AndroidViewModel {
    private final JuegosRepository repository = new JuegosRepository();
    private final MutableLiveData<List<Juego>> juegosLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> puntuacion = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MutableLiveData<String> servidorError = new MutableLiveData<>();

    public final SingleLiveEvent<Boolean> puntuacionGuardada = new SingleLiveEvent<>();
    public LiveData<Boolean> getPuntuacionGuardada() {
        return puntuacionGuardada;
    }



    public LiveData<String> getServidorError() {
        return servidorError;
    }

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
        Log.d("ViewModel", "Iniciando fetchScore...");

        servidorError.postValue(null);
        puntuacionGuardada.postValue(null);

        executorService.execute(() -> {
            try {
                String result = repository.cargarPuntuacionDelServidor();
                Log.d("ViewModel", "Respuesta del servidor: " + result);

                if (result == null || result.isEmpty()) {
                    servidorError.postValue("Error de conexión con el servidor");
                    return;
                }
                JSONObject jsonObject = new JSONObject(result);
                int score = jsonObject.getInt("score");
                puntuacion.postValue(score);
                guardarPuntuacion(idJuego, score);    } catch (Exception e) {
                Log.e("ViewModel", "Error en fetchScore", e);
                servidorError.postValue("Error interno");
            }
        });
    }


    private void guardarPuntuacion(String idJuego, int score) {
        repository.guardarPuntuacion(idJuego, score, new MutableLiveData<Boolean>() {
            @Override
            public void postValue(Boolean value) {
                super.postValue(value);
                puntuacionGuardada.postValue(value);
            }
        });
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
