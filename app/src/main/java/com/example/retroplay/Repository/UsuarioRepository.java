package com.example.retroplay.Repository;

import android.content.Context;
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
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    private final Context appContext;

    public UsuarioRepository(Context context) {
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.appContext = context.getApplicationContext();
    }

    public UsuarioRepository(FirebaseAuth mAuth, FirebaseFirestore db, Context appContext) {
        this.mAuth = mAuth;
        this.db = db;
        this.appContext = appContext;
    }

    public MutableLiveData<String> getUserName(String userId) {
        MutableLiveData<String> userNameLiveData = new MutableLiveData<>();

        db.collection("Usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("nombre");
                        userNameLiveData.setValue(nombre != null ? nombre : mAuth.getCurrentUser().getDisplayName());
                    } else {
                        userNameLiveData.setValue(mAuth.getCurrentUser().getDisplayName());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRepository", "Error al obtener nombre de usuario", e);
                    userNameLiveData.setValue(null);
                });

        return userNameLiveData;
    }

    public MutableLiveData<Boolean> reauthenticateUser(String email, String password) {
        MutableLiveData<Boolean> reauthLiveData = new MutableLiveData<>();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            reauthLiveData.setValue(false);
            return reauthLiveData;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> reauthLiveData.setValue(task.isSuccessful()));

        return reauthLiveData;
    }

    public MutableLiveData<Boolean> updateUserData(String userId, String nombre, String nuevaPassword) {
        MutableLiveData<Boolean> updateLiveData = new MutableLiveData<>();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            updateLiveData.setValue(false);
            return updateLiveData;
        }

        // Actualizar Firestore
        Map<String, Object> datosUsuario = new HashMap<>();
        datosUsuario.put("nombre", nombre);

        db.collection("Usuarios").document(userId)
                .set(datosUsuario)
                .addOnSuccessListener(aVoid -> {
                    Log.d("UserRepository", "Datos actualizados en Firestore");
                    updateAuthData(user, nombre, nuevaPassword, updateLiveData);
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRepository", "Error al actualizar Firestore", e);
                    updateLiveData.setValue(false);
                });

        return updateLiveData;
    }

    private void updateAuthData(FirebaseUser user, String nombre, String nuevaPassword,
                                MutableLiveData<Boolean> updateLiveData) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(nombre)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(profileTask -> {
                    if (nuevaPassword != null && !nuevaPassword.isEmpty()) {
                        user.updatePassword(nuevaPassword)
                                .addOnCompleteListener(passwordTask -> {
                                    updateLiveData.setValue(passwordTask.isSuccessful() && profileTask.isSuccessful());
                                });
                    } else {
                        updateLiveData.setValue(profileTask.isSuccessful());
                    }
                });
    }

    public void signOut() {
        mAuth.signOut();
    }
}
