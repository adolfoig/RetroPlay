package com.example.retroplay;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.retroplay.databinding.ActivityMainBinding;
import com.example.retroplay.databinding.FragmentJugarJuegoBinding;

public class JugarJuegoFragment extends Fragment {

    FragmentJugarJuegoBinding binding;
    private WebView gameWebView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

        gameWebView.loadUrl("file:///android_asset/pacman/index.html");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (gameWebView != null) {
            gameWebView.destroy();
        }
    }
}
