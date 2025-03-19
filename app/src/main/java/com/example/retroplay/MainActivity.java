package com.example.retroplay;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.retroplay.Registro.LoginFragment;
import com.example.retroplay.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    AppBarConfiguration mAppBarConfiguration;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((binding = ActivityMainBinding.inflate(getLayoutInflater())).getRoot());

        // Inicializamos FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Verificamos si el usuario está autenticado
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Si no está logueado, mostramos el fragmento de Login
            mostrarLoginFragment();
            // Ocultamos el Toolbar y el BottomNavigation
            ocultarInterfaz();
        } else {
            // Si está logueado, mostramos el BottomNavigation y el Toolbar
            irAlBottomMenu();
            mostrarInterfaz();
        }
        setupNavListener();
    }

    private void mostrarLoginFragment() {
        // Si el usuario no está autenticado, mostramos el LoginFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, new LoginFragment()) // Asegúrate de que el contenedor esté correctamente definido en el layout
                .commit();
    }

    private void irAlBottomMenu(){
        setSupportActionBar(binding.toolbar);
        NavController navController=((NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavView,navController);
        NavigationUI.setupWithNavController(binding.toolbar,navController);

    }

    private void ocultarInterfaz() {
        ViewGroup.LayoutParams appBarParams = binding.appBarLayout.getLayoutParams();
        appBarParams.height = 0;
        binding.appBarLayout.setLayoutParams(appBarParams);

        ViewGroup.LayoutParams toolbarParams = binding.toolbar.getLayoutParams();
        toolbarParams.height = 0;
        binding.toolbar.setLayoutParams(toolbarParams);

        ViewGroup.LayoutParams bottomNavParams = binding.bottomNavView.getLayoutParams();
        bottomNavParams.height = 0;
        binding.bottomNavView.setLayoutParams(bottomNavParams);
        binding.bottomNavView.setVisibility(View.GONE);

    }


    private void mostrarInterfaz() {
        binding.appBarLayout.setVisibility(View.VISIBLE);
        binding.bottomNavView.setVisibility(View.VISIBLE);
    }

    private void setupNavListener() {
        NavController navController = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController();

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.jugarJuegoFragment) {
                ocultarInterfaz();
            } else {
                mostrarInterfaz();
            }
        });
    }
}