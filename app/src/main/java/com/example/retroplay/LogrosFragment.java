package com.example.retroplay;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.retroplay.clases.Logro;
import com.example.retroplay.databinding.FragmentLogrosBinding;
import com.example.retroplay.databinding.ViewholderLogrosBinding;

import java.util.ArrayList;
import java.util.List;

public class LogrosFragment extends Fragment {

    private FragmentLogrosBinding binding;
    private List<Logro> logrosList;
    private LogroAdapter logroAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLogrosBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Configurar RecyclerView
        binding.recyclerViewLogros.setLayoutManager(new LinearLayoutManager(getContext()));

        // Crear lista de logros
        logrosList = new ArrayList<>();
        logrosList.add(new Logro("Conseguir 50 puntos en PACMAN", R.drawable.medallablanca));
        logrosList.add(new Logro("Conseguir 150 puntos en PACMAN", R.drawable.medallablanca));
        logrosList.add(new Logro("Conseguir 250 puntos en PACMAN", R.drawable.medallablanca));

        // Configurar el adaptador
        logroAdapter = new LogroAdapter(logrosList);
        binding.recyclerViewLogros.setAdapter(logroAdapter);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Evita memory leaks
    }

    // Clase interna para representar un logro

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
            holder.binding.imagen.setImageResource(logro.getImagenMedalla());
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