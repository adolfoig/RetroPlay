package com.example.retroplay.Registro;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.retroplay.R;
import com.example.retroplay.databinding.FragmentRegistroBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class RegistroFragment extends Fragment {

    private FragmentRegistroBinding binding;
    private static final int PICK_IMAGE_REQUEST = 1;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseStorage firebaseStorage;
    private Uri imageUri;

    public RegistroFragment() {
        // Constructor vacío para poder navegar
    }

    @SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Asegúrate de inflar la vista correctamente
        binding = FragmentRegistroBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        // Seleccionar imagen de perfil
        binding.btnSubirImagenPerfil.setOnClickListener(v -> openImageChooser());

        // Registrar usuario
        binding.btnRegistrarUsuario.setOnClickListener(v -> registrarUsuario());

        // Listener para cerrar el teclado cuando el usuario toque fuera de los campos de texto
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View currentFocus = getActivity().getCurrentFocus();
                if (currentFocus != null) {
                    currentFocus.clearFocus();

                    // Ocultar el teclado
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getActivity().getSystemService(getContext().INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                    }
                }
            }
            return false;
        });

        return binding.getRoot();
    }

    private void openImageChooser() {
        // Abrir el selector de imágenes
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            binding.imagen.setImageURI(imageUri);
        }
    }

    private void registrarUsuario() {
        String nombre = binding.textoNombre.getText().toString().trim();
        String email = binding.textoEmail.getText().toString().trim();
        String password = binding.textoPassword.getText().toString().trim();
        String confirmPassword = binding.textoConfirmarPassword.getText().toString().trim();

        // Validación de campos vacíos
        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(getActivity(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar el formato del correo electrónico
        if (!email.contains("@")) {
            Toast.makeText(getActivity(), "Correo electrónico inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar si las contraseñas coinciden
        if (!password.equals(confirmPassword)) {
            Toast.makeText(getActivity(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar la longitud de la contraseña
        if (password.length() < 6) {
            Toast.makeText(getActivity(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear usuario con Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(getActivity(), "Error: usuario no autenticado", Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            uploadProfileImage(user);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadProfileImage(FirebaseUser usuario) {
        if (imageUri != null) {
            StorageReference fileReference = firebaseStorage.getReference().child("profile_images/" + usuario.getUid() + ".jpg");

            fileReference.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Guardar la URL de la imagen de perfil en Firestore
                    guardarDatosUsuario(usuario, uri.toString());
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(getActivity(), "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
            });
        } else {
            guardarDatosUsuario(usuario, null);
        }
    }

    private void guardarDatosUsuario(FirebaseUser usuario, String profileImageUrl) {
        // Guardar los datos del usuario en Firestore
        Map<String, Object> userData = new HashMap<>();
        userData.put("nombre", binding.textoNombre.getText().toString().trim());
        userData.put("email", binding.textoEmail.getText().toString().trim());
        userData.put("profileImageUrl", profileImageUrl);

        firestore.collection("Usuarios").document(usuario.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show();
                    getActivity().finish(); // Cerrar actividad después del registro
                    irLoginFragment();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void irLoginFragment() {
        LoginFragment loginFragment = new LoginFragment();

        // Reemplazar el fragmento actual por el de login
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, loginFragment)
                .addToBackStack(null)
                .commit();
    }
}
