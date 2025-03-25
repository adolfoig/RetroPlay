package com.example.retroplay;

import android.os.Bundle;
import android.util.Log;
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
import com.example.retroplay.clases.Juego;
import com.example.retroplay.databinding.FragmentRankingBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class RankingFragment extends Fragment {

    private FragmentRankingBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String usuarioActualId;  // Almacena el id del usuario actual
    private List<Juego> listaJuegos = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();  // Inicializar FirebaseAuth
        usuarioActualId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
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
        db.collection("Juegos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        listaJuegos.clear();
                        listaJuegos.add(new Juego());  // Añadir elemento por defecto

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Juego juego = document.toObject(Juego.class);
                            if (juego != null && !listaJuegos.contains(juego)) {
                                listaJuegos.add(juego);
                            }
                        }

                        if (listaJuegos.size() <= 1) {  // Si solo contiene el juego por defecto
                            binding.tvTituloJuego.setText("Ningún juego seleccionado");
                            limpiarTablaPuntuaciones();
                        }

                        configurarSpinner();
                    } else {
                        mostrarError("Error al cargar juegos");
                    }
                });
    }

    private void configurarSpinner() {
        ArrayAdapter<Juego> adapter = new ArrayAdapter<Juego>(requireContext(),
                android.R.layout.simple_spinner_item, listaJuegos) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setText(listaJuegos.get(position).getNombre());
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setText(listaJuegos.get(position).getNombre());
                }
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerJuegos.setAdapter(adapter);

        // Listener del spinner para manejar selección
        binding.spinnerJuegos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {  // Si es el juego vacío
                    binding.tvTituloJuego.setText("Ningún juego seleccionado");
                    limpiarTablaPuntuaciones();  // Vaciar tabla
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

        binding.tvTituloJuego.setText(obtenerNombreJuego(idJuego));
        limpiarTablaPuntuaciones();
        consultarPuntuacionesFirestore(idJuego);
    }

    private String obtenerNombreJuego(String idJuego) {
        for (Juego juego : listaJuegos) {
            if (juego != null && idJuego.equals(juego.getId())) {
                return juego.getNombre();
            }
        }
        return "Juego no encontrado";
    }

    private void limpiarTablaPuntuaciones() {
        int childCount = binding.tablaPuntuaciones.getChildCount();
        if (childCount > 1) {
            binding.tablaPuntuaciones.removeViews(1, childCount - 1);
        }
    }

    private void consultarPuntuacionesFirestore(String idJuego) {
        db.collection("Puntuaciones")
                .whereEqualTo("idJuego", idJuego)
                // Mostrar de mayor a menor las puntuaciones
                .orderBy("puntuacionMaxima", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        procesarResultadosPuntuaciones(task);
                    } else {
                        mostrarError("Error de conexión: " + task.getException().getMessage());
                        Log.d("Index", task.getException().getMessage());
                    }
                });
    }

    private void procesarResultadosPuntuaciones(Task<QuerySnapshot> task) {
        if (task.getResult() == null || task.getResult().isEmpty()) {
            mostrarMensaje("No hay puntuaciones registradas");
            return;
        }

        int filas = 0;
        for (QueryDocumentSnapshot document : task.getResult()) {
            String idUsuario = document.getString("idUsuario");
            Long puntuacionMaxima = document.getLong("puntuacionMaxima");
            int puntuacion = puntuacionMaxima != null ? puntuacionMaxima.intValue() : 0;

            obtenerNombreUsuario(idUsuario, puntuacion, filas);
            filas++;
        }
    }

    private void obtenerNombreUsuario(String idUsuario, int puntuacion, int filas) {
        db.collection("Usuarios")
                .document(idUsuario)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String nombreUsuario = documentSnapshot.getString("nombre");
                    if (nombreUsuario == null || nombreUsuario.isEmpty()) {
                        nombreUsuario = "Usuario de Google";
                    }
                    agregarFilaTabla(nombreUsuario, puntuacion, filas, idUsuario.equals(usuarioActualId));
                })
                .addOnFailureListener(e -> {
                    agregarFilaTabla("Error al obtener nombre", puntuacion, filas, false);
                });
    }

    private void agregarFilaTabla(String nombreUsuario, int puntuacion, int filas, boolean esUsuarioActual) {
        TableRow row = new TableRow(requireContext());

        int backgroundColor = esUsuarioActual ?
                ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light) :  // Resaltar en azul claro
                filas % 2 == 0 ?
                        ContextCompat.getColor(requireContext(), android.R.color.white) :
                        ContextCompat.getColor(requireContext(), android.R.color.darker_gray);
        row.setBackgroundColor(backgroundColor);

        TextView tvUsuario = new TextView(requireContext());
        tvUsuario.setText(nombreUsuario);
        tvUsuario.setPadding(8, 8, 8, 8);
        if (esUsuarioActual) tvUsuario.setTypeface(null, android.graphics.Typeface.BOLD);  // Negrita si es el usuario actual
        row.addView(tvUsuario);

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
