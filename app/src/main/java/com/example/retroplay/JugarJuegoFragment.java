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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;


public class JugarJuegoFragment extends Fragment {
    private FragmentJugarJuegoBinding binding;
    private WebView gameWebView;
    private String idJuego;
    private JuegosViewModel juegosViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        juegosViewModel = new ViewModelProvider(requireActivity()).get(JuegosViewModel.class);
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
            cargarJuegos(idJuego);
        }

        configurarObservers();
    }

    private void cargarJuegos(String gameId) {
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

    // En JugarJuegoFragment.java
    private void configurarObservers() {

        juegosViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showToast(error);
            }
        });

        juegosViewModel.getPuntuacionGuardada().observe(getViewLifecycleOwner(), success -> {
            if (success == null) return;

            if (success) {
                showToast("Puntuación guardada correctamente");
            } else {
                showToast("Error al guardar la puntuación");
            }
        });

        juegosViewModel.getServidorError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showToast(error);
            }
        });
    }

    private void showToast(String mensaje) {
        // requireContext() lanza IllegalStateException si el fragmento ya no está añadido,
        // así que no necesitas chequear isAdded() cada vez
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
    }


    private void enviarPuntuacionCero() {
        new Thread(() -> {
            try {
                URL url = new URL("https://7cd1a43d-f123-432a-8a32-15d60b150f6c-00-1jwr28pe3c5ky.kirk.replit.dev/score");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);
                conn.setInstanceFollowRedirects(false);   // Para ver si hay 301/302

                String json = "{\"score\":0}";
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes("utf-8"));
                }

                int code = conn.getResponseCode();
                String msg  = conn.getResponseMessage();

                // Lee cuerpo (input o error-stream) para pasarlo al Logcat
                InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
                String body = "";
                if (is != null) {
                    body = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                            .lines().collect(Collectors.joining("\n"));
                }

                Log.d("POST", "Status: " + code + " " + msg + "\nBody: " + body);
                conn.disconnect();

            } catch (Exception e) {
                Log.e("POST", "Excepción al enviar", e);
            }
        }).start();
    }

    @Override
    public void onPause() {
        super.onPause();
        juegosViewModel.fetchScore(idJuego);
        enviarPuntuacionCero();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (gameWebView != null) {
            gameWebView.destroy();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).mostrarToolBar();
            }
        }
    }
}
