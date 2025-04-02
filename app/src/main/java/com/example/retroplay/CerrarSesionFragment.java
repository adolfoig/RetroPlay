package com.example.retroplay;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.retroplay.databinding.FragmentCerrarSesionBinding;
import com.example.retroplay.databinding.FragmentLoginBinding;
import com.google.firebase.auth.FirebaseAuth;


public class CerrarSesionFragment extends Fragment {

    private FragmentCerrarSesionBinding binding;
    private FirebaseAuth mAuth;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCerrarSesionBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();


        // Como cierrar sesion va al Login
        binding.btnSi.setOnClickListener(v -> cerrarSesion());

        // Como no se cierra sesion lleva al JuegosFragment
        binding.btnNo.setOnClickListener(v -> irAMain());

        return binding.getRoot();
    }

    private void cerrarSesion() {
        mAuth.signOut();
        Toast.makeText(getContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show();
        irAMain();
    }

    private void irAMain(){
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        assert getActivity() != null;
        getActivity().finish();
    }



}