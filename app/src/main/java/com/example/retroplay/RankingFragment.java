package com.example.retroplay;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.retroplay.Viewmodel.RankingViewModel;
import com.example.retroplay.Model.Juego;
import com.example.retroplay.databinding.FragmentRankingBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class RankingFragment extends Fragment {

    private FragmentRankingBinding binding;
    private RankingViewModel viewModel;
    private List<Juego> listaJuegos = new ArrayList<>();

    public interface JuegoLoadingCallback {
        void onGamesLoaded(List<Juego> juegos, boolean isEmpty);
        void onError(String message);
    }

    public interface ScoreLoadingCallback {
        void onScoresLoaded(Task<QuerySnapshot> task);
        void onError(String message);
    }

    public interface UserNameCallback {
        void onUserNameLoaded(String nombreUsuario, int puntuacion, int filas, boolean esUsuarioActual);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RankingViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRankingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cargarJuegos();
    }

    private void cargarJuegos() {
        viewModel.cargarJuegos(new JuegoLoadingCallback() {
            @Override
            public void onGamesLoaded(List<Juego> juegos, boolean isEmpty) {
                listaJuegos = juegos;
                if (isEmpty) {
                    binding.tvTituloJuego.setText("Ningún juego seleccionado");
                    limpiarTablaPuntuaciones();
                }
                configurarSpinner();
            }

            @Override
            public void onError(String message) {
                mostrarError(message);
            }
        });
    }

    private void configurarSpinner() {
        List<Juego> listaConHint = new ArrayList<>();
        listaConHint.add(new Juego("", "Selecciona un juego")); // Juego vacío con texto de hint
        listaConHint.addAll(listaJuegos);

        ArrayAdapter<Juego> adapter = new ArrayAdapter<Juego>(
                requireContext(),
                R.layout.spinner_item_selected,
                listaJuegos) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setText(listaJuegos.get(position).getNombre());
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setText(listaJuegos.get(position).getNombre());
                textView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.white));
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerJuegos.setAdapter(adapter);

        binding.spinnerJuegos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    binding.tvTituloJuego.setText("Ningún juego seleccionado");
                    limpiarTablaPuntuaciones();
                } else {
                    Juego juegoSeleccionado = (Juego) parent.getItemAtPosition(position);
                    cargarPuntuaciones(juegoSeleccionado.getId());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void cargarPuntuaciones(String idJuego) {
        if (idJuego == null || idJuego.isEmpty()) {
            mostrarMensaje("Selecciona un juego válido");
            return;
        }

        binding.tvTituloJuego.setText(viewModel.obtenerNombreJuego(idJuego));
        limpiarTablaPuntuaciones();

        viewModel.cargarPuntuaciones(idJuego, new ScoreLoadingCallback() {
            @Override
            public void onScoresLoaded(Task<QuerySnapshot> task) {
                procesarResultadosPuntuaciones(task);
            }

            @Override
            public void onError(String message) {
                mostrarError(message);
            }
        });
    }

    private void procesarResultadosPuntuaciones(Task<QuerySnapshot> task) {
        if (task.getResult() == null || task.getResult().isEmpty()) {
            mostrarMensaje("No hay puntuaciones registradas");
            return;
        }

        int posicion = 1; // Contador para la posición en el ranking
        for (QueryDocumentSnapshot document : task.getResult()) {
            String idUsuario = document.getString("idUsuario");
            Long puntuacionMaxima = document.getLong("puntuacionMaxima");
            int puntuacion = puntuacionMaxima != null ? puntuacionMaxima.intValue() : 0;

            viewModel.obtenerNombreUsuario(idUsuario, puntuacion, posicion, new UserNameCallback() {
                @Override
                public void onUserNameLoaded(String nombreUsuario, int puntuacion, int posicion, boolean esUsuarioActual) {
                    agregarFilaTabla(nombreUsuario, puntuacion, posicion, esUsuarioActual);
                }
            });
            posicion++;
        }
    }

    private void limpiarTablaPuntuaciones() {
        int childCount = binding.tablaPuntuaciones.getChildCount();
        if (childCount > 1) {
            binding.tablaPuntuaciones.removeViews(1, childCount - 1);
        }
    }

    private void agregarFilaTabla(String nombreUsuario, int puntuacion, int posicion, boolean esUsuarioActual) {
        TableRow row = new TableRow(requireContext());

        // Fondo según posición y usuario actual
        int backgroundColor = esUsuarioActual ?
                ContextCompat.getColor(requireContext(), R.color.ranking) :
                posicion % 2 == 0 ?
                        ContextCompat.getColor(requireContext(), android.R.color.white) :
                        ContextCompat.getColor(requireContext(), android.R.color.darker_gray);
        row.setBackgroundColor(backgroundColor);

        // TextView para la posición
        TextView tvPosicion = new TextView(requireContext());
        tvPosicion.setText(String.valueOf(posicion));
        tvPosicion.setPadding(8, 8, 8, 8);
        row.addView(tvPosicion);

        // TextView para el nombre
        TextView tvUsuario = new TextView(requireContext());
        tvUsuario.setText(nombreUsuario);
        tvUsuario.setPadding(8, 8, 8, 8);
        if (esUsuarioActual) tvUsuario.setTypeface(null, Typeface.BOLD);
        row.addView(tvUsuario);

        // TextView para la puntuación
        TextView tvPuntuacion = new TextView(requireContext());
        tvPuntuacion.setText(String.valueOf(puntuacion));
        tvPuntuacion.setPadding(8, 8, 8, 8);
        row.addView(tvPuntuacion);

        binding.tablaPuntuaciones.addView(row);
    }

    private void mostrarError(String mensaje) {
        binding.tvTituloJuego.setText(mensaje);
    }

    private void mostrarMensaje(String mensaje) {
        binding.tvTituloJuego.setText(mensaje);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}