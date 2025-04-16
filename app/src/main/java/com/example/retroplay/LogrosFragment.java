package com.example.retroplay;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retroplay.Viewmodel.LogrosViewModel;
import com.example.retroplay.Model.Logro;
import com.example.retroplay.databinding.FragmentLogrosBinding;
import com.example.retroplay.databinding.ViewholderLogrosBinding;

import java.util.ArrayList;
import java.util.List;

public class LogrosFragment extends Fragment {
    private FragmentLogrosBinding binding;
    private LogrosViewModel viewModel;
    private LogroAdapter logroAdapter;

    public interface LogrosCallback {
        void onLogrosCargados(List<Logro> logros);
        void onError(String message);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLogrosBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        viewModel = new ViewModelProvider(this).get(LogrosViewModel.class);

        binding.recyclerViewLogros.setLayoutManager(new LinearLayoutManager(getContext()));
        logroAdapter = new LogroAdapter(new ArrayList<>());
        binding.recyclerViewLogros.setAdapter(logroAdapter);

        cargarLogrosDesdeFireBase();

        return view;
    }

    private void cargarLogrosDesdeFireBase() {
        viewModel.cargarLogrosDesdeFireBase(new LogrosCallback() {
            @Override
            public void onLogrosCargados(List<Logro> logros) {
                logroAdapter = new LogroAdapter(logros);
                binding.recyclerViewLogros.setAdapter(logroAdapter);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

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

        @SuppressLint("ResourceAsColor")
        @Override
        public void onBindViewHolder(@NonNull LogroViewHolder holder, int position) {
            Logro logro = logros.get(position);
            holder.binding.textoDescripcion.setText(logro.getDescripcion());

            if (logro.getDescripcion().contains("PACMAN")) {
                holder.binding.getRoot().setBackgroundResource(R.drawable.logro_pacman);
            } else if (logro.getDescripcion().contains("TETRIS")) {
                holder.binding.getRoot().setBackgroundResource(R.drawable.logro_tetris);
            } else if (logro.getDescripcion().contains("FLAPPY BIRD")) {
                holder.binding.getRoot().setBackgroundResource(R.drawable.logro_flappybird);
            }

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