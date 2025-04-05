package com.example.retroplay.Repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class RankingRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public RankingRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public Task<QuerySnapshot> loadGames() {
        return db.collection("Juegos").get();
    }

    public Task<QuerySnapshot> loadScores(String gameId) {
        return db.collection("Puntuaciones")
                .whereEqualTo("idJuego", gameId)
                .orderBy("puntuacionMaxima", Query.Direction.DESCENDING)
                .get();
    }

    public Task<DocumentSnapshot> getUserName(String userId) {
        return db.collection("Usuarios").document(userId).get();
    }
}
