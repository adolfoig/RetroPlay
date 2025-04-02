package com.example.retroplay.Repository;

import androidx.lifecycle.MutableLiveData;

import com.example.retroplay.clases.Juego;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JuegosRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public MutableLiveData<List<Juego>> getJuegos() {
        MutableLiveData<List<Juego>> juegosLiveData = new MutableLiveData<>();
        List<Juego> listaJuegos = new ArrayList<>();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String idUsuario = user.getUid();

            db.collection("Juegos")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Juego juego = document.toObject(Juego.class);
                                verificarFavorito(juego, idUsuario, () -> {
                                    listaJuegos.add(juego);
                                    juegosLiveData.setValue(listaJuegos);
                                });
                            }
                        }
                    });
        }
        return juegosLiveData;
    }

    public void agregarFavorito(String idJuego, RepositoryCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String idUsuario = user.getUid();

            Map<String, Object> favorito = new HashMap<>();
            favorito.put("idUsuario", idUsuario);
            favorito.put("idJuego", idJuego);

            db.collection("Favoritos")  // Corregí el nombre de la colección (antes decía "Favoritos")
                    .add(favorito)
                    .addOnCompleteListener(task -> {
                        if (callback != null) {
                            callback.onComplete(task.isSuccessful());
                        }
                    });
        }
    }

    public void eliminarFavorito(String idJuego, RepositoryCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String idUsuario = user.getUid();

            db.collection("Favoritos")
                    .whereEqualTo("idUsuario", idUsuario)
                    .whereEqualTo("idJuego", idJuego)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot result = task.getResult();
                            if (result != null && !result.isEmpty()) {
                                for (QueryDocumentSnapshot document : result) {
                                    db.collection("Favoritos")
                                            .document(document.getId())
                                            .delete()
                                            .addOnCompleteListener(deleteTask -> {
                                                if (callback != null) {
                                                    callback.onComplete(deleteTask.isSuccessful());
                                                }
                                            });
                                }
                            } else {
                                if (callback != null) {
                                    callback.onComplete(false);
                                }
                            }
                        } else {
                            if (callback != null) {
                                callback.onComplete(false);
                            }
                        }
                    });
        }
    }

    private void verificarFavorito(Juego juego, String idUsuario, Runnable onComplete) {
        db.collection("Favoritos")
                .whereEqualTo("idUsuario", idUsuario)
                .whereEqualTo("idJuego", juego.getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        juego.setFavorito(!task.getResult().isEmpty());
                    }
                    onComplete.run();
                });
    }

    public interface RepositoryCallback<T> {
        void onComplete(boolean success);
    }

    public void verificarEstadoFavorito(Juego juego, RepositoryCallback<Boolean> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onComplete(false);
            return;
        }

        String idUsuario = user.getUid();

        db.collection("Favoritos")
                .whereEqualTo("idUsuario", idUsuario)
                .whereEqualTo("idJuego", juego.getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        if (callback != null) callback.onComplete(true);
                    } else {
                        if (callback != null) callback.onComplete(false);
                    }
                });
    }
}