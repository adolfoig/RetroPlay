package com.example.retroplay;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.retroplay.clases.Logro;
import com.example.retroplay.databinding.FragmentLogrosBinding;
import com.example.retroplay.databinding.ViewholderLogrosBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogrosFragment extends Fragment {
    private FragmentLogrosBinding binding;
    private List<Logro> listaLogros = new ArrayList<>();
    private LogroAdapter logroAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLogrosBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Configurar RecyclerView
        binding.recyclerViewLogros.setLayoutManager(new LinearLayoutManager(getContext()));
        logroAdapter = new LogroAdapter(listaLogros);
        binding.recyclerViewLogros.setAdapter(logroAdapter);

        // Cargar logros
        cargarLogrosDesdeFireBase();

        return view;
    }

    private void ordenarLogrosPorPuntuacion() {
        Collections.sort(listaLogros, (logro1, logro2) -> Integer.compare(logro1.getPuntuacion(), logro2.getPuntuacion()));
    }

    private void cargarLogrosDesdeFireBase() {
        FirebaseFirestore.getInstance().collection("LogrosDisponibles")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listaLogros.clear();
                        for (DocumentSnapshot doc : task.getResult()) {
                            Logro logro = doc.toObject(Logro.class);
                            if (logro != null) {
                                logro.setId(doc.getId()); // Guardar el ID del documento
                                listaLogros.add(logro);
                            }
                        }
                        ordenarLogrosPorPuntuacion();
                        verificarLogrosObtenidos(); // Verificar después de cargar
                    }
                });
    }

    private void verificarLogrosObtenidos() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = user.getUid();

        // 1. Obtener la puntuación máxima del usuario
        db.collection("Puntuaciones")
                .whereEqualTo("idUsuario", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        int puntuacionMaxima = 0;

                        // Buscar la puntuación máxima del usuario
                        for (DocumentSnapshot doc : task.getResult()) {
                            Long puntuacion = doc.getLong("puntuacionMaxima");
                            if (puntuacion != null && puntuacion > puntuacionMaxima) {
                                puntuacionMaxima = puntuacion.intValue();
                            }
                        }

                        // 2. Verificar qué logros ha obtenido
                        for (Logro logro : listaLogros) {
                            boolean yaObtenido = false;

                            // Primero verificar si ya está marcado como obtenido
                            int finalPuntuacionMaxima = puntuacionMaxima;
                            db.collection("LogrosObtenidos")
                                    .whereEqualTo("idUsuario", userId)
                                    .whereEqualTo("idLogro", logro.getId())
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        if (querySnapshot.isEmpty()) {
                                            // Si no está obtenido, verificar si cumple el requisito
                                            if (finalPuntuacionMaxima >= logro.getPuntuacion()) {
                                                // 3. Guardar en LogrosObtenidos
                                                Map<String, Object> nuevoLogro = new HashMap<>();
                                                nuevoLogro.put("idUsuario", userId);
                                                nuevoLogro.put("idLogro", logro.getId());

                                                db.collection("LogrosObtenidos")
                                                        .add(nuevoLogro)
                                                        .addOnSuccessListener(documentReference -> {
                                                            logro.setObtenido(true);
                                                            logroAdapter.notifyDataSetChanged();
                                                        });
                                            }
                                        } else {
                                            logro.setObtenido(true);
                                        }
                                    });
                        }

                        // Actualizar la UI
                        logroAdapter.notifyDataSetChanged();
                    }
                });
    }

    // Adaptador optimizado
    private class LogroAdapter extends RecyclerView.Adapter<LogroAdapter.LogroViewHolder> {
        private final List<Logro> logros;

        public LogroAdapter(List<Logro> logros) {
            this.logros = logros;
        }

        @NonNull
        @Override
        public LogroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ViewholderLogrosBinding binding = ViewholderLogrosBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new LogroViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull LogroViewHolder holder, int position) {
            Logro logro = listaLogros.get(position);
            holder.binding.textoDescipcion.setText(logro.getDescripcion());

            if (logro.isObtenido()) {
                holder.binding.imagen.setImageResource(R.drawable.medallacoloreada);
            } else {
                holder.binding.imagen.setImageResource(R.drawable.medallablanca);
            }
        }

        @Override
        public int getItemCount() {
            return logros.size();
        }

        class LogroViewHolder extends RecyclerView.ViewHolder {
            ViewholderLogrosBinding binding;

            public LogroViewHolder(ViewholderLogrosBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}