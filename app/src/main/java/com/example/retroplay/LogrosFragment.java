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

import com.example.retroplay.clases.Logro;
import com.example.retroplay.databinding.FragmentLogrosBinding;
import com.example.retroplay.databinding.ViewholderLogrosBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        // Configurar el adaptador
        logroAdapter = new LogroAdapter(listaLogros);
        binding.recyclerViewLogros.setAdapter(logroAdapter);

        // Cargar logros desde Firebase
        cargarLogrosDesdeFireBase();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Evita memory leaks
    }

    private void cargarLogrosDesdeFireBase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("LogrosDisponibles")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            listaLogros.clear();  // Limpiar la lista antes de añadir los nuevos logros
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                Logro logro = document.toObject(Logro.class);
                                if (logro != null) {
                                    listaLogros.add(logro);
                                }
                            }

                            // Ordenar la lista localmente por puntuacionRequerida (convertida a int)
                            ordenarLogrosPorPuntuacion();

                            // Notificar al adaptador que los datos han cambiado
                            logroAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(getContext(), "Error al cargar los logros.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void ordenarLogrosPorPuntuacion() {
        Collections.sort(listaLogros, (logro1, logro2) -> {
            int puntuacion1 = Integer.parseInt(logro1.getPuntuacion());
            int puntuacion2 = Integer.parseInt(logro2.getPuntuacion());
            return Integer.compare(puntuacion1, puntuacion2); // Ordenar de menor a mayor
        });
    }

        // Adaptador con ViewBinding
    private static class LogroAdapter extends RecyclerView.Adapter<LogroAdapter.LogroViewHolder> {

        private final List<Logro> logrosList;

        public LogroAdapter(List<Logro> logrosList) {
            this.logrosList = logrosList;
        }

        @NonNull
        @Override
        public LogroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ViewholderLogrosBinding binding = ViewholderLogrosBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new LogroViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull LogroViewHolder holder, int position) {
            Logro logro = logrosList.get(position);
            holder.binding.textoDescipcion.setText(logro.getDescripcion());
            holder.binding.imagen.setImageResource(R.drawable.medallablanca);
        }

        @Override
        public int getItemCount() {
            return logrosList.size();
        }

        // ViewHolder con ViewBinding
        static class LogroViewHolder extends RecyclerView.ViewHolder {
            private final ViewholderLogrosBinding binding;

            public LogroViewHolder(@NonNull ViewholderLogrosBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}