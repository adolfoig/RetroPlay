package com.example.retroplay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
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

import com.example.retroplay.Model.Puntuacion;
import com.example.retroplay.Viewmodel.RankingViewModel;
import com.example.retroplay.Model.Juego;
import com.example.retroplay.databinding.FragmentRankingBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

                if (!isAdded() || getContext() == null) {
                    return;
                }

                listaJuegos = juegos;
                if (isEmpty) {
                    binding.tvTituloJuego.setText(R.string.ningunJuego);
                    limpiarTablaPuntuaciones();
                }
                configurarSpinner();
            }

            @Override
            public void onError(String message) {
                if (isAdded() && getContext() != null) {
                    mostrarError(message);
                }
            }
        });
    }

    private void configurarSpinner() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        ArrayAdapter<Juego> adapter = new ArrayAdapter<Juego>(context, R.layout.spinner_item_selected, listaJuegos) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setText(listaJuegos.get(position).getNombre());
                textView.setTextColor(ContextCompat.getColor(getContext(), R.color.borde_gris));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view == null) {
                    view = LayoutInflater.from(getContext()).inflate(
                            R.layout.spinner_dropbox_item, parent, false);
                }
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setText(listaJuegos.get(position).getNombre());
                textView.setTextColor(ContextCompat.getColor(getContext(), R.color.borde_gris));
                textView.setBackgroundColor(Color.TRANSPARENT);
                return view;
            }
        };

        adapter.setDropDownViewResource(R.layout.spinner_dropbox_item);

        binding.spinnerJuegos.setAdapter(adapter);

        binding.spinnerJuegos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {
                    TextView textView = view.findViewById(android.R.id.text1);
                    if (textView != null) {
                        textView.setTextColor(ContextCompat.getColor(getContext(), R.color.borde_gris));
                    }
                }

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

        List<Puntuacion> puntuaciones = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (QueryDocumentSnapshot document : task.getResult()) {
            String idUsuario = document.getString("idUsuario");
            Long puntuacionMaxima = document.getLong("puntuacionMaxima");
            int puntuacion = puntuacionMaxima != null ? puntuacionMaxima.intValue() : 0;

            CompletableFuture<Void> future = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                future = new CompletableFuture<>();
            }
            futures.add(future);

            CompletableFuture<Void> finalFuture = future;
            viewModel.obtenerNombreUsuario(idUsuario, puntuacion, 0, new UserNameCallback() {
                @Override
                public void onUserNameLoaded(String nombreUsuario, int puntuacion, int posicion, boolean esUsuarioActual) {
                    puntuaciones.add(new Puntuacion(nombreUsuario, puntuacion, esUsuarioActual));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        finalFuture.complete(null);
                    }
                }
            });
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        // Ordenar por puntuación descendente
                        Collections.sort(puntuaciones, (p1, p2) -> p2.getPuntuacion() - p1.getPuntuacion());

                        // Limpiar tabla antes de agregar nuevas filas
                        limpiarTablaPuntuaciones();

                        // Agregar filas ordenadas
                        for (int i = 0; i < puntuaciones.size(); i++) {
                            Puntuacion p = puntuaciones.get(i);
                            agregarFilaTabla(p.getNombreUsuario(), p.getPuntuacion(), i + 1, p.isEsUsuarioActual());
                        }
                    });
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
        int backgroundDrawable;

        if (esUsuarioActual) {
            backgroundDrawable = R.drawable.filas_usuario_actual;
        } else {
            backgroundDrawable = posicion % 2 == 0 ? R.drawable.filas_blancas : R.drawable.filas_grises;
        }

        Drawable roundedBackground = ContextCompat.getDrawable(requireContext(), backgroundDrawable);
        row.setBackground(roundedBackground);

        TextView tvPosicion = new TextView(requireContext());
        tvPosicion.setText(String.valueOf(posicion));
        tvPosicion.setPadding(16, 16, 16, 16);
        row.addView(tvPosicion);

        TextView tvUsuario = new TextView(requireContext());
        tvUsuario.setText(nombreUsuario);
        tvUsuario.setPadding(16, 16, 16, 16);
        if (esUsuarioActual) tvUsuario.setTypeface(null, Typeface.BOLD);
        row.addView(tvUsuario);

        TextView tvPuntuacion = new TextView(requireContext());
        tvPuntuacion.setText(String.valueOf(puntuacion));
        tvPuntuacion.setPadding(16, 16, 16, 16);
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