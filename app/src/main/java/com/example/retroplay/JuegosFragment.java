package com.example.retroplay;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retroplay.Viewmodel.FavoritosViewModel;
import com.example.retroplay.Viewmodel.JuegosViewModel;
import com.example.retroplay.Model.Juego;
import com.example.retroplay.databinding.ViewholderJuegosBinding;

import java.util.List;

public class JuegosFragment extends Fragment {

    private JuegosViewModel juegosViewModel;
    private FavoritosViewModel favoritosViewModel;
    private JuegosAdapter adapter;
    private NavController navController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        juegosViewModel = new ViewModelProvider(this).get(JuegosViewModel.class);
        favoritosViewModel = new ViewModelProvider(requireActivity()).get(FavoritosViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_juegos, container, false);
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        RecyclerView juegosRecyclerView = view.findViewById(R.id.recyclerViewJuegos);
        juegosRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new JuegosAdapter(navController, R.id.action_juegosFragment_to_detailFragment);
        juegosRecyclerView.setAdapter(adapter);

        observarViewModel();

        return view;
    }

    private void observarViewModel() {
        juegosViewModel.getJuegos().observe(getViewLifecycleOwner(), juegos -> {
            if (juegos != null) {
                adapter.establecerLista(juegos);
            }
        });

        favoritosViewModel.getErrorMessage().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje != null) {
                Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
            }
        });

        // Añade este nuevo observer
        favoritosViewModel.getFavoritoAgregado().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje != null) {
                Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
                favoritosViewModel.favoritoAgregado.setValue(null);
            }
        });

        favoritosViewModel.getJuegoEliminado().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje != null) {
                Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
                favoritosViewModel.juegoEliminado.setValue(null);
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
        private List<Juego> listaJuegos;
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
            ViewholderJuegosBinding binding = ViewholderJuegosBinding.inflate(inflater, parent, false);
            return new JuegosViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull JuegosViewHolder holder, int position) {
            Juego juego = listaJuegos.get(position);
            holder.binding.textNombreJuego.setText(juego.getNombre());

            switch (juego.getId()) {
                case "1":
                    holder.binding.imagenJuego.setImageResource(R.drawable.pacman);
                    break;
                case "2":
                    holder.binding.imagenJuego.setImageResource(R.drawable.tetris);
                    break;
                case "3":
                    holder.binding.imagenJuego.setImageResource(R.drawable.flappybird);
                    break;
            }

            actualizarIconoFavorito(holder.binding.imagenEstrella, juego.isFavorito());

            holder.binding.imagenEstrella.setOnClickListener(v -> {
                favoritosViewModel.alternarFavorito(juego);
                actualizarIconoFavorito(holder.binding.imagenEstrella, !juego.isFavorito());
            });

            holder.itemView.setOnClickListener(v -> navegarPantallaDetalle(juego));
            holder.binding.btnJugar.setOnClickListener(v -> navegarAWebView(juego.getId()));
        }

        @Override
        public int getItemCount() {
            return listaJuegos != null ? listaJuegos.size() : 0;
        }

        public void establecerLista(List<Juego> listaJuegos) {
            this.listaJuegos = listaJuegos;

            for (Juego juego : listaJuegos) {
                favoritosViewModel.verificarEstadoFavorito(juego, esFavorito -> {
                    juego.setFavorito(esFavorito);
                    notifyDataSetChanged();
                });
            }
        }

        private void navegarPantallaDetalle(Juego juego) {
            Bundle args = new Bundle();
            args.putSerializable("juego", juego);
            navController.navigate(idAction, args);
        }

        private void actualizarIconoFavorito(ImageButton imagenEstrella, boolean esFavorito) {
            if (esFavorito) {
                imagenEstrella.setImageResource(R.drawable.estrella);
            } else {
                imagenEstrella.setImageResource(R.drawable.estrellablanca);
            }
        }
    }

    private void navegarAWebView(String idJuego) {
        Bundle bundle = new Bundle();
        bundle.putString("idJuego", idJuego);
        navController.navigate(R.id.action_juegosFragment_to_jugarJuegoFragment, bundle);
    }
}