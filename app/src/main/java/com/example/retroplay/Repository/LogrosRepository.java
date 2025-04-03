package com.example.retroplay.Repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class LogrosRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public LogrosRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public Task<QuerySnapshot> getLogrosDisponibles() {
        return db.collection("LogrosDisponibles").get();
    }

    public Task<QuerySnapshot> getLogrosObtenidos(String userId) {
        return db.collection("LogrosObtenidos")
                .whereEqualTo("idUsuario", userId)
                .get();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
}
