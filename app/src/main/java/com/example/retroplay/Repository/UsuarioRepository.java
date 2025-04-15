package com.example.retroplay.Repository;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.retroplay.Model.Usuario;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UsuarioRepository {
    private static final String TAG = "UsuarioRepository";
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    private final MutableLiveData<Map<String, String>> datosUsuario = new MutableLiveData<>();
    private final MutableLiveData<String> resultadoActualizarUsuario = new MutableLiveData<>();
    private final MutableLiveData<Boolean> registroExitoso = new MutableLiveData<>();
    private final MutableLiveData<String> errorRegistro = new MutableLiveData<>();

    public UsuarioRepository() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // Métodos para LiveData
    public MutableLiveData<Map<String, String>> getDatosUsuario() {
        return datosUsuario;
    }

    public MutableLiveData<String> getResultadoActualizacionUsuario() {
        return resultadoActualizarUsuario;
    }

    public MutableLiveData<Boolean> getRegistroExitoso() {
        return registroExitoso;
    }

    public MutableLiveData<String> getErrorRegistro() {
        return errorRegistro;
    }

    // Métodos de autenticación existentes
    public void cargarDatosUsuario() {
        FirebaseUser usuario = mAuth.getCurrentUser();
        if (usuario != null) {
            String email = usuario.getEmail();

            db.collection("Usuarios").document(usuario.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Map<String, String> infoUsuario = new HashMap<>();
                        infoUsuario.put("email", email);

                        if (documentSnapshot.exists()) {
                            String nombre = documentSnapshot.getString("nombre");
                            String urlImagen = documentSnapshot.getString("UrlImagenPerfil");
                            infoUsuario.put("nombre", nombre != null ? nombre : usuario.getDisplayName());
                            infoUsuario.put("urlImagen", urlImagen != null ? urlImagen : "");
                        } else {
                            infoUsuario.put("nombre", usuario.getDisplayName());
                            infoUsuario.put("urlImagen", "");
                        }

                        datosUsuario.postValue(infoUsuario);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al cargar datos del usuario", e);
                        datosUsuario.postValue(null);
                    });
        } else {
            datosUsuario.postValue(null);
        }
    }

    public void actualizarUsuario(String contrasenaActual, String nuevoNombre, String nuevaContrasena) {
        FirebaseUser usuario = mAuth.getCurrentUser();
        if (usuario == null) {
            resultadoActualizarUsuario.postValue("Usuario no autenticado");
            return;
        }

        if (nuevoNombre.isEmpty()) {
            resultadoActualizarUsuario.postValue("El nombre es obligatorio");
            return;
        }

        if (contrasenaActual.isEmpty()) {
            resultadoActualizarUsuario.postValue("Debe ingresar su contraseña actual para realizar cambios");
            return;
        }

        AuthCredential authCredential = EmailAuthProvider.getCredential(usuario.getEmail(), contrasenaActual);
        usuario.reauthenticate(authCredential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        actualizarDatosUsuario(usuario, nuevoNombre, nuevaContrasena);
                    } else {
                        resultadoActualizarUsuario.postValue("La contraseña actual no es correcta");
                    }
                });
    }

    private void actualizarDatosUsuario(FirebaseUser usuario, String nombre, String nuevaContrasena) {
        int totalOperations = 1;
        if (!nuevaContrasena.isEmpty()) totalOperations++;

        final int[] completedOperations = {0};

        Map<String, Object> datosUsuario = new HashMap<>();
        datosUsuario.put("nombre", nombre);
        datosUsuario.put("email", usuario.getEmail());

        int finalTotalOperations2 = totalOperations;
        db.collection("Usuarios").document(usuario.getUid())
                .set(datosUsuario)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Datos actualizados en Firestore");
                    comprobarActualizacion(completedOperations, finalTotalOperations2);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al actualizar datos en Firestore", e);
                    resultadoActualizarUsuario.postValue("Error al guardar datos");
                });

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(nombre)
                .build();

        int finalTotalOperations1 = totalOperations;
        usuario.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Perfil de autenticación actualizado");
                    }
                    comprobarActualizacion(completedOperations, finalTotalOperations1);
                });

        if (!nuevaContrasena.isEmpty()) {
            int finalTotalOperations = totalOperations;
            usuario.updatePassword(nuevaContrasena)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Contraseña actualizada");
                        }
                        comprobarActualizacion(completedOperations, finalTotalOperations);
                    });
        }
    }

    private void comprobarActualizacion(int[] completedOperations, int totalOperations) {
        completedOperations[0]++;
        if (completedOperations[0] == totalOperations) {
            resultadoActualizarUsuario.postValue("Datos actualizados correctamente");
        }
    }

    // Nuevos métodos para el registro
    public void registrarUsuarioFirebase(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Solo notificamos éxito en Auth, Firestore se manejará después
                            registroExitoso.postValue(true);
                        } else {
                            errorRegistro.postValue("Error: usuario no autenticado");
                        }
                    } else {
                        errorRegistro.postValue("Error: " + task.getException().getMessage());
                    }
                });
    }

    public void guardarDatosUsuarioFirestore(String userId, String nombre, String email, String profileImageUrl) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("nombre", nombre);
        userData.put("email", email);
        userData.put("UrlImagenPerfil", profileImageUrl);

        db.collection("Usuarios").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> registroExitoso.postValue(true))
                .addOnFailureListener(e -> {
                    errorRegistro.postValue("Error al guardar datos: " + e.getMessage());
                    Log.e(TAG, "Error al guardar datos", e);
                });
    }

    public Task<DocumentSnapshot> getUsuarioPorId(String userId) {
        return db.collection("Usuarios").document(userId).get();
    }

    public void cerrarSesion() {
        mAuth.signOut();
        resultadoActualizarUsuario.postValue("logout_success");
    }
}