package com.example.retroplay.Registro;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.example.retroplay.MainActivity;
import com.example.retroplay.R;
import com.example.retroplay.databinding.FragmentLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private FirebaseAuth mAuth;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private GoogleSignInClient googleSignInClient;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflamos la vista y configuramos el binding
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        configurarClienteGoogleSignIn();
        inicializarLauncherGoogleSignIn();

        // Evento para el botón de Google Sign-In
        binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        // Evento para inicio de sesión
        binding.loginButton.setOnClickListener(v -> loginUsuario());

        // Evento para abrir el fragmento de registro
        binding.registerTextView.setOnClickListener(v -> irRegistroActivity());

        // Listener para cerrar el teclado cuando el usuario toque fuera del campo de texto
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

        return binding.getRoot(); // Retornamos la vista inflada
    }

    private void configurarClienteGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Usa tu Web client ID
                .requestEmail()
                //.setAccountName(null)
                .build();

        googleSignInClient = GoogleSignIn.getClient(getActivity(), gso);
    }

    private void inicializarLauncherGoogleSignIn() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d("GoogleSignIn", "Resultado recibido del intent de Google Sign-In");
                    Log.d("GoogleSignIn", "ResultCode: " + result.getResultCode());

                    Intent data = result.getData();
                    if (data != null) {
                        Log.d("GoogleSignIn", "Intent data: " + data.toString());
                        Bundle extras = data.getExtras();
                        if (extras != null) {
                            for (String key : extras.keySet()) {
                                Log.d("GoogleSignIn", "Extra [" + key + "]: " + extras.get(key));
                            }
                        }
                    }

                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        try {
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            if (task.isSuccessful()) {
                                Log.d("GoogleSignIn", "Cuenta obtenida correctamente del intent");
                                gestionarResultadoSignIn(task);
                            } else {
                                Exception e = task.getException();
                                Log.e("GoogleSignIn", "Fallo en getSignedInAccountFromIntent", e);
                                Toast.makeText(getActivity(), "Error al obtener cuenta de Google", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("GoogleSignIn", "Excepción procesando el intent: " + e.getMessage(), e);
                            Toast.makeText(getActivity(), "Error al procesar el resultado", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), "Error en el inicio de sesión", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }


    private void signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void gestionarResultadoSignIn(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount cuenta = task.getResult(ApiException.class);
            Log.d("GoogleSignIn", "Inicio de sesión con Google exitoso. Usuario: " + cuenta.getEmail());
            firebaseAuthWithGoogle(cuenta);
        } catch (ApiException e) {
            Log.e("GoogleSignIn", "Error al iniciar sesión con Google. Código: " + e.getStatusCode(), e);
            Toast.makeText(getActivity(), "Error en el inicio de sesión con Google: ", Toast.LENGTH_SHORT).show();
        }
    }


    private void firebaseAuthWithGoogle(GoogleSignInAccount cuenta) {
        AuthCredential credential = GoogleAuthProvider.getCredential(cuenta.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Verificar si es un nuevo usuario (primer inicio de sesión)
                            if (task.getResult().getAdditionalUserInfo().isNewUser()) {
                                registrarUsuarioEnFirestore(user, cuenta.getDisplayName());
                            }
                            Toast.makeText(getActivity(), "Inicio de sesión con Google exitoso", Toast.LENGTH_SHORT).show();
                            irAMain();
                        }
                    } else {
                        Log.e("GoogleSignIn", "Error en firebaseAuthWithGoogle", task.getException());
                        Toast.makeText(getActivity(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registrarUsuarioEnFirestore(FirebaseUser user, String displayName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Obtener el email del usuario
        String email = user.getEmail();

        // Si no hay nombre, extraer la parte antes del @ del email
        String nombreUsuario;
        if (displayName != null && !displayName.isEmpty()) {
            nombreUsuario = displayName; // Usar el nombre de Google si existe
        } else {
            // Extraer la parte antes del @ (si el email es válido)
            nombreUsuario = (email != null && email.contains("@"))
                    ? email.substring(0, email.indexOf("@"))
                    : "Usuario Google";
        }

        // Crear el mapa de datos para Firestore
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("email", email);
        usuario.put("nombre", nombreUsuario); // Nombre personalizado o parte del email

        // Registrar/actualizar en Firestore
        db.collection("Usuarios")
                .document(user.getUid())
                .set(usuario)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Usuario registrado con Google"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error al registrar usuario", e));
    }

    private void irAMain() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private void loginUsuario() {
        String email = binding.emailEditText.getText().toString().trim();
        String contrasena = binding.passwordEditText.getText().toString().trim();

        // Validación de campos vacíos
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(contrasena)) {
            Toast.makeText(getActivity(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.loginButton.setText(R.string.iniciandosesion);
        if(!email.contains("@")){
            Toast.makeText(getActivity(), "El correo tiene que contener un @", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.loginButton.setText(R.string.iniciandosesion);
        binding.loginButton.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, contrasena)
                .addOnCompleteListener(getActivity(), task -> {

                    binding.loginButton.setText(R.string.iniciarSesion);
                    binding.loginButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser usuario = mAuth.getCurrentUser();
                        Toast.makeText(getActivity(), "Inicio de sesión exitoso: " + usuario.getEmail(), Toast.LENGTH_SHORT).show();
                        irAMain();
                    } else {
                        Toast.makeText(getActivity(), "Inicio de sesión incorrecto", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void irRegistroActivity() {
        Intent intent = new Intent(getActivity(), RegistroActivity.class);
        startActivity(intent);
    }
}