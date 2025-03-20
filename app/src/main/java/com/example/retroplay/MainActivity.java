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

        // Configura el Toolbar como la ActionBar
        setSupportActionBar(binding.toolbar);

        // Configura el NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        // Configura el NavController para que pueda manejar las acciones de navegación
        NavigationUI.setupActionBarWithNavController(this, navController);

        // Inicializamos FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Verificamos si el usuario está autenticado
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Si no está logueado, mostramos el fragmento de Login
            mostrarLoginFragment();
            // Ocultamos el Toolbar y el BottomNavigation
            ocultarInterfaz2();
        } else {
            // Si está logueado, mostramos el BottomNavigation y el Toolbar
            irAlBottomMenu();
        }

        setupNavListener();
    }


    private void mostrarLoginFragment() {
        // Si el usuario no está autenticado, ocultamos la interfaz
        ocultarInterfaz2();

        // Reemplazamos el fragmento de Login
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, new LoginFragment()) // Asegúrate de que el contenedor esté correctamente definido en el layout
                .commit();
    }

    private void irAlBottomMenu() {
        setSupportActionBar(binding.toolbar);
        NavController navController = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavView, navController);
        NavigationUI.setupWithNavController(binding.toolbar, navController);
    }

    private void ocultarInterfaz() {
        // Ponerlo en 0dp para ocultarlo
        ViewGroup.LayoutParams appBarParams = binding.appBarLayout.getLayoutParams();
        appBarParams.height = 0;
        binding.appBarLayout.setLayoutParams(appBarParams);
        binding.toolbar.setVisibility(View.GONE);


        ViewGroup.LayoutParams toolbarParams = binding.toolbar.getLayoutParams();
        toolbarParams.height = 0;
        binding.toolbar.setLayoutParams(toolbarParams);
        binding.toolbar.setVisibility(View.GONE);

        ViewGroup.LayoutParams bottomNavParams = binding.bottomNavView.getLayoutParams();
        bottomNavParams.height = 0;
        binding.bottomNavView.setLayoutParams(bottomNavParams);
        binding.bottomNavView.setVisibility(View.GONE);
    }

    private void ocultarInterfaz2() {
        // Ocultamos el Toolbar y BottomNavigation
        binding.appBarLayout.setVisibility(View.GONE);
        binding.bottomNavView.setVisibility(View.GONE);
    }

    private void mostrarInterfaz2() {
        // Mostramos el Toolbar y BottomNavigation
        binding.appBarLayout.setVisibility(View.VISIBLE);
        binding.bottomNavView.setVisibility(View.VISIBLE);
    }

    public void mostrarInterfaz() {
        // Restaurar altura y visibilidad de AppBarLayout
        ViewGroup.LayoutParams appBarParams = binding.appBarLayout.getLayoutParams();
        appBarParams.height = getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
        binding.appBarLayout.setLayoutParams(appBarParams);
        binding.appBarLayout.setVisibility(View.VISIBLE);

        // Restaurar altura y visibilidad de Toolbar
        ViewGroup.LayoutParams toolbarParams = binding.toolbar.getLayoutParams();
        toolbarParams.height = getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
        binding.toolbar.setLayoutParams(toolbarParams);
        binding.toolbar.setVisibility(View.VISIBLE);

        // Restaurar altura y visibilidad de BottomNavView
        ViewGroup.LayoutParams bottomNavParams = binding.bottomNavView.getLayoutParams();
        bottomNavParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        binding.bottomNavView.setLayoutParams(bottomNavParams);
        binding.bottomNavView.setVisibility(View.VISIBLE);
    }

    // Método para ver si estás en el fragment jugarJuegos, login o registro y ocultar el menú
    private void setupNavListener() {
        NavController navController = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController();

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.jugarJuegoFragment) {
                ocultarInterfaz();
            } else if(destination.getId() == R.id.loginButton){
                ocultarInterfaz2();
            }
            else if(destination.getId() == R.id.registroFragment){
                ocultarInterfaz2();
            }
        });
    }

    private void setupNavListener2() {
        NavController navController = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController();

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() != R.id.juegosFragment) {
                mostrarInterfaz();
            } else if(destination.getId() == R.id.favoritosFragment){
                mostrarInterfaz();
            } else if(destination.getId() == R.id.rankingFragment){
                mostrarInterfaz();
            } else if(destination.getId() == R.id.logrosFragment){
                mostrarInterfaz();
            }
        });
    }



}
