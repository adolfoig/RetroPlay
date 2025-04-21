package com.example.retroplay.Repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;

public class LogrosRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth firebaseAuth;

    public LogrosRepository() {
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public Task<QuerySnapshot> getLogrosDisponibles() {
        return db.collection("LogrosDisponibles").get();
    }

    public Task<QuerySnapshot> getLogrosObtenidos(String idUsuario) {
        return db.collection("LogrosObtenidos")
                .whereEqualTo("idUsuario", idUsuario)
                .get();
    }

    public FirebaseUser getUsuarioActual() {
        return firebaseAuth.getCurrentUser();
    }
}
