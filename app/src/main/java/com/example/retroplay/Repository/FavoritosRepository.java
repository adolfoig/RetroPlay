package com.example.retroplay.Repository;

import androidx.lifecycle.MutableLiveData;

import com.example.retroplay.Model.Juego;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoritosRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<List<Juego>> favoritosLiveData = new MutableLiveData<>();

    public void cargarFavoritos() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
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

    public void quitarFavorito(String idJuego) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("Favoritos")
                    .whereEqualTo("idUsuario", user.getUid())
                    .whereEqualTo("idJuego", idJuego)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                db.collection("Favoritos").document(document.getId()).delete();
                            }
                        }
                    });
        }
    }

    public MutableLiveData<List<Juego>> getFavoritosLiveData() {
        return favoritosLiveData;
    }
}