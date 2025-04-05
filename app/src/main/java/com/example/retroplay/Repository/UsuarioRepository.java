package com.example.retroplay.Repository;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UsuarioRepository {
    private static final String TAG = "UsuarioRepository";
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    private final MutableLiveData<Map<String, String>> userData = new MutableLiveData<>();
    private final MutableLiveData<String> userUpdateResult = new MutableLiveData<>();

    public UsuarioRepository() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public MutableLiveData<String> getUserUpdateResult() {
        return userUpdateResult;
    }

    public MutableLiveData<Map<String, String>> getUserData() {
        return userData;
    }

    public void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userEmail = user.getEmail(); // Obtenemos el email del usuario

            db.collection("Usuarios").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Map<String, String> userInfo = new HashMap<>();

                        // Siempre guardamos el email
                        userInfo.put("email", userEmail);

                        if (documentSnapshot.exists()) {
                            String nombre = documentSnapshot.getString("nombre");
                            userInfo.put("nombre", nombre != null ? nombre : user.getDisplayName());
                        } else {
                            userInfo.put("nombre", user.getDisplayName());
                        }

                        userData.postValue(userInfo);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al cargar datos del usuario", e);
                        userData.postValue(null);
                    });
        } else {
            userData.postValue(null);
        }
    }

    public void updateUser(String currentPassword, String newName, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            userUpdateResult.postValue("Usuario no autenticado");
            return;
        }

        if (newName.isEmpty()) {
            userUpdateResult.postValue("El nombre es obligatorio");
            return;
        }

        if (currentPassword.isEmpty()) {
            userUpdateResult.postValue("Debe ingresar su contraseña actual para realizar cambios");
            return;
        }

        // Reautenticación del usuario
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateUserData(user, newName, newPassword);
                    } else {
                        userUpdateResult.postValue("La contraseña actual no es correcta");
                    }
                });
    }

    private void updateUserData(FirebaseUser user, String name, String newPassword) {
        int totalOperations = 1; // actualización de nombre
        if (!newPassword.isEmpty()) totalOperations++;

        final int[] completedOperations = {0};

        // Actualizar Firestore (guardamos tanto nombre como email)
        Map<String, Object> userData = new HashMap<>();
        userData.put("nombre", name);
        userData.put("email", user.getEmail()); // Guardamos el email actual

        int finalTotalOperations2 = totalOperations;
        db.collection("Usuarios").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Datos actualizados en Firestore");
                    checkCompletion(completedOperations, finalTotalOperations2);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al actualizar datos en Firestore", e);
                    userUpdateResult.postValue("Error al guardar datos");
                });

        // Actualizar perfil de autenticación
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        int finalTotalOperations1 = totalOperations;
        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Perfil de autenticación actualizado");
                    }
                    checkCompletion(completedOperations, finalTotalOperations1);
                });

        // Actualizar contraseña si se proporcionó
        if (!newPassword.isEmpty()) {
            int finalTotalOperations = totalOperations;
            user.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Contraseña actualizada");
                        }
                        checkCompletion(completedOperations, finalTotalOperations);
                    });
        }
    }

    private void checkCompletion(int[] completedOperations, int totalOperations) {
        completedOperations[0]++;
        if (completedOperations[0] == totalOperations) {
            userUpdateResult.postValue("Datos actualizados correctamente");
        }
    }

    public void logout() {
        mAuth.signOut();
        userUpdateResult.postValue("logout_success");
    }
}