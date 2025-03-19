package com.example.retroplay;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.retroplay.clases.Juego;
import com.example.retroplay.databinding.FragmentFavoritosBinding;
import com.example.retroplay.databinding.ViewholderJuegosBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoritosFragment extends Fragment {

    private FirebaseFirestore db;
    private ArrayList<Juego> listaFavoritos;
    private FavoritosAdapter adapter;
    private NavController navController;
    private FragmentFavoritosBinding binding; // Usamos FragmentFavoritosBinding

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        listaFavoritos = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflar el layout con ViewBinding
        binding = FragmentFavoritosBinding.inflate(inflater, container, false);

        // Obtener el NavController
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        // Configurar RecyclerView usando binding
        binding.recyclerViewFavoritos.setLayoutManager(new GridLayoutManager(getContext(), 2));  // 2 juegos por fila

        // Crear el adaptador y configurarlo
        adapter = new FavoritosAdapter();
        binding.recyclerViewFavoritos.setAdapter(adapter);

        // Cargar los favoritos desde Firestore
        cargarFavoritosDesdeFireBase();

        return binding.getRoot();  // Usar binding para retornar la vista
    }

    private void cargarFavoritosDesdeFireBase() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String idUsuario = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        if (!idUsuario.isEmpty()) {
            db.collection("Favoritos")
                    .whereEqualTo("idUsuario", idUsuario)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null) {
                                ArrayList<String> idJuegosFavoritos = new ArrayList<>();
                                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                    String idJuego = document.getString("idJuego");
                                    if (idJuego != null && !idJuego.isEmpty()) {
                                        idJuegosFavoritos.add(idJuego);
                                    }
                                }

                                // Ahora que tenemos todos los idJuego de los favoritos, obtenemos los juegos
                                if (!idJuegosFavoritos.isEmpty()) {
                                    cargarJuegosPorId(idJuegosFavoritos); // Pasamos la lista de idJuego para obtener los juegos
                                }
                            }
                        } else {
                            Toast.makeText(getContext(), "Error al cargar los favoritos.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void cargarJuegosPorId(List<String> idJuegosFavoritos) {
        db.collection("Juegos")
                .whereIn("id", idJuegosFavoritos)  // Consultar los juegos con los idJuego favoritos
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            listaFavoritos.clear();
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                Juego juego = document.toObject(Juego.class);
                                if (juego != null) {
                                    listaFavoritos.add(juego);
                                }
                            }
                            adapter.establecerLista(listaFavoritos);  // Actualizamos el adaptador con los juegos favoritos
                        }
                    } else {
                        Toast.makeText(getContext(), "Error al cargar los juegos.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // Adaptador para mostrar los juegos favoritos
    class FavoritosAdapter extends RecyclerView.Adapter<FavoritosAdapter.FavoritosViewHolder> {

        private List<Juego> listaFavoritos = new ArrayList<>();

        @NonNull
        @Override
        public FavoritosViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new FavoritosViewHolder(ViewholderJuegosBinding.inflate(inflater, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull FavoritosViewHolder holder, int position) {
            Juego juego = listaFavoritos.get(position);
            holder.binding.textNombreJuego.setText(juego.getNombre());
            // Usar Glide para cargar la imagen desde URL si está disponible
            if (juego.getId().equals("1")) {
                holder.binding.imagenJuego.setImageResource(R.drawable.pacman);
            } else if (juego.getId().equals("2")) {
                holder.binding.imagenJuego.setImageResource(R.drawable.tetris);
            } else if (juego.getId().equals("3")) {
                holder.binding.imagenJuego.setImageResource(R.drawable.flappybird);
            }

            // Manejar el clic en un item del RecyclerView
            holder.itemView.setOnClickListener(v -> navegarPantallaDetalle(juego));

            holder.binding.btnJugar.setOnClickListener(v -> navegarAWebView(juego.getId()));
        }

        @Override
        public int getItemCount() {
            return listaFavoritos != null ? listaFavoritos.size() : 0;
        }

        public void establecerLista(List<Juego> listaFavoritos) {
            this.listaFavoritos = listaFavoritos;
            notifyDataSetChanged();  // Notificar a RecyclerView que los datos han cambiado
        }

        class FavoritosViewHolder extends RecyclerView.ViewHolder {
            final ViewholderJuegosBinding binding;

            public FavoritosViewHolder(ViewholderJuegosBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        private void navegarPantallaDetalle(Juego juego) {
            Bundle args = new Bundle();
            args.putSerializable("juego", juego); // No es necesario hacer un casting a Serializable si ya lo implementa
            navController.navigate(R.id.action_favoritosFragment_to_detailFragment, args); // Navegar a la pantalla de detalle
        }

        private void navegarAWebView(String idJuego) {
            Bundle bundle = new Bundle();
            bundle.putString("idJuego", idJuego);
            navController.navigate(R.id.action_favoritosFragment_to_jugarJuegoFragment, bundle); // Navegar al fragmento con el WebView
        }
    }
}
