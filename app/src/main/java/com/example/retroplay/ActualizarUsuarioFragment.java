package com.example.retroplay;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.retroplay.Supebase.SupabaseClient;
import com.example.retroplay.Supebase.SupabaseStorageApi;
import com.example.retroplay.Utils.ImageUtils;
import com.example.retroplay.Viewmodel.UsuarioViewModel;
import com.example.retroplay.databinding.FragmentActualizarUsuarioBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActualizarUsuarioFragment extends Fragment {

    private FragmentActualizarUsuarioBinding binding;
    private UsuarioViewModel usuarioViewModel;
    private Context context;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private String currentImageUrl;

    private static final String SUPABASE_AUTH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImplcWh5empqd215YnZtbGl4aW1oIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQ2OTc0MjgsImV4cCI6MjA2MDI3MzQyOH0.04H44bmAJpwZo2wLQ92FghRse4KLSOLQJd9OICJJVvo";
    private static final String BUCKET_NAME = "imagenes";


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context.getApplicationContext();
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

        binding.btnSubirImagenPerfil.setOnClickListener(v -> openImageChooser());

        binding.btnRegistrarUsuario.setOnClickListener(v -> {
            String nuevoNombre = binding.textoNombre.getText().toString().trim();
            String contrasenaActual = binding.textoPasswordAntigua.getText().toString().trim();
            String nuevaContrasena = binding.textoPasswordNueva.getText().toString().trim();

            if (imageUri != null) {
                // Subir nueva imagen primero
                //deleteImage();
                //usuarioViewModel.actualizarUsuarioConImagen(nuevoNombre, contrasenaActual, nuevaContrasena, );
            } else {
                // Actualizar sin cambiar imagen
                usuarioViewModel.actualizarUsuario(contrasenaActual, nuevoNombre, nuevaContrasena);
            }
        });
    }


    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(requireContext())
                    .load(imageUri)
                    .circleCrop()
                    .into(binding.imagen);
        }
    }

    private void setupObservers() {
        usuarioViewModel.getDatosUsuario().observe(getViewLifecycleOwner(), userData -> {
            if (userData != null) {
                binding.textoNombre.setText(usuarioViewModel.getUserName(userData));

                String email = usuarioViewModel.getUserEmail(userData);
                if (email != null) {
                    binding.textoEmail.setText(email);
                    binding.textoEmail.setEnabled(false);
                }

                currentImageUrl = usuarioViewModel.getUserImageUrl(userData);
                if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                    Glide.with(requireContext())
                            .load(currentImageUrl)
                            .circleCrop()
                            .into(binding.imagen);
                } else {
                    binding.imagen.setImageResource(R.drawable.logo);
                }
            }
        });
    }

    private String obtenerNombreArchivo(String fileUrl) {
        try {
            Uri uri = Uri.parse(fileUrl);
            return uri.getLastPathSegment(); // Obtiene el último segmento de la URL (nombre del archivo)
        } catch (Exception e) {
            return null;
        }
    }

    public LiveData<Boolean> deleteImage(String fileUrl) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();

        // Extraer el nombre del archivo de la URL pública
        String fileName = obtenerNombreArchivo(fileUrl);
        if (fileName == null) {
            resultLiveData.postValue(false);
            return resultLiveData;
        }
        SupabaseStorageApi api = SupabaseClient.getClient().create(SupabaseStorageApi.class);

        // Llamada a la API de Supabase para eliminar la imagen
        Call<Void> call = api.deleteImage("Bearer " + SUPABASE_AUTH_TOKEN, "imagenes", fileName);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    resultLiveData.postValue(true);
                } else {
                    resultLiveData.postValue(false);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                resultLiveData.postValue(false);
            }
        });

        return resultLiveData;
    }


    private void cargarDatosUsuario() {
        usuarioViewModel.cargarDatosUsuario();
    }

    private void mostrarToast(String message) {
        if (isAdded() && context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void irALogin() {
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