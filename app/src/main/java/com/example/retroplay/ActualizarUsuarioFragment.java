package com.example.retroplay;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.example.retroplay.Supebase.SupabaseStorageApi;
import com.example.retroplay.Viewmodel.UsuarioViewModel;
import com.example.retroplay.databinding.FragmentActualizarUsuarioBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ActualizarUsuarioFragment extends Fragment {

    private FragmentActualizarUsuarioBinding binding;
    private UsuarioViewModel usuarioViewModel;
    private Context context;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private String currentImageUrl;

    SupabaseStorageApi supabaseStorageApi;


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

        // Create a proper Retrofit instance for SupabaseStorageApi
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://jeqhyzjjwmybvmliximh.supabase.co")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        supabaseStorageApi = retrofit.create(SupabaseStorageApi.class);
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
            String email = binding.textoEmail.getText().toString().trim();
            String contrasenaActual = binding.textoPasswordAntigua.getText().toString().trim();
            String nuevaContrasena = binding.textoPasswordNueva.getText().toString().trim();

            // Validaciones mejoradas
            if (TextUtils.isEmpty(nuevoNombre)) {
                mostrarToast("El nombre es obligatorio");
                return;
            }

            if (TextUtils.isEmpty(email)) {
                mostrarToast("El email es obligatorio");
                return;
            }

            if (TextUtils.isEmpty(contrasenaActual)) {
                mostrarToast("Debe ingresar su contraseña actual");
                return;
            }

            if (imageUri != null) {
                String nombreImagen = "profile_" + System.currentTimeMillis() + ".jpg";
                uploadImageAndUpdateUser(nombreImagen, contrasenaActual, nuevoNombre, email,
                        TextUtils.isEmpty(nuevaContrasena) ? null : nuevaContrasena);
            } else {
                usuarioViewModel.actualizarUsuario(
                        contrasenaActual,
                        nuevoNombre,
                        email,
                        TextUtils.isEmpty(nuevaContrasena) ? null : nuevaContrasena,
                        currentImageUrl
                );

                usuarioViewModel.getResultadoActualizacion().observe(getViewLifecycleOwner(), resultado -> {
                    if (resultado != null && resultado.equals("Datos actualizados correctamente")) {
                        mostrarToast("Perfil actualizado correctamente");
                        usuarioViewModel.cerrarSesion();
                        irALogin();
                    } else if (resultado != null && resultado.startsWith("Error")) {
                        mostrarToast(resultado);
                    }
                });
            }
        });
    }

    private void uploadImageAndUpdateUser(String nombreImagen, String contrasenaActual,
                                          String nuevoNombre, String email, String nuevaContrasena) {
        try {
            // Convertir URI a File de manera segura
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            File imageFile = new File(requireContext().getCacheDir(), nombreImagen);
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();

            uploadImage(imageFile).observe(getViewLifecycleOwner(), nuevaUrl -> {
                if (nuevaUrl != null) {
                    // Eliminar imagen anterior si existe
                    if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                        deleteImage(currentImageUrl);
                    }

                    // Actualizar usuario con nueva imagen
                    usuarioViewModel.actualizarUsuario(
                            contrasenaActual,
                            nuevoNombre,
                            email,
                            nuevaContrasena,
                            nuevaUrl
                    );

                    // Observar el resultado de la actualización
                    usuarioViewModel.getResultadoActualizacion().observe(getViewLifecycleOwner(), resultado -> {
                        if (resultado != null && resultado.equals("Datos actualizados correctamente")) {
                            mostrarToast("Perfil actualizado correctamente");
                            usuarioViewModel.cerrarSesion();
                            irALogin();
                        } else if (resultado != null && resultado.startsWith("Error")) {
                            mostrarToast(resultado);
                        }
                    });
                } else {
                    mostrarToast("Error al subir la imagen");
                }
            });
        } catch (IOException e) {
            mostrarToast("Error al procesar la imagen");
            e.printStackTrace();
        }
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

    public LiveData<Boolean> deleteImage(String fileUrl) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();

        // Extraer el nombre del archivo de la URL pública
        String fileName = obtenerNombreArchivo(fileUrl);
        if (fileName == null) {
            resultLiveData.postValue(false);
            return resultLiveData;
        }

        // Llamada a la API de Supabase para eliminar la imagen
        Call<Void> call = supabaseStorageApi.deleteImage("Bearer " + SUPABASE_AUTH_TOKEN, BUCKET_NAME, fileName);

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

    public LiveData<String> uploadImage(File imageFile) {
        // LiveData en el que devolveremos la URL pública de la imagen generada
        MutableLiveData<String> liveDataUrl = new MutableLiveData<>();

        // Crear el cuerpo de la petición para enviar a Supabase (en él se envía el fichero)
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", imageFile.getName(), requestFile);

        // Llamada a la API de Supabase
        // Param 1: Tu API KEY (Autenticación)
        // Param 2: nombre de tu bucket
        // Param 3: nombre con el que se creará el fichero en Supabase
        // Param 4: cuerpo de la petición (imagen)
        Call<Void> call = supabaseStorageApi.uploadImage("Bearer " + SUPABASE_AUTH_TOKEN, BUCKET_NAME, imageFile.getName(), body);

        // Enviamos la petición
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Esta URL será la que guardemos en nuestra base de datos
                    String fileUrl = response.raw().request().url().toString();

                    // Almacenamos el valor en el LiveData
                    liveDataUrl.postValue(fileUrl);
                } else {
                    // Si no se ha podido completar la petición, devolvemos null
                    liveDataUrl.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Si no se ha podido completar la petición, devolvemos null
                liveDataUrl.postValue(null);
            }
        });

        // Devolvemos el LiveData con la URL de nuestra imagen
        return liveDataUrl;
    }

    // Método auxiliar para extraer el nombre del archivo de la URL pública
    private String obtenerNombreArchivo(String fileUrl) {
        try {
            Uri uri = Uri.parse(fileUrl);
            return uri.getLastPathSegment();
        } catch (Exception e) {
            return null;
        }
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