package com.example.retroplay.Repository;

import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.retroplay.Model.Usuario;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
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

                        // Verificar si el usuario es de Google y tiene foto
                        boolean esUsuarioGoogle = false;
                        for (UserInfo userInfo : usuario.getProviderData()) {
                            if ("google.com".equals(userInfo.getProviderId())) {
                                esUsuarioGoogle = true;
                                break;
                            }
                        }

                        String googlePhotoUrl = usuario.getPhotoUrl() != null ? usuario.getPhotoUrl().toString() : "";

                        if (documentSnapshot.exists()) {
                            String nombre = documentSnapshot.getString("nombre");
                            String urlImagen = documentSnapshot.getString("UrlImagenPerfil");

                            infoUsuario.put("nombre", nombre != null ? nombre : usuario.getDisplayName());
                            // Si es usuario de Google y no hay URL en Firestore, usar la de Google
                            infoUsuario.put("urlImagen",
                                    (urlImagen != null && !urlImagen.isEmpty()) ? urlImagen :
                                            (esUsuarioGoogle ? googlePhotoUrl : ""));
                        } else {
                            infoUsuario.put("nombre", usuario.getDisplayName());
                            // Si es usuario de Google, usar su foto
                            infoUsuario.put("urlImagen", esUsuarioGoogle ? googlePhotoUrl : "");
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

    public void actualizarUsuario(String contrasenaActual, String nuevoNombre,
                                  String nuevoEmail, String nuevaContrasena, String urlImagenPerfil) {
        FirebaseUser usuario = mAuth.getCurrentUser();
        if (usuario == null) {
            resultadoActualizarUsuario.postValue("Usuario no autenticado");
            return;
        }

        // Validar que la nueva contraseña no sea igual a la actual
        if (!TextUtils.isEmpty(nuevaContrasena) && nuevaContrasena.equals(contrasenaActual)) {
            resultadoActualizarUsuario.postValue("La nueva contraseña no puede ser igual a la actual");
            return;
        }

        // Resto de validaciones...
        if (TextUtils.isEmpty(nuevoNombre)) {
            resultadoActualizarUsuario.postValue("El nombre es obligatorio");
            return;
        }

        if (TextUtils.isEmpty(nuevoEmail)) {
            resultadoActualizarUsuario.postValue("El email es obligatorio");
            return;
        }

        if (TextUtils.isEmpty(contrasenaActual)) {
            resultadoActualizarUsuario.postValue("Debe ingresar su contraseña actual");
            return;
        }

        AuthCredential authCredential = EmailAuthProvider.getCredential(usuario.getEmail(), contrasenaActual);
        usuario.reauthenticate(authCredential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Actualizar email si cambió
                        if (!usuario.getEmail().equals(nuevoEmail)) {
                            usuario.updateEmail(nuevoEmail)
                                    .addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            actualizarPerfilCompleto(usuario, nuevoNombre, nuevaContrasena, urlImagenPerfil);
                                        } else {
                                            resultadoActualizarUsuario.postValue("Error al actualizar email: " +
                                                    emailTask.getException().getMessage());
                                        }
                                    });
                        } else {
                            actualizarPerfilCompleto(usuario, nuevoNombre, nuevaContrasena, urlImagenPerfil);
                        }
                    } else {
                        resultadoActualizarUsuario.postValue("Contraseña actual incorrecta");
                    }
                });
    }

    private void actualizarPerfilCompleto(FirebaseUser usuario, String nuevoNombre,
                                          String nuevaContrasena, String urlImagenPerfil) {
        // Actualizar nombre en Firebase Auth
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(nuevoNombre)
                .build();

        usuario.updateProfile(profileUpdates)
                .addOnCompleteListener(profileTask -> {
                    if (profileTask.isSuccessful()) {
                        // Actualizar contraseña si se proporcionó una nueva
                        if (!TextUtils.isEmpty(nuevaContrasena)) {
                            usuario.updatePassword(nuevaContrasena)
                                    .addOnCompleteListener(passwordTask -> {
                                        if (passwordTask.isSuccessful()) {
                                            guardarDatosEnFirestore(usuario, nuevoNombre, urlImagenPerfil);
                                        } else {
                                            resultadoActualizarUsuario.postValue("Error al actualizar contraseña: " +
                                                    passwordTask.getException().getMessage());
                                        }
                                    });
                        } else {
                            guardarDatosEnFirestore(usuario, nuevoNombre, urlImagenPerfil);
                        }
                    } else {
                        resultadoActualizarUsuario.postValue("Error al actualizar perfil: " +
                                profileTask.getException().getMessage());
                    }
                });
    }

    private void guardarDatosEnFirestore(FirebaseUser usuario, String nombre, String urlImagenPerfil) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("nombre", nombre);
        if (urlImagenPerfil != null) {
            updates.put("UrlImagenPerfil", urlImagenPerfil);
        }

        db.collection("Usuarios").document(usuario.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    resultadoActualizarUsuario.postValue("Datos actualizados correctamente");
                    cargarDatosUsuario();
                })
                .addOnFailureListener(e -> {
                    resultadoActualizarUsuario.postValue("Error al guardar datos: " + e.getMessage());
                });
    }
    private void comprobarActualizacion(int[] completedOperations, int totalOperations) {
        completedOperations[0]++;
        if (completedOperations[0] == totalOperations) {
            resultadoActualizarUsuario.postValue("Datos actualizados correctamente");
        }
    }

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

    public void guardarDatosUsuarioFirestore(String idUsuario, String nombre, String email, String profileImageUrl) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("nombre", nombre);
        userData.put("email", email);
        userData.put("UrlImagenPerfil", profileImageUrl);

        db.collection("Usuarios").document(idUsuario)
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Datos actualizados en Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error al actualizar Firestore", e));
    }

    public Task<DocumentSnapshot> getUsuarioPorId(String userId) {
        return db.collection("Usuarios").document(userId).get();
    }

    public void cerrarSesion() {
        mAuth.signOut();
        resultadoActualizarUsuario.postValue("logout_success");
    }
}