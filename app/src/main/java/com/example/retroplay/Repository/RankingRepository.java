package com.example.retroplay.Repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class RankingRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth firebaseAuth;

    public RankingRepository() {
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public String getIdUsuarioActual() {
        return firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : null;
    }

    public Task<QuerySnapshot> cargarJuegos() {
        return db.collection("Juegos").get();
    }

    public Task<QuerySnapshot> cargarPuntuaciones(String idJuego) {
        return db.collection("Puntuaciones")
                .whereEqualTo("idJuego", idJuego)
                .orderBy("puntuacionMaxima", Query.Direction.DESCENDING) // <- Orden descendente
                .get();
    }

    public Task<DocumentSnapshot> getUsuarioPorId(String idUsuario) {
        return db.collection("Usuarios").document(idUsuario).get();
    }
}
