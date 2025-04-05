package com.example.retroplay;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.retroplay.Repository.UsuarioRepository;
import com.example.retroplay.Viewmodel.UsuarioViewModel;
import com.example.retroplay.databinding.FragmentActualizarUsuarioBinding;

public class ActualizarUsuarioFragment extends Fragment {

    private FragmentActualizarUsuarioBinding binding;
    private UsuarioViewModel usuarioViewModel;
    private Context appContext;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        appContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usuarioViewModel = new ViewModelProvider(this).get(UsuarioViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentActualizarUsuarioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupObservers();
        loadUserData();

        binding.btnRegistrarUsuario.setOnClickListener(v -> {
            String newName = binding.textoNombre.getText().toString().trim();
            String currentPassword = binding.textoPasswordAntigua.getText().toString().trim();
            String newPassword = binding.textoPasswordNueva.getText().toString().trim();

            usuarioViewModel.getUsuarioRepository().updateUser(currentPassword, newName, newPassword);
        });
    }

    private void setupObservers() {
        UsuarioRepository usuarioRepository = usuarioViewModel.getUsuarioRepository(); // Cambiado a getUsuarioRepository

        usuarioRepository.getUserData().observe(getViewLifecycleOwner(), userData -> {
            if (userData != null) {
                // Actualizar nombre
                binding.textoNombre.setText(userData.get("nombre"));

                // Mostrar email (no editable)
                String email = userData.get("email");
                if (email != null) {
                    binding.textoEmail.setText(email);
                    binding.textoEmail.setEnabled(false); // Deshabilitar edición
                }
            }
        });

        usuarioRepository.getUserUpdateResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                showToast(result);

                if (result.equals("Datos actualizados correctamente")) {
                    usuarioRepository.logout();
                } else if (result.equals("logout_success")) {
                    showToast("Sesión cerrada");
                    goToLogin();
                }
            }
        });
    }

    private void loadUserData() {
        usuarioViewModel.getUsuarioRepository().loadUserData();
    }

    private void showToast(String message) {
        if (isAdded() && appContext != null) {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}