package com.example.retroplay;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.example.retroplay.Registro.LoginFragment;
import com.example.retroplay.Viewmodel.UsuarioViewModel;
import com.example.retroplay.databinding.ActivityMainBinding;
import com.example.retroplay.databinding.NavHeaderBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    ActivityMainBinding binding;
    NavHeaderBinding headerBinding;
    private FirebaseAuth mAuth;
    NavController navController;
    private UsuarioViewModel usuarioViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        usuarioViewModel = new ViewModelProvider(this).get(UsuarioViewModel.class);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        setContentView((binding = ActivityMainBinding.inflate(getLayoutInflater())).getRoot());

        headerBinding = NavHeaderBinding.bind(binding.navigationDrawer.getHeaderView(0));

        // Bloquear giro de pantalla
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        this.navController = ((NavHostFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)))
                .getNavController();

        if (currentUser == null) {
            ocultarInterfaz();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new LoginFragment())
                    .commit();
        } else {
            irAlBottomMenu();
        }

        // Configura el AppBarConfiguration con los fragments deseados
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.juegosFragment
        )
                .setOpenableLayout(binding.drawerLayout)
                .build();

        datosUsuarioHeaderDrawer();

        // Vincula el Toolbar y el BottomNavigationView
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNavView, navController);


        NavigationView navigationView = findViewById(R.id.navigation_drawer);
        navigationView.setNavigationItemSelectedListener(this);

        setupNavListener();
        configurarObservadoresUsuario();
    }

    private void irAlBottomMenu() {
        setSupportActionBar(binding.toolbar);
        NavController navController = ((NavHostFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment))).getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavView, navController);
        NavigationUI.setupWithNavController(binding.toolbar, navController);
    }

    private void ocultarInterfaz() {
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

    void ocultarInterfaz2() {
        // Ocultamos el Toolbar y BottomNavigation
        binding.bottomNavView.setVisibility(View.GONE);
        binding.toolbar.setVisibility(View.GONE);
    }

    void mostrarInterfaz2() {
        // Mostramos el Toolbar y BottomNavigation
        binding.bottomNavView.setVisibility(View.VISIBLE);
        binding.toolbar.setVisibility(View.VISIBLE);

    }
    void mostrarToolBar() {
        // Mostramos el Toolbar y BottomNavigation
        binding.toolbar.setVisibility(View.VISIBLE);

    }

    private void ocultarBottomNavView(){
        binding.bottomNavView.setVisibility(View.GONE);
    }

    // Metodo para ver si estás en el fragment jugarJuegos, login o registro y ocultar el menú
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
            } else if(destinationId == R.id.detailFragment){
                ocultarBottomNavView();
            }
            else {
                // Muestra la interfaz por defecto en otros fragmentos
                mostrarInterfaz2();
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        binding.drawerLayout.closeDrawer(GravityCompat.START);

        // Verificar si el usuario inició sesión con Google
        FirebaseUser currentUser = mAuth.getCurrentUser();
        boolean isGoogleUser = currentUser != null && !currentUser.getProviderData().isEmpty()
                && currentUser.getProviderData().get(1).getProviderId().equals("google.com");

        // Usamos post para asegurar que la navegación ocurra después de cerrar el drawer
        binding.getRoot().post(() -> {
            if (itemId == R.id.nav_actualizarUsuario) {
                if (isGoogleUser) {
                    // Mostrar mensaje indicando que no se puede actualizar cuenta de Google
                    Toast.makeText(this, "No puedes actualizar una cuenta de Google", Toast.LENGTH_SHORT).show();
                } else {
                    navController.navigate(R.id.actualizarUsuarioFragment);
                    ocultarBottomNavView();
                }
            } else if (itemId == R.id.nav_cerrarSesion) {
                navController.navigate(R.id.cerrarSesionFragment);
                ocultarBottomNavView();
            }
        });

        return true;
    }

    private void configurarObservadoresUsuario() {
        usuarioViewModel.getDatosUsuario().observe(this, datosUsuario -> {
            if (datosUsuario != null) {
                // Actualizar nombre
                String nombre = datosUsuario.get("nombre");
                headerBinding.nombreUsuario.setText(nombre != null ? nombre : "Usuario");

                // Actualizar email
                String email = datosUsuario.get("email");
                if (email != null) {
                    headerBinding.emailUsuario.setText(email);
                }

                // Actualizar imagen
                String imageUrl = datosUsuario.get("urlImagen");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(this)
                            .load(imageUrl)
                            .circleCrop()
                            .placeholder(R.drawable.logo)
                            .error(R.drawable.logo)
                            .into(headerBinding.imageView);
                } else {
                    // Si no hay URL en Firestore, intentar con la foto de Google
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null && user.getPhotoUrl() != null) {
                        Glide.with(this)
                                .load(user.getPhotoUrl())
                                .circleCrop()
                                .into(headerBinding.imageView);
                    } else {
                        headerBinding.imageView.setImageResource(R.drawable.logo);
                    }
                }
            }
        });
    }

    private void datosUsuarioHeaderDrawer() {
        usuarioViewModel.cargarDatosUsuario();
    }
}
