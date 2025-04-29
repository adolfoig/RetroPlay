package com.example.retroplay.ViewModel;

import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.retroplay.Viewmodel.UsuarioViewModel;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UsuarioViewModelInstrumentedTest {

    private UsuarioViewModel usuarioViewModel;
    private FirebaseAuth auth;
    private CountDownLatch latch;
    private String testEmail;
    private String testPassword = "test123";
    private String testNombre = "TestUser";
    private String testImagen = "http://example.com/test.jpg";

    @Before
    public void setUp() throws InterruptedException {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        auth = FirebaseAuth.getInstance();
        usuarioViewModel = new UsuarioViewModel();
        latch = new CountDownLatch(1);

        // Crear un usuario de prueba antes de cada test
        testEmail = "test_user_" + System.currentTimeMillis() + "@example.com";

        CountDownLatch registerLatch = new CountDownLatch(1);

        runOnUiThread(() -> {
            usuarioViewModel.getRegistroExitoso().observeForever(success -> {
                if (success != null && success) {
                    registerLatch.countDown();
                }
            });

            usuarioViewModel.getErrorRegistro().observeForever(error -> {
                if (error != null) {
                    fail("Failed to register user: " + error);
                    registerLatch.countDown();
                }
            });

            usuarioViewModel.registrarUsuario(testNombre, testEmail, testPassword, testImagen);
        });

        assertTrue(registerLatch.await(30, TimeUnit.SECONDS));

        // Ahora iniciar sesión
        CountDownLatch loginLatch = new CountDownLatch(1);
        auth.signInWithEmailAndPassword(testEmail, testPassword)
                .addOnCompleteListener(task -> loginLatch.countDown());
        assertTrue(loginLatch.await(30, TimeUnit.SECONDS));
    }

    @After
    public void tearDown() {
        // Eliminar el usuario de prueba
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.delete();
        }
    }

    private void runOnUiThread(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    @Test
    public void testCargarDatosUsuario() throws InterruptedException {
        runOnUiThread(() -> {
            Observer<Map<String, String>> observer = new Observer<Map<String, String>>() {
                @Override
                public void onChanged(Map<String, String> userData) {
                    if (userData != null) {
                        try {
                            assertNotNull(userData);
                            assertTrue(userData.containsKey("nombre"));
                            assertTrue(userData.containsKey("email"));
                        } finally {
                            latch.countDown();
                            usuarioViewModel.getDatosUsuario().removeObserver(this);
                        }
                    }
                }
            };
            usuarioViewModel.getDatosUsuario().observeForever(observer);
            usuarioViewModel.cargarDatosUsuario();
        });

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void testActualizarUsuario() throws InterruptedException {
        runOnUiThread(() -> {
            Observer<String> observer = new Observer<String>() {
                @Override
                public void onChanged(String resultado) {
                    if (resultado != null) {
                        try {
                            assertNotNull(resultado);
                            assertFalse(resultado.isEmpty());
                        } finally {
                            latch.countDown();
                            usuarioViewModel.getResultadoActualizacion().removeObserver(this);
                        }
                    }
                }
            };
            usuarioViewModel.getResultadoActualizacion().observeForever(observer);

            usuarioViewModel.actualizarUsuario(
                    auth.getCurrentUser().getUid(),
                    "NuevoNombre",
                    testEmail,
                    "nuevoPassword123",
                    "http://nuevaimagen.com/perfil.jpg"
            );
        });

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void testRegistrarUsuario() throws InterruptedException {
        String newTestEmail = "test_registrar_" + System.currentTimeMillis() + "@example.com";

        CountDownLatch registrarLatch = new CountDownLatch(1);

        runOnUiThread(() -> {
            Observer<Boolean> successObserver = new Observer<Boolean>() {
                @Override
                public void onChanged(Boolean success) {
                    if (success != null && success) {
                        try {
                            assertTrue(success);
                        } finally {
                            registrarLatch.countDown();
                            usuarioViewModel.getRegistroExitoso().removeObserver(this);
                        }
                    }
                }
            };

            Observer<String> errorObserver = new Observer<String>() {
                @Override
                public void onChanged(String error) {
                    if (error != null) {
                        try {
                            fail("Registration error: " + error);
                        } finally {
                            registrarLatch.countDown();
                            usuarioViewModel.getErrorRegistro().removeObserver(this);
                        }
                    }
                }
            };

            usuarioViewModel.getRegistroExitoso().observeForever(successObserver);
            usuarioViewModel.getErrorRegistro().observeForever(errorObserver);

            usuarioViewModel.registrarUsuario("NuevoUsuario", newTestEmail, "nuevo123", "http://example.com/nueva.jpg");
        });

        assertTrue(registrarLatch.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void testMetodosAyuda() throws InterruptedException {
        runOnUiThread(() -> {
            Observer<Map<String, String>> observer = new Observer<Map<String, String>>() {
                @Override
                public void onChanged(Map<String, String> userData) {
                    if (userData != null) {
                        try {
                            String email = usuarioViewModel.getEmailUsuario(userData);
                            assertNotNull(email);
                            assertFalse(email.isEmpty());

                            String nombre = usuarioViewModel.getNombreUsuario(userData);
                            assertNotNull(nombre);
                            assertFalse(nombre.isEmpty());

                            String urlImagen = usuarioViewModel.getUrlImagenPerfilUsuario(userData);
                            assertNotNull(urlImagen);
                        } finally {
                            latch.countDown();
                            usuarioViewModel.getDatosUsuario().removeObserver(this);
                        }
                    }
                }
            };
            usuarioViewModel.getDatosUsuario().observeForever(observer);
            usuarioViewModel.cargarDatosUsuario();
        });

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void testCerrarSesion() throws InterruptedException {
        runOnUiThread(() -> {
            usuarioViewModel.cerrarSesion();
            latch.countDown();
        });

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertNull(auth.getCurrentUser());
    }
}
