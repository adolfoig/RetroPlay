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

import com.example.retroplay.Viewmodel.UsuarioViewModel;
import com.example.retroplay.databinding.FragmentActualizarUsuarioBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ActualizarUsuarioFragment extends Fragment {

    private FragmentActualizarUsuarioBinding binding;
    private FirebaseAuth mAuth;
    private UsuarioViewModel usuarioViewModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mAuth = FirebaseAuth.getInstance();
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
        cargarDatosUsuario();
        binding.btnRegistrarUsuario.setOnClickListener(v -> actualizarUsuario());
    }

    private void setupObservers() {
        usuarioViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                showToast(message);
            }
        });

        usuarioViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.btnRegistrarUsuario.setEnabled(!isLoading);
        });

        usuarioViewModel.getUpdateSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                showToast("Datos actualizados correctamente");
                cerrarSesion();
            }
        });
    }

    private void cargarDatosUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            usuarioViewModel.getUserName(user.getUid()).observe(getViewLifecycleOwner(), nombre -> {
                if (nombre != null) {
                    binding.textoNombre.setText(nombre);
                }
            });
        }
    }

    private void actualizarUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            showToast("Usuario no autenticado");
            return;
        }

        String nuevoNombre = binding.textoNombre.getText().toString().trim();
        String passwordActual = binding.textoPasswordAntigua.getText().toString().trim();
        String nuevaPassword = binding.textoPasswordNueva.getText().toString().trim();

        if (nuevoNombre.isEmpty()) {
            showToast("El nombre es obligatorio");
            return;
        }

        if (passwordActual.isEmpty()) {
            binding.textoPasswordAntigua.setError("Ingrese su contraseña actual");
            binding.textoPasswordAntigua.requestFocus();
            showToast("Debe ingresar su contraseña actual para realizar cambios");
            return;
        }

        usuarioViewModel.updateUser(user.getUid(), user.getEmail(), passwordActual, nuevoNombre, nuevaPassword);
    }

    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void cerrarSesion() {
        usuarioViewModel.signOut();
        Toast.makeText(getContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show();
        irAlLogin();
    }

    private void irAlLogin() {
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