package com.example.retroplay.Registro;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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

        // Listener para cerrar el teclado
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View currentFocus = requireActivity().getCurrentFocus();
                if (currentFocus != null) {
                    currentFocus.clearFocus();
                    InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                    }
                }
            }
            return false;
        });

        return view;
    }

    private void configurarClienteGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
    }

    private void inicializarLauncherGoogleSignIn() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == requireActivity().RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        manejarResultadoGoogleSignIn(task);
                    } else {
                        Toast.makeText(requireContext(), "Error en inicio de sesión con Google", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void manejarResultadoGoogleSignIn(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account);
            } else {
                Toast.makeText(requireContext(), "Error: Cuenta de Google no válida", Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            Log.e("GoogleSignIn", "Error al iniciar sesión con Google", e);
            Toast.makeText(requireContext(), "Error al autenticar con Google: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount cuenta) {
        AuthCredential credential = GoogleAuthProvider.getCredential(cuenta.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (task.getResult().getAdditionalUserInfo() != null &&
                                    task.getResult().getAdditionalUserInfo().isNewUser()) {
                                registrarUsuarioEnFirestore(user, cuenta.getDisplayName());
                            }
                            Toast.makeText(requireContext(), "Inicio de sesión con Google exitoso", Toast.LENGTH_SHORT).show();
                            irAMain();
                        }
                    } else {
                        Log.e("GoogleSignIn", "Error en firebaseAuthWithGoogle", task.getException());
                        Toast.makeText(requireContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registrarUsuarioEnFirestore(FirebaseUser user, String displayName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String email = user.getEmail();

        String nombreUsuario = (displayName != null && !displayName.isEmpty()) ?
                displayName :
                (email != null && email.contains("@") ?
                        email.substring(0, email.indexOf("@")) :
                        "Usuario Google");

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("email", email);
        usuario.put("nombre", nombreUsuario);

        db.collection("Usuarios")
                .document(user.getUid())
                .set(usuario)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Usuario registrado con Google"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error al registrar usuario", e));
    }

    private void signInWithGoogle() {
        Intent intent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(intent);
    }

    private void irAMain() {
        Intent intent = new Intent(requireActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void loginUsuario() {
        String email = binding.emailEditText.getText().toString().trim();
        String contrasena = binding.passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(contrasena)) {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!email.contains("@")) {
            Toast.makeText(requireContext(), "El correo debe contener un @", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, contrasena)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser usuario = mAuth.getCurrentUser();
                        Toast.makeText(requireContext(), "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                        irAMain();
                    } else {
                        Toast.makeText(requireContext(), "Login incorrecto", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void irRegistroActivity() {
        Intent intent = new Intent(requireActivity(), RegistroActivity.class);
        startActivity(intent);
    }
}