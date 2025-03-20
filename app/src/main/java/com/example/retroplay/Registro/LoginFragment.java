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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

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

        // Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        configurarClienteGoogleSignIn();
        inicializarLauncherGoogleSignIn();

        // Evento para el botón de Google Sign-In
        binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        // Evento para inicio de sesión
        binding.loginButton.setOnClickListener(v -> loginUser());

        // Evento para abrir el fragmento de registro
        binding.registerTextView.setOnClickListener(v -> irRegistroActivity());

        // Listener para cerrar el teclado cuando el usuario toque fuera del campo de texto
        binding.getRoot().setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View currentFocus = getActivity().getCurrentFocus();
                if (currentFocus != null) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
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
                .build();

        googleSignInClient = GoogleSignIn.getClient(getActivity(), gso);
    }

    private void inicializarLauncherGoogleSignIn() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        gestionarResultadoSignIn(task);
                    } else {
                        Toast.makeText(getActivity(), "Error en el inicio de sesión con Google", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void gestionarResultadoSignIn(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            Log.e("GoogleSignIn", "Error al iniciar sesión con Google: " + e.getStatusCode() + " - " + e.getMessage());
            Toast.makeText(getActivity(), "Error en el inicio de sesión con Google: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), task -> {
                    if (task.isSuccessful()) {
                        Log.d("GoogleSignIn", "Inicio de sesión con Google exitoso");
                        Toast.makeText(getActivity(), "Inicio de sesión con Google exitoso", Toast.LENGTH_SHORT).show();
                        redirectToMain();
                    } else {
                        Log.e("GoogleSignIn", "Error en firebaseAuthWithGoogle", task.getException());
                        Toast.makeText(getActivity(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void redirectToMain() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish(); // Añadir esta línea
    }

    private void loginUser() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        // Validación de campos vacíos
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getActivity(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!email.contains("@")){
            Toast.makeText(getActivity(), "El correo tiene que contener un @", Toast.LENGTH_SHORT).show();
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser usuario = mAuth.getCurrentUser();
                        Toast.makeText(getActivity(), "Inicio de sesión exitoso: " + usuario.getEmail(), Toast.LENGTH_SHORT).show();
                        redirectToMain();
                    } else {
                        Toast.makeText(getActivity(), "Login incorrecto ", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void irRegistroActivity() {
        Intent intent = new Intent(getActivity(), RegistroActivity.class);
        startActivity(intent);
    }
}
