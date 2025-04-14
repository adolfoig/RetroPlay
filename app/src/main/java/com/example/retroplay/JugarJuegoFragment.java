package com.example.retroplay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.retroplay.Viewmodel.JuegosViewModel;
import com.example.retroplay.databinding.FragmentJugarJuegoBinding;


public class JugarJuegoFragment extends Fragment {
    private FragmentJugarJuegoBinding binding;
    private WebView gameWebView;
    private String idJuego;
    private JuegosViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(JuegosViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentJugarJuegoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gameWebView = binding.gameWebView;
        WebSettings webSettings = gameWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        Bundle args = getArguments();
        if (args != null) {
            idJuego = args.getString("idJuego");
            loadGame(idJuego);
        }

        setupObservers();
    }

    private void loadGame(String gameId) {
        if (gameId != null) {
            switch (gameId) {
                case "1":
                    gameWebView.loadUrl("file:///android_asset/pacman/index.html");
                    break;
                case "2":
                    gameWebView.loadUrl("file:///android_asset/classic-tetris-js-master/index.html");
                    break;
                case "3":
                    gameWebView.loadUrl("file:///android_asset/flappy-bird-master/index.html");
                    break;
            }
        }
    }

    private void setupObservers() {
        viewModel.getScore().observe(getViewLifecycleOwner(), score -> {
            if (score != null && isAdded() && getActivity() != null) {
                Intent intent = new Intent(requireActivity(), MainActivity.class);
                intent.putExtra("puntuacion", String.valueOf(score));
                startActivity(intent);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (gameWebView != null) {
            gameWebView.destroy();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).mostrarToolBar();
            }
            viewModel.fetchScore(idJuego);
        }
    }
}
