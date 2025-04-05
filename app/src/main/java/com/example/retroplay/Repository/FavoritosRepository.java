package com.example.retroplay.Repository;

import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;

import com.example.retroplay.Model.Juego;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoritosRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final MutableLiveData<List<Juego>> favoritosLiveData = new MutableLiveData<>();

    public void cargarFavoritos() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("Favoritos")
                .whereEqualTo("idUsuario", user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    List<String> idJuegos = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        String idJuego = doc.getString("idJuego");
                        if (idJuego != null) idJuegos.add(idJuego);
                    }

                    if (!idJuegos.isEmpty()) {
                        obtenerJuegosPorIds(idJuegos);
                    } else {
                        favoritosLiveData.setValue(new ArrayList<>());
                    }
                });
    }

    private void obtenerJuegosPorIds(List<String> idJuegos) {
        db.collection("Juegos")
                .whereIn("id", idJuegos)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Juego> juegos = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            Juego juego = doc.toObject(Juego.class);
                            if (juego != null) juegos.add(juego);
                        }
                        favoritosLiveData.setValue(juegos);
                    }
                });
    }

    public void agregarFavorito(String idJuego, RepositoryCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            Map<String, Object> favorito = new HashMap<>();
            favorito.put("idUsuario", user.getUid());
            favorito.put("idJuego", idJuego);

            db.collection("Favoritos")
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
            db.collection("Favoritos")
                    .whereEqualTo("idUsuario", user.getUid())
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

    public void verificarEstadoFavorito(Juego juego, RepositoryCallback<Boolean> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onComplete(false);
            return;
        }

        db.collection("Favoritos")
                .whereEqualTo("idUsuario", user.getUid())
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

    public MutableLiveData<List<Juego>> getFavoritosLiveData() {
        return favoritosLiveData;
    }

    public interface RepositoryCallback<T> {
        void onComplete(boolean success);
    }
}