package com.example.retroplay;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retroplay.Viewmodel.FavoritosViewModel;
import com.example.retroplay.clases.Juego;
import com.example.retroplay.databinding.FragmentFavoritosBinding;
import com.example.retroplay.databinding.ViewholderFavoritosBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoritosFragment extends Fragment {

    private FavoritosViewModel viewModel;
    private FavoritosAdapter adapter;
    private NavController navController;
    private FragmentFavoritosBinding binding;
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(FavoritosViewModel.class);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFavoritosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        setupRecyclerView();
        setupObservers();
    }

    private void setupRecyclerView() {
        // Configurar layout y animaciones
        binding.recyclerViewFavoritos.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(200);
        animator.setRemoveDuration(200);
        animator.setChangeDuration(150);
        binding.recyclerViewFavoritos.setItemAnimator(animator);

        adapter = new FavoritosAdapter();
        binding.recyclerViewFavoritos.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getFavoritos().observe(getViewLifecycleOwner(), juegos -> {
            if (juegos != null) {
                adapter.establecerLista(juegos);
                binding.textoListaVacia.setVisibility(juegos.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getJuegoSeleccionado().observe(getViewLifecycleOwner(), juego -> {
            if (juego != null) {
                navegarADetalle(juego);
                viewModel.seleccionarJuego(null);
            }
        });

        viewModel.getJuegoEliminado().observe(getViewLifecycleOwner(), idJuego -> {
            if (idJuego != null) {
                Toast.makeText(requireContext(), "Juego eliminado de favoritos", Toast.LENGTH_SHORT).show();
                viewModel.juegoEliminado.setValue(null);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                viewModel.errorMessage.setValue(null);
            }
        });
    }

    private void navegarADetalle(Juego juego) {
        Bundle args = new Bundle();
        args.putSerializable("juego", juego);
        navController.navigate(R.id.action_favoritosFragment_to_detailFragment, args);
    }

    class FavoritosAdapter extends RecyclerView.Adapter<FavoritosAdapter.FavoritosViewHolder> {
        private List<Juego> listaFavoritos = new ArrayList<>();

        @NonNull
        @Override
        public FavoritosViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ViewholderFavoritosBinding binding = ViewholderFavoritosBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new FavoritosViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull FavoritosViewHolder holder, int position) {
            holder.bind(listaFavoritos.get(position));
        }

        @Override
        public int getItemCount() {
            return listaFavoritos.size();
        }

        public void establecerLista(List<Juego> nuevaLista) {
            // Actualización optimizada sin DiffUtil
            int oldSize = listaFavoritos.size();
            int newSize = nuevaLista.size();

            listaFavoritos.clear();
            listaFavoritos.addAll(nuevaLista);

            if (oldSize == newSize) {
                notifyItemRangeChanged(0, oldSize);
            } else if (oldSize < newSize) {
                if (oldSize > 0) notifyItemRangeChanged(0, oldSize);
                notifyItemRangeInserted(oldSize, newSize - oldSize);
            } else {
                if (newSize > 0) notifyItemRangeChanged(0, newSize);
                notifyItemRangeRemoved(newSize, oldSize - newSize);
            }
        }

        class FavoritosViewHolder extends RecyclerView.ViewHolder {
            final ViewholderFavoritosBinding binding;

            FavoritosViewHolder(ViewholderFavoritosBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(Juego juego) {
                binding.textNombreJuego.setText(juego.getNombre());

                // Configurar imagen según ID
                switch (juego.getId()) {
                    case "1":
                        binding.imagenJuego.setImageResource(R.drawable.pacman);
                        break;
                    case "2":
                        binding.imagenJuego.setImageResource(R.drawable.tetris);
                        break;
                    case "3":
                        binding.imagenJuego.setImageResource(R.drawable.flappybird);
                        break;
                }

                binding.imagenEstrella.setImageResource(R.drawable.estrella);
                binding.imagenEstrella.setOnClickListener(v -> {
                    // Cambio visual inmediato
                    binding.imagenEstrella.setImageResource(R.drawable.estrellablanca);
                    quitarFavorito(juego.getId());
                });

                itemView.setOnClickListener(v -> viewModel.seleccionarJuego(juego));
                binding.btnJugar.setOnClickListener(v -> navegarAWebView(juego.getId()));
            }
        }

        private void quitarFavorito(String idJuego) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                db.collection("Favoritos")
                        .whereEqualTo("idUsuario", user.getUid())
                        .whereEqualTo("idJuego", idJuego)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    db.collection("Favoritos").document(document.getId()).delete()
                                            .addOnSuccessListener(aVoid -> viewModel.quitarFavorito(idJuego))
                                            .addOnFailureListener(e -> Toast.makeText(
                                                    requireContext(),
                                                    "Error al eliminar favorito",
                                                    Toast.LENGTH_SHORT).show());
                                }
                            }
                        });
            }
        }
    }

    private void navegarAWebView(String idJuego) {
        Bundle bundle = new Bundle();
        bundle.putString("idJuego", idJuego);
        navController.navigate(R.id.action_favoritosFragment_to_jugarJuegoFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}