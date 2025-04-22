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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retroplay.Viewmodel.FavoritosViewModel;
import com.example.retroplay.Model.Juego;
import com.example.retroplay.databinding.FragmentFavoritosBinding;
import com.example.retroplay.databinding.ViewholderFavoritosBinding;

import java.util.ArrayList;
import java.util.List;

public class FavoritosFragment extends Fragment {

    private FavoritosViewModel viewModel;
    private FavoritosAdapter adapter;
    private NavController navController;
    private FragmentFavoritosBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(FavoritosViewModel.class);
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

        configurarReyclerView();
        configurarObservers();
    }

    private void configurarReyclerView() {
        binding.recyclerViewFavoritos.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        adapter = new FavoritosAdapter();
        binding.recyclerViewFavoritos.setAdapter(adapter);
    }

    private void configurarObservers() {
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

        viewModel.getJuegoEliminado().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
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
            int viejoSize = listaFavoritos.size();
            int nuevoSize = nuevaLista.size();

            listaFavoritos.clear();
            listaFavoritos.addAll(nuevaLista);

            if (viejoSize == nuevoSize) {
                notifyItemRangeChanged(0, viejoSize);
            } else if (viejoSize < nuevoSize) {
                if (viejoSize > 0) notifyItemRangeChanged(0, viejoSize);
                notifyItemRangeInserted(viejoSize, nuevoSize - viejoSize);
            } else {
                if (nuevoSize > 0) notifyItemRangeChanged(0, nuevoSize);
                notifyItemRangeRemoved(nuevoSize, viejoSize - nuevoSize);
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
                    binding.imagenEstrella.setImageResource(R.drawable.estrellablanca);
                    viewModel.eliminarFavorito(juego);
                });

                itemView.setOnClickListener(v -> viewModel.seleccionarJuego(juego));
                binding.btnJugar.setOnClickListener(v -> navegarAWebView(juego.getId()));
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