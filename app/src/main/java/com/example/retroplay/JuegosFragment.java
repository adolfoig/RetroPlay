package com.example.retroplay;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.retroplay.clases.Juego;
import com.example.retroplay.databinding.ViewholderJuegosBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JuegosFragment extends Fragment {

    private FirebaseFirestore db;
    private ArrayList<Juego> listaJuegos;
    private JuegosAdapter adapter;

    NavController navController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inicializar Firebase Firestore
        db = FirebaseFirestore.getInstance();
        listaJuegos = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        View view = inflater.inflate(R.layout.fragment_juegos, container, false);

        // Obtener el NavController
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        // Configurar RecyclerView
        RecyclerView juegosRecyclerView = view.findViewById(R.id.recyclerViewJuegos);
        juegosRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Crear el adaptador y configurarlo
        adapter = new JuegosAdapter(navController, R.id.action_juegosFragment_to_detailFragment);
        juegosRecyclerView.setAdapter(adapter);

        // Cargar los juegos desde Firestore
        cargarJuegosDesdeFireBase();

        return view;
    }

    private void cargarJuegosDesdeFireBase() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String idUsuario = user.getUid();  // Obtener ID del usuario
            db.collection("Juegos")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null) {
                                listaJuegos.clear();  // Limpiar la lista antes de añadir los nuevos juegos
                                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                    Juego juego = document.toObject(Juego.class);
                                    // Verificar si el juego está en los favoritos
                                    verificarFavorito(juego, idUsuario);  // Verificar si el juego está en favoritos
                                    listaJuegos.add(juego);
                                }

                                // Asegúrate de actualizar el adaptador con la nueva lista
                                if (adapter != null) {
                                    adapter.establecerLista(listaJuegos);
                                }
                            }
                        } else {
                            Toast.makeText(getContext(), "Error al cargar los juegos.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getContext(), "Debes iniciar sesión para ver tus favoritos", Toast.LENGTH_SHORT).show();
        }
    }

    private void verificarFavorito(Juego juego, String idUsuario) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Verificar si el juego ya está en favoritos para el usuario
        db.collection("Favoritos")
                .whereEqualTo("idUsuario", idUsuario)
                .whereEqualTo("idJuego", juego.getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            // Si el juego está en favoritos, actualizar la propiedad "isFavorito" del juego
                            juego.setFavorito(true);  // Establecer que este juego está en favoritos
                        } else {
                            juego.setFavorito(false);  // Si no está en favoritos
                        }

                        // Notificar al adaptador que el estado del juego ha cambiado para actualizar la UI
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Error al verificar favoritos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    class JuegosViewHolder extends RecyclerView.ViewHolder {
        final ViewholderJuegosBinding binding;

        public JuegosViewHolder(ViewholderJuegosBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public class JuegosAdapter extends RecyclerView.Adapter<JuegosViewHolder> {
        List<Juego> listaJuegos;
        private final NavController navController;
        private final int idAction;

        public JuegosAdapter(NavController navController, int idAction) {
            this.navController = navController;
            this.idAction = idAction;
        }

        @NonNull
        @Override
        public JuegosViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new JuegosViewHolder(ViewholderJuegosBinding.inflate(inflater, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull JuegosViewHolder holder, int position) {
            Juego juego = listaJuegos.get(position);
            holder.binding.textNombreJuego.setText(juego.getNombre());

            // Establecer la imagen dependiendo del ID del juego
            if (juego.getId().equals("1")) {
                holder.binding.imagenJuego.setImageResource(R.drawable.pacman);
            } else if (juego.getId().equals("2")) {
                holder.binding.imagenJuego.setImageResource(R.drawable.tetris);
            } else if (juego.getId().equals("3")) {
                holder.binding.imagenJuego.setImageResource(R.drawable.flappybird);
            }

            // Actualizar la estrella dependiendo del estado "favorito" del juego
            if (juego.isFavorito()) {
                holder.binding.imagenEstrella.setImageResource(R.drawable.estrella); // Estrella llena
            } else {
                holder.binding.imagenEstrella.setImageResource(R.drawable.estrellablanca); // Estrella vacía
            }

            // Llamar a aniadirFavorito cuando se hace clic en la estrella
            holder.binding.imagenEstrella.setOnClickListener(v -> aniadirFavorito(juego.getId(), holder.binding.imagenEstrella, v, position));

            // Manejar el clic en un item del RecyclerView
            holder.itemView.setOnClickListener(v -> navegarPantallaDetalle(juego));

            holder.binding.btnJugar.setOnClickListener(v -> navegarAWebView(juego.getId()));
        }

        @Override
        public int getItemCount() {
            return listaJuegos != null ? listaJuegos.size() : 0;
        }

        public void establecerLista(List<Juego> listaJuegos) {
            this.listaJuegos = listaJuegos;
            notifyDataSetChanged();
        }

        private void navegarPantallaDetalle(Juego juego) {
            Bundle args = new Bundle();
            args.putSerializable("juego", juego); // No es necesario hacer un casting a Serializable si ya lo implementa
            navController.navigate(idAction, args); // Navegar a la pantalla de detalle
        }
    }

    private void navegarAWebView(String idJuego) {
        Bundle bundle = new Bundle();
        bundle.putString("idJuego", idJuego);
        // Pasamos la URL del juego
        navController.navigate(R.id.action_juegosFragment_to_jugarJuegoFragment, bundle); // Navegar al fragmento con el WebView
    }

    private void aniadirFavorito(String idJuego, ImageButton imagenEstrella, View v, int position) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String idUsuario = user.getUid();  // Obtener ID del usuario

            // Verificar si el juego ya está en favoritos para el usuario
            db.collection("Favoritos")
                    .whereEqualTo("idUsuario", idUsuario)
                    .whereEqualTo("idJuego", idJuego)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                // Si el juego ya está en favoritos, mostrar un mensaje
                                Toast.makeText(v.getContext(), "Este juego ya está en tus favoritos", Toast.LENGTH_SHORT).show();
                            } else {
                                // Si el juego no está en favoritos, añadirlo
                                Map<String, Object> favorito = new HashMap<>();
                                favorito.put("idUsuario", idUsuario);
                                favorito.put("idJuego", idJuego);

                                // Guardar en Firestore en la colección "Favoritos"
                                db.collection("Favoritos")
                                        .add(favorito)
                                        .addOnSuccessListener(documentReference -> {
                                            Toast.makeText(v.getContext(), "Juego añadido a favoritos", Toast.LENGTH_SHORT).show();
                                            // Actualizar el estado del juego a favorito
                                            imagenEstrella.setImageResource(R.drawable.estrella); // Estrella llena
                                            // Notificar al adaptador que el estado del juego ha cambiado
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(v.getContext(), "Error al añadir favorito", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            Toast.makeText(v.getContext(), "Error al verificar si el juego ya está en favoritos", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(v.getContext(), "Debes iniciar sesión para guardar favoritos", Toast.LENGTH_SHORT).show();
        }
    }
}
