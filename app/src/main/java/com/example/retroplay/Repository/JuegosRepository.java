package com.example.retroplay.Repository;

import androidx.lifecycle.MutableLiveData;

import com.example.retroplay.Model.Juego;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class JuegosRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public MutableLiveData<List<Juego>> getJuegos() {
        MutableLiveData<List<Juego>> juegosLiveData = new MutableLiveData<>();
        List<Juego> listaJuegos = new ArrayList<>();

        db.collection("Juegos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Juego juego = document.toObject(Juego.class);
                            listaJuegos.add(juego);
                        }
                        juegosLiveData.setValue(listaJuegos);
                    }
                });
        return juegosLiveData;
    }
}