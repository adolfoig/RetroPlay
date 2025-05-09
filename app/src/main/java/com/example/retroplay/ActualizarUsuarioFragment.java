package com.example.retroplay;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.content.Context;
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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.retroplay.Supebase.SupabaseStorageApi;
import com.example.retroplay.Utils.ImageUtils;
import com.example.retroplay.Viewmodel.UsuarioViewModel;
import com.example.retroplay.databinding.FragmentActualizarUsuarioBinding;

import java.io.File;
import java.io.IOException;

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

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://jeqhyzjjwmybvmliximh.supabase.co")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        supabaseStorageApi = retrofit.create(SupabaseStorageApi.class);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentActualizarUsuarioBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupObservers();
        cargarDatosUsuario();

        usuarioViewModel.getResultadoActualizacion().observe(getViewLifecycleOwner(), resultado -> {
            if (resultado != null) {
                if (resultado.equals("Datos actualizados correctamente")) {
                    mostrarToast("Perfil actualizado correctamente");
                    usuarioViewModel.cerrarSesion();
                    irALogin();
                } else if (resultado.equals("Contraseña actual incorrecta")) {
                    mostrarToast("La contraseña actual es incorrecta");
                } else if (resultado.equals("La nueva contraseña no puede ser igual a la actual")) {
                    mostrarToast("La nueva contraseña debe ser diferente a la actual");
                } else if (resultado.startsWith("Error")) {
                    mostrarToast(resultado);
                }
            }
        });

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

            binding.btnRegistrarUsuario.setText(R.string.actualizandoUsuario);
            binding.btnRegistrarUsuario.setEnabled(false);

            if (imageUri != null) {
                // Generar nombre único para la imagen
                String nombreImagen = obtenerNombreArchivo(currentImageUrl)+ ".jpg";
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
            }

            usuarioViewModel.getResultadoActualizacion().observe(getViewLifecycleOwner(), resultado -> {
                // Restaurar el botón en cualquier caso
                binding.btnRegistrarUsuario.setText(R.string.actualizar);
                binding.btnRegistrarUsuario.setEnabled(true);

                if (resultado != null) {
                    if (resultado.equals("Datos actualizados correctamente")) {
                        mostrarToast("Perfil actualizado correctamente");
                        usuarioViewModel.cerrarSesion();
                        irALogin();
                    } else if (resultado.equals("Contraseña actual incorrecta")) {
                        mostrarToast("La contraseña actual es incorrecta");
                    } else if (resultado.equals("La nueva contraseña no puede ser igual a la actual")) {
                        mostrarToast("La nueva contraseña debe ser diferente a la actual");
                    } else if (resultado.startsWith("Error")) {
                        mostrarToast(resultado);
                    }
                }
            });

        });
    }

    private void uploadImageAndUpdateUser(String nombreImagen, String contrasenaActual,
                                          String nuevoNombre, String email, String nuevaContrasena) {
        try {
            // Usar ImageUtils para convertir URI a File
            File imageFile = ImageUtils.getFileFromUri(requireContext(), imageUri);

            uploadImage(imageFile, nombreImagen).observe(getViewLifecycleOwner(), nuevaUrl -> {
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
        usuarioViewModel.getDatosUsuario().observe(getViewLifecycleOwner(), datosUsuario -> {
            if (datosUsuario != null) {
                binding.textoNombre.setText(usuarioViewModel.getNombreUsuario(datosUsuario));

                String email = usuarioViewModel.getEmailUsuario(datosUsuario);
                if (email != null) {
                    binding.textoEmail.setText(email);
                    binding.textoEmail.setEnabled(false);
                }

                currentImageUrl = usuarioViewModel.getUrlImagenPerfilUsuario(datosUsuario);
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

        String fileName = obtenerNombreArchivo(fileUrl);
        if (fileName == null) {
            resultLiveData.postValue(false);
            return resultLiveData;
        }

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

    public LiveData<String> uploadImage(File imageFile, String customFileName) {
        MutableLiveData<String> liveDataUrl = new MutableLiveData<>();
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", customFileName, requestFile);

        // Llamada a la API de Supabase
        // Param 1: Tu API KEY (Autenticación)
        // Param 2: nombre de tu bucket
        // Param 3: nombre con el que se creará el fichero en Supabase
        // Param 4: cuerpo de la petición (imagen)
        Call<Void> call = supabaseStorageApi.uploadImage(
                "Bearer " + SUPABASE_AUTH_TOKEN,
                BUCKET_NAME,
                customFileName,
                body
        );;

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    String fileUrl = response.raw().request().url().toString();
                    liveDataUrl.postValue(fileUrl);
                } else {
                    liveDataUrl.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                liveDataUrl.postValue(null);
            }
        });
        return liveDataUrl;
    }

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