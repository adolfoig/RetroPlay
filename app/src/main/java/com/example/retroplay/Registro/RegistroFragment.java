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
import androidx.lifecycle.ViewModelProvider;

import com.example.retroplay.R;
import com.example.retroplay.Supebase.SupabaseClient;
import com.example.retroplay.Supebase.SupabaseStorageApi;
import com.example.retroplay.Utils.ImageUtils;
import com.example.retroplay.Viewmodel.UsuarioViewModel;
import com.example.retroplay.databinding.FragmentRegistroBinding;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistroFragment extends Fragment {

    private FragmentRegistroBinding binding;
    private static final int PICK_IMAGE_REQUEST = 1;
    private UsuarioViewModel usuarioViewModel;
    private Uri imagenUri;
    private static final String SUPABASE_AUTH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImplcWh5empqd215YnZtbGl4aW1oIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQ2OTc0MjgsImV4cCI6MjA2MDI3MzQyOH0.04H44bmAJpwZo2wLQ92FghRse4KLSOLQJd9OICJJVvo";
    private static final String BUCKET_NAME = "imagenes";

    public RegistroFragment() {
        // Constructor vacío
    }

    @SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistroBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        usuarioViewModel = new ViewModelProvider(this).get(UsuarioViewModel.class);

        binding.btnSubirImagenPerfil.setOnClickListener(v -> openImageChooser());
        binding.btnRegistrarUsuario.setOnClickListener(v -> registrarUsuario());

        usuarioViewModel.getRegistroExitoso().observe(getViewLifecycleOwner(), exito -> {
            if (exito) {
                Toast.makeText(getActivity(), "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show();

                // Navegar al LoginFragment
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new LoginFragment())
                            .addToBackStack(null)
                            .commit();
                }
            }
        });

        usuarioViewModel.getErrorRegistro().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
            }
        });

        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View currentFocus = getActivity().getCurrentFocus();
                if (currentFocus != null) {
                    currentFocus.clearFocus();
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
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            imagenUri = data.getData();
            binding.imagen.setImageURI(imagenUri);
        }
    }

    private void registrarUsuario() {
        String nombre = binding.textoNombre.getText().toString().trim();
        String email = binding.textoEmail.getText().toString().trim();
        String password = binding.textoPassword.getText().toString().trim();
        String confirmPassword = binding.textoConfirmarPassword.getText().toString().trim();

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(getActivity(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.contains("@")) {
            Toast.makeText(getActivity(), "Correo electrónico inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(getActivity(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(getActivity(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnRegistrarUsuario.setText(R.string.registrando);
        binding.btnRegistrarUsuario.setEnabled(false);


        if (imagenUri != null) {
            subirImagen(nombre, email, password);
        } else {
            usuarioViewModel.registrarUsuario(nombre, email, password, null);
        }

        usuarioViewModel.getRegistroExitoso().observe(getViewLifecycleOwner(), exito -> {
            // Restaurar el botón independientemente del resultado
            binding.btnRegistrarUsuario.setText(R.string.registrarse);
            binding.btnRegistrarUsuario.setEnabled(true);
        });

        usuarioViewModel.getErrorRegistro().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                // Restaurar el botón cuando hay error
                binding.btnRegistrarUsuario.setText(R.string.registrarse);
                binding.btnRegistrarUsuario.setEnabled(true);
            }
        });
    }

    private void subirImagen(String nombre, String email, String password) {
        try {
            File file = ImageUtils.getFileFromUri(requireContext(), imagenUri);
            String nombreArchivo = email.hashCode() + ".jpg";

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", nombreArchivo, requestBody);

            SupabaseStorageApi api = SupabaseClient.getClient().create(SupabaseStorageApi.class);
            Call<Void> call = api.uploadImage(SUPABASE_AUTH_TOKEN, BUCKET_NAME, nombreArchivo, body);

            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        String imageUrl = SupabaseClient.BASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + nombreArchivo;
                        // Pasar todos los parámetros al ViewModel
                        usuarioViewModel.registrarUsuario(nombre, email, password, imageUrl);
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(getActivity(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (IOException e) {
            Toast.makeText(getActivity(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
        }
    }
}