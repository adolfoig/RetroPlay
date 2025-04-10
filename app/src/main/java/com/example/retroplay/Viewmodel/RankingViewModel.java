package com.example.retroplay.Viewmodel;

import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.example.retroplay.RankingFragment;
import com.example.retroplay.Repository.RankingRepository;
import com.example.retroplay.Model.Juego;
import com.example.retroplay.Repository.UsuarioRepository;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class RankingViewModel extends ViewModel {
    private final RankingRepository rankingRepository;
    private final UsuarioRepository usuarioRepository;
    private List<Juego> listaJuegos = new ArrayList<>();

    public RankingViewModel() {
        usuarioRepository = new UsuarioRepository();
        rankingRepository = new RankingRepository();
    }

    public String getCurrentUserId() {
        return rankingRepository.getIdusuarioActual();
    }

    public void cargarJuegos(RankingFragment.JuegoLoadingCallback callback) {
        rankingRepository.cargarJuegos().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                listaJuegos.clear();
                listaJuegos.add(new Juego());  // Elemento por defecto

                for (QueryDocumentSnapshot document : task.getResult()) {
                    Juego juego = document.toObject(Juego.class);
                    if (juego != null && !listaJuegos.contains(juego)) {
                        listaJuegos.add(juego);
                    }
                }

                if (listaJuegos.size() <= 1) {
                    callback.onGamesLoaded(listaJuegos, true);
                } else {
                    callback.onGamesLoaded(listaJuegos, false);
                }
            } else {
                callback.onError("Error al cargar juegos");
            }
        });
    }

    public void cargarPuntuaciones(String idJuego, RankingFragment.ScoreLoadingCallback callback) {
        rankingRepository.cargarPuntuaciones(idJuego).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onScoresLoaded(task);
            } else {
                callback.onError("Error de conexión: " + task.getException().getMessage());
                Log.d("Index", task.getException().getMessage());
            }
        });
    }

    public void obtenerNombreUsuario(String idUsuario, int puntuacion, int filas, RankingFragment.UserNameCallback callback) {
        usuarioRepository.getUsuarioPorId(idUsuario).addOnSuccessListener(documentSnapshot -> {
            String nombreUsuario = documentSnapshot.getString("nombre");
            if (nombreUsuario == null || nombreUsuario.isEmpty()) {
                nombreUsuario = "Usuario de Google";
            }
            callback.onUserNameLoaded(nombreUsuario, puntuacion, filas, idUsuario.equals(getCurrentUserId()));
        }).addOnFailureListener(e -> {
            callback.onUserNameLoaded("Error al obtener nombre", puntuacion, filas, false);
        });
    }

    public String obtenerNombreJuego(String idJuego) {
        for (Juego juego : listaJuegos) {
            if (juego != null && idJuego.equals(juego.getId())) {
                return juego.getNombre();
            }
        }
        return "Juego no encontrado";
    }
}
