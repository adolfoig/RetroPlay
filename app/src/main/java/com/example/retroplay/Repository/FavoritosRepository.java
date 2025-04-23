package com.example.retroplay.Repository;

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

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private final MutableLiveData<List<Juego>> favoritosLiveData = new MutableLiveData<>();

    public FavoritosRepository() {
        this(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance());
    }

    // Constructor para pruebas (inyección de dependencias)
    public FavoritosRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }

    public void cargarFavoritos() {
        FirebaseUser usuario = auth.getCurrentUser();
        if (usuario == null) return;

        db.collection("Favoritos")
                .whereEqualTo("idUsuario", usuario.getUid())
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

    private void obtenerJuegosPorIds(List<String> listaIdJuegos) {
        db.collection("Juegos")
                .whereIn("id", listaIdJuegos)
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
        FirebaseUser usuario = auth.getCurrentUser();
        if (usuario != null) {
            Map<String, Object> favorito = new HashMap<>();
            favorito.put("idUsuario", usuario.getUid());
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
        FirebaseUser usuario = auth.getCurrentUser();
        if (usuario != null) {
            db.collection("Favoritos")
                    .whereEqualTo("idUsuario", usuario.getUid())
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
        FirebaseUser usuario = auth.getCurrentUser();
        if (usuario == null) {
            if (callback != null) callback.onComplete(false);
            return;
        }

        db.collection("Favoritos")
                .whereEqualTo("idUsuario", usuario.getUid())
                .whereEqualTo("idJuego", juego.getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (callback != null) {
                        boolean esFavorito = task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty();
                        callback.onComplete(esFavorito);
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
