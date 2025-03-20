package com.example.retroplay.Registro;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton, selectImageButton;
    private ImageView profileImageView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private Uri imageUri;

    public RegistroFragment() {
        // Constructor vacío requerido
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Asegúrate de inflar la vista correctamente
        View view = inflater.inflate(R.layout.fragment_registro, container, false);

        // Asignar las vistas a los elementos del layout
        nameEditText = view.findViewById(R.id.textoNombre);
        emailEditText = view.findViewById(R.id.textoEmail);
        passwordEditText = view.findViewById(R.id.textoPassword);
        confirmPasswordEditText = view.findViewById(R.id.textoConfirmarPassword);
        registerButton = view.findViewById(R.id.btnRegistrarUsuario);
        selectImageButton = view.findViewById(R.id.btnSubirImagenPerfil);
        profileImageView = view.findViewById(R.id.imagen);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Acción para seleccionar imagen de perfil
        selectImageButton.setOnClickListener(v -> openImageChooser());

        // Acción para registrar usuario
        registerButton.setOnClickListener(v -> registerUser());

        return view;
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
            profileImageView.setImageURI(imageUri);
        }
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Validación de campos vacíos
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
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
                        }else {
                            uploadProfileImage(user);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadProfileImage(FirebaseUser user) {
        if (imageUri != null) {
            StorageReference fileReference = storage.getReference().child("profile_images/" + user.getUid() + ".jpg");

            fileReference.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Guardar la URL de la imagen de perfil en Firestore
                    saveUserData(user, uri.toString());
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(getActivity(), "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
            });
        } else {
            saveUserData(user, null);
        }
    }

    private void saveUserData(FirebaseUser user, String profileImageUrl) {
        // Guardar los datos del usuario en Firestore
        Map<String, Object> userData = new HashMap<>();
        userData.put("nombre", nameEditText.getText().toString().trim());
        userData.put("email", emailEditText.getText().toString().trim());
        userData.put("puntuacionGlobal", 0);
        userData.put("profileImageUrl", profileImageUrl);

        firestore.collection("Usuarios").document(user.getUid())
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

        // Reemplazar el fragmento actual por el de registro
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, loginFragment)
                .addToBackStack(null) // Añadir a la pila de retroceso
                .commit();
    }
}