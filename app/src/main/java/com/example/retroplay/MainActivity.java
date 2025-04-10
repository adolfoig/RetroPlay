package com.example.retroplay;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.retroplay.Registro.LoginFragment;
import com.example.retroplay.databinding.ActivityMainBinding;
import com.example.retroplay.databinding.NavHeaderBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    ActivityMainBinding binding;

    NavHeaderBinding bindingHeader;
    private FirebaseAuth mAuth;
    NavController navController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((binding = ActivityMainBinding.inflate(getLayoutInflater())).getRoot());

        // Bloquear el giro de pantalla
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();



        // Configura el NavController (usa la variable de clase)
        this.navController = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment))
                .getNavController();

        if (currentUser == null) {
            navController.navigate(R.id.loginFragment); // Usa la navegación del NavController
            ocultarInterfaz2();
        } else {
            irAlBottomMenu();
        }


        // Configura el AppBarConfiguration con los fragments deseados
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.juegosFragment
        )
                .setOpenableLayout(binding.drawerLayout)
                .build();

        // Vincula el Toolbar y el BottomNavigationView
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNavView, navController);

        NavigationView navigationView = findViewById(R.id.navigation_drawer);
        navigationView.setNavigationItemSelectedListener(this);

        setupNavListener();
    }

    private void irAlBottomMenu() {
        setSupportActionBar(binding.toolbar);
        NavController navController = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavView, navController);
        NavigationUI.setupWithNavController(binding.toolbar, navController);
    }

    private void ocultarInterfaz2() {
        // Ocultamos el Toolbar y BottomNavigation
        binding.bottomNavView.setVisibility(View.GONE);
        binding.toolbar.setVisibility(View.GONE);
    }

    void mostrarInterfaz2() {
        // Mostramos el Toolbar y BottomNavigation
        binding.bottomNavView.setVisibility(View.VISIBLE);
        binding.toolbar.setVisibility(View.VISIBLE);
    }

    private void ocultarBottomNavView(){
        binding.bottomNavView.setVisibility(View.GONE);
    }

    // Método para ver si estás en el fragment jugarJuegos, login o registro y ocultar el menú
    private void setupNavListener() {
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();

            // Solo habilitar el Drawer en el fragmento juegosFragment
            boolean shouldEnableDrawer = destinationId == R.id.juegosFragment;

            binding.drawerLayout.setDrawerLockMode(
                    shouldEnableDrawer ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED
            );


            // Oculta o muestra la interfaz según el fragmento actual
            if (destinationId == R.id.loginFragment || destinationId == R.id.registroFragment || destinationId == R.id.jugarJuegoFragment) {
                ocultarInterfaz2();
            } else {
                mostrarInterfaz2(); // Muestra la interfaz por defecto en otros fragmentos
            }
        });
    }



    private void abrirFragment(Fragment fragment) {
        // Reemplazamos el fragmento actual con el nuevo fragmento
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_actualizarUsuario) {
            abrirFragment(new ActualizarUsuarioFragment());
            ocultarBottomNavView();
        } else if (itemId == R.id.nav_cerrarSesion) {
            abrirFragment(new CerrarSesionFragment());
            ocultarBottomNavView();
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if(binding.drawerLayout.isDrawerOpen(GravityCompat.START)){
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
