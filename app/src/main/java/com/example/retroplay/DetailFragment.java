package com.example.retroplay;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.retroplay.Model.Juego;
import com.example.retroplay.Viewmodel.FavoritosViewModel;
import com.example.retroplay.databinding.FragmentDetailBinding;

public class DetailFragment extends Fragment {

    FragmentDetailBinding binding;
    FavoritosViewModel favoritosViewModel;
    Juego juego;
    NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            juego = (Juego) getArguments().getSerializable("juego");
        }

        favoritosViewModel = new ViewModelProvider(requireActivity()).get(FavoritosViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDetailBinding.inflate(inflater, container, false);
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        configurarObservadoresViewModel();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navController = Navigation.findNavController(view);

        actualizarIconoFavorito(binding.imagenEstrella, juego.isFavorito());

        favoritosViewModel.verificarEstadoFavorito(juego, esFavorito -> {
            juego.setFavorito(esFavorito);
            actualizarIconoFavorito(binding.imagenEstrella, esFavorito);
        });

        binding.imagenEstrella.setOnClickListener(v -> {
            favoritosViewModel.alternarFavorito(juego);
            actualizarIconoFavorito(binding.imagenEstrella, !juego.isFavorito());
        });

        binding.btnJugar.setOnClickListener(v -> navegarAWebView(juego.getId()));

        if (juego != null) {
            binding.textNombreJuego.setText(juego.getNombre());

            int colorAmarillo = ContextCompat.getColor(requireContext(), R.color.amarillo2);
            int colorVerde = ContextCompat.getColor(requireContext(), R.color.verde);
            int colorAzul = ContextCompat.getColor(requireContext(), R.color.azul);

            switch (juego.getId()) {
                case "1":
                    binding.imagenJuego.setImageResource(R.drawable.pacman);
                    binding.textNombreJuego.setTextColor(colorAmarillo);
                    binding.textoDescripcion.setTextColor(colorAmarillo);
                    binding.textNombreJuego.setShadowLayer(25f, 0f, 20f,
                            ContextCompat.getColor(requireContext(), R.color.amarillo2));
                    break;
                case "2":
                    binding.imagenJuego.setImageResource(R.drawable.tetris);
                    binding.textNombreJuego.setTextColor(colorAzul);
                    binding.textoDescripcion.setTextColor(colorAzul);
                    binding.textNombreJuego.setShadowLayer(25f, 0f, 20f,
                            ContextCompat.getColor(requireContext(), R.color.azul));
                    break;
                case "3":
                    binding.imagenJuego.setImageResource(R.drawable.flappybird);
                    binding.textNombreJuego.setTextColor(colorVerde);
                    binding.textoDescripcion.setTextColor(colorVerde);
                    binding.textNombreJuego.setShadowLayer(25f, 0f, 20f,
                            ContextCompat.getColor(requireContext(), R.color.verde));

                    break;
            }
            binding.textoDescripcion.setText(juego.getDescripcion());

        } else {
            navController.popBackStack();
            Toast.makeText(getContext(), "Error al cargar el juego.", Toast.LENGTH_SHORT).show();
        }
    }

    private void configurarObservadoresViewModel() {
        favoritosViewModel.getErrorMessage().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje != null) {
                Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
            }
        });

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

    private void actualizarIconoFavorito(ImageButton imagenEstrella, boolean esFavorito) {
        if (esFavorito) {
            imagenEstrella.setImageResource(R.drawable.estrella);
        } else {
            imagenEstrella.setImageResource(R.drawable.estrellablanca);
        }
    }

    private void navegarAWebView(String idJuego) {
        Bundle bundle = new Bundle();
        bundle.putString("idJuego", idJuego);

        navController.navigate(R.id.action_detailFragment_to_jugarJuegoFragment, bundle);
    }

}