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
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.retroplay.clases.Juego;
import com.example.retroplay.databinding.ViewholderJuegosBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JuegosFragment extends Fragment {

    private FirebaseFirestore db;
    private ArrayList<Juego> listaJuegos;
    private JuegosAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();
        listaJuegos = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_juegos, container, false);

        // Obtener el NavController
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        // Configurar RecyclerView
        RecyclerView juegosRecyclerView = view.findViewById(R.id.juegosRecyclerView);
        juegosRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Crear el adaptador y configurarlo
        adapter = new JuegosAdapter(navController, R.id.action_juegosFragment2_to_detailFragment);
        juegosRecyclerView.setAdapter(adapter);

        // Cargar los juegos desde Firestore
        //loadJuegosFromFirestore();

        return view;
    }

    /*private void loadJuegosFromFirestore() {
        db.collection("Juegos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            listaJuegos.clear();  // Limpiar la lista antes de añadir los nuevos juegos
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                Juego juego = document.toObject(Juego.class);
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
    }*/

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

            // Usar Glide para cargar la imagen
            Glide.with(holder.itemView.getContext())
                    .load(juego.getRutaImagen()) // Cargar la imagen desde la URL
                    .into(holder.binding.imagenJuego); // Establecer la imagen en el ImageView

            // Manejar el clic en un item del RecyclerView
            holder.itemView.setOnClickListener(v -> navegarPantallaDetalle(juego));
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
}
