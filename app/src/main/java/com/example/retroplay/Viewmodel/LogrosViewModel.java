package com.example.retroplay.Viewmodel;


import androidx.lifecycle.ViewModel;

import com.example.retroplay.LogrosFragment;
import com.example.retroplay.Repository.LogrosRepository;
import com.example.retroplay.Model.Logro;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LogrosViewModel extends ViewModel {
    private final LogrosRepository logrosRepository;
    private List<Logro> listaLogros = new ArrayList<>();

    public LogrosViewModel() {
        logrosRepository = new LogrosRepository();
    }

    public void cargarLogrosDesdeFireBase(LogrosFragment.LogrosCallback callback) {
        logrosRepository.getLogrosDisponibles().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                listaLogros.clear();
                for (DocumentSnapshot doc : task.getResult()) {
                    Logro logro = doc.toObject(Logro.class);
                    if (logro != null && logro.getDescripcion() != null && !logro.getDescripcion().isEmpty()) {
                        logro.setId(doc.getId());
                        listaLogros.add(logro);
                    }
                }
                ordenarLogrosPorPuntuacion();
                verificarLogrosObtenidos(callback);
            } else {
                callback.onError("Error al cargar logros");
            }
        });
    }

    private void ordenarLogrosPorPuntuacion() {
        Collections.sort(listaLogros, (logro1, logro2) -> Integer.compare(logro1.getPuntuacion(), logro2.getPuntuacion()));
    }

    private void verificarLogrosObtenidos(LogrosFragment.LogrosCallback callback) {
        FirebaseUser usuario = logrosRepository.getUsuarioActual();

        // Add null check for user
        if (usuario == null) {
            for (Logro logro : listaLogros) {
                logro.setObtenido(false);
            }
            callback.onLogrosCargados(listaLogros);
            return;
        }

        logrosRepository.getLogrosObtenidos(usuario.getUid()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> logrosObtenidosIds = new ArrayList<>();
                for (DocumentSnapshot doc : task.getResult()) {
                    logrosObtenidosIds.add(doc.getString("idLogro"));
                }

                for (Logro logro : listaLogros) {
                    logro.setObtenido(logrosObtenidosIds.contains(logro.getId()));
                }

                callback.onLogrosCargados(listaLogros);
            } else {
                // Handle error case
                callback.onError("Error al verificar logros obtenidos");
            }
        });
    }
}