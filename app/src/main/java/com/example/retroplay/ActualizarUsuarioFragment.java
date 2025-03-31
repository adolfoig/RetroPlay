package com.example.retroplay;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.retroplay.databinding.FragmentActualizarUsuarioBinding;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ActualizarUsuarioFragment extends Fragment {

    private FragmentActualizarUsuarioBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Context appContext;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        appContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
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

        cargarDatosUsuario();
        binding.btnRegistrarUsuario.setOnClickListener(v -> actualizarUsuario());
    }

    private void cargarDatosUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            binding.textoEmail.setText(user.getEmail());
            db.collection("Usuarios").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nombre = documentSnapshot.getString("nombre");
                            binding.textoNombre.setText(nombre != null ? nombre : user.getDisplayName());
                        } else {
                            binding.textoNombre.setText(user.getDisplayName());
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
        String nuevoEmail = binding.textoEmail.getText().toString().trim();
        String passwordActual = binding.textoPasswordAntigua.getText().toString().trim();
        String nuevaPassword = binding.textoPasswordNueva.getText().toString().trim();

        if (nuevoNombre.isEmpty() || nuevoEmail.isEmpty()) {
            showToast("Nombre y email son obligatorios");
            return;
        }

        if (passwordActual.isEmpty()) {
            binding.textoPasswordAntigua.setError("Ingrese su contraseña actual");
            binding.textoPasswordAntigua.requestFocus();
            showToast("Debe ingresar su contraseña actual para realizar cambios");
            return;
        }

        verificarPasswordActual(user, passwordActual, nuevoNombre, nuevoEmail, nuevaPassword);
    }

    private void verificarPasswordActual(FirebaseUser user, String passwordActual,
                                         String nuevoNombre, String nuevoEmail, String nuevaPassword) {
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), passwordActual);

        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        actualizarDatosCompletos(user, nuevoNombre, nuevoEmail, nuevaPassword);
                    } else {
                        binding.textoPasswordAntigua.setError("Contraseña incorrecta");
                        binding.textoPasswordAntigua.requestFocus();
                        showToast("La contraseña actual no es correcta");
                    }
                });
    }

    private void actualizarDatosCompletos(FirebaseUser user, String nombre, String email, String nuevaPassword) {
        actualizarDatosFirestore(user.getUid(), nombre, email);
        actualizarDatosAuth(user, nombre, email, nuevaPassword);
    }

    private void actualizarDatosFirestore(String userId, String nombre, String email) {
        Map<String, Object> datosUsuario = new HashMap<>();
        datosUsuario.put("nombre", nombre);
        datosUsuario.put("email", email);

        db.collection("Usuarios").document(userId)
                .set(datosUsuario)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Datos actualizados correctamente"))
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error al actualizar datos", e);
                    showToast("Error al guardar datos");
                });
    }

    private void actualizarDatosAuth(FirebaseUser user, String nombre, String email, String nuevaPassword) {
        if (!isAdded()) return;

        final int[] totalOperations = {1}; // Actualización de nombre
        if (!email.equals(user.getEmail())) totalOperations[0]++;
        if (!nuevaPassword.isEmpty()) totalOperations[0]++;

        final int[] completedOperations = {0};

        Runnable checkCompletion = () -> {
            completedOperations[0]++;
            if (completedOperations[0] == totalOperations[0]) {
                showToast("Datos actualizados correctamente");
                if (!email.equals(user.getEmail())) {
                    // Enviar correo de verificación solo si el email cambió
                    user.sendEmailVerification()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    showToast("Se ha enviado un correo de verificación");
                                }
                                cerrarSesion();
                            });
                } else {
                    cerrarSesion();
                }
            }
        };

        // 1. Actualizar nombre
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(nombre)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Auth", "Nombre actualizado en Auth");
                    }
                    checkCompletion.run();
                });

        // 2. Actualizar email si es diferente
        if (!email.equals(user.getEmail())) {
            // Primero reautenticar
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), binding.textoPasswordAntigua.getText().toString());
            user.reauthenticate(credential)
                    .addOnSuccessListener(authTask -> {
                        // Luego actualizar email
                        user.updateEmail(email)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d("Auth", "Email actualizado");
                                        // Enviar correo de verificación
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(verificationTask -> {
                                                    if (verificationTask.isSuccessful()) {
                                                        showToast("Se ha enviado un correo de verificación");
                                                    }
                                                });
                                    } else {
                                        showToast("Error al actualizar email: " + task.getException().getMessage());
                                    }
                                    checkCompletion.run();
                                });
                    })
                    .addOnFailureListener(e -> {
                        showToast("Error de autenticación: " + e.getMessage());
                        checkCompletion.run();
                    });
        }

        // 3. Actualizar contraseña si se proporcionó
        if (!nuevaPassword.isEmpty()) {
            user.updatePassword(nuevaPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("Auth", "Contraseña actualizada");
                            binding.textoPasswordAntigua.setText("");
                            binding.textoPasswordNueva.setText("");
                        }
                        checkCompletion.run();
                    });
        }
    }

    private void showToast(String message) {
        if (isAdded() && appContext != null) {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void cerrarSesion() {
        mAuth.signOut();
        Toast.makeText(getContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show();
        irAlLogin();
    }

    private void irAlLogin(){
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        assert getActivity() != null;
        getActivity().finish();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}