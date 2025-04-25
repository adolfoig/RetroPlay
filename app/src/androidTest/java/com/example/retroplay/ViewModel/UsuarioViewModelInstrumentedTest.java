package com.example.retroplay.ViewModel;

import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.retroplay.Viewmodel.UsuarioViewModel;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

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

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        auth = FirebaseAuth.getInstance();
        usuarioViewModel = new UsuarioViewModel();
        latch = new CountDownLatch(1);
    }

    @Test
    public void testCargarDatosUsuario() throws InterruptedException {
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        usuarioViewModel.getDatosUsuario().observeForever(new Observer<Map<String, String>>() {
                            @Override
                            public void onChanged(Map<String, String> userData) {
                                if (userData != null) {
                                    assertNotNull(userData);
                                    assertTrue(userData.containsKey("nombre"));
                                    assertTrue(userData.containsKey("email"));
                                    latch.countDown();
                                    usuarioViewModel.getDatosUsuario().removeObserver(this);
                                }
                            }
                        });
                        usuarioViewModel.cargarDatosUsuario();
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void testActualizarUsuario() throws InterruptedException {
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        usuarioViewModel.getResultadoActualizacion().observeForever(new Observer<String>() {
                            @Override
                            public void onChanged(String resultado) {
                                if (resultado != null) {
                                    assertNotNull(resultado);
                                    assertFalse(resultado.isEmpty());
                                    latch.countDown();
                                    usuarioViewModel.getResultadoActualizacion().removeObserver(this);
                                }
                            }
                        });

                        // Datos de prueba para actualización
                        usuarioViewModel.actualizarUsuario(
                                "123456", // contraseña actual
                                "NuevoNombre",
                                "prueba@gmail.com",
                                "nueva123",
                                "http://imagen.com/perfil.jpg"
                        );
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(20, TimeUnit.SECONDS)); // Más tiempo para esta operación
    }

    @Test
    public void testRegistrarUsuario() throws InterruptedException {
        // Datos de prueba para registro
        String testEmail = "nuevo_usuario_" + System.currentTimeMillis() + "@test.com";
        String testPassword = "test123";
        String testNombre = "UsuarioTest";
        String testImagen = "http://imagen.com/test.jpg";

        // Observador para registro exitoso
        usuarioViewModel.getRegistroExitoso().observeForever(success -> {
            if (success != null) {
                assertTrue(success);
                latch.countDown();
                usuarioViewModel.getRegistroExitoso().removeObserver((Observer<? super Boolean>) this);
            }
        });

        // Observador para errores
        usuarioViewModel.getErrorRegistro().observeForever(error -> {
            if (error != null) {
                fail("Error en registro: " + error);
                latch.countDown();
                usuarioViewModel.getErrorRegistro().removeObserver((Observer<? super String>) this);
            }
        });

        usuarioViewModel.registrarUsuario(testNombre, testEmail, testPassword, testImagen);
        assertTrue(latch.await(25, TimeUnit.SECONDS)); // Más tiempo para registro

        // Limpieza: eliminar usuario de prueba
        auth.signInWithEmailAndPassword(testEmail, testPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        auth.getCurrentUser().delete();
                    }
                });
    }

    @Test
    public void testMetodosAyuda() throws InterruptedException {
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        usuarioViewModel.getDatosUsuario().observeForever(userData -> {
                            if (userData != null) {
                                // Test getEmailUsuario
                                String email = usuarioViewModel.getEmailUsuario(userData);
                                assertNotNull(email);
                                assertFalse(email.isEmpty());

                                // Test getNombreUsuario
                                String nombre = usuarioViewModel.getNombreUsuario(userData);
                                assertNotNull(nombre);
                                assertFalse(nombre.isEmpty());

                                // Test getUrlImagenPerfilUsuario
                                String urlImagen = usuarioViewModel.getUrlImagenPerfilUsuario(userData);
                                assertNotNull(urlImagen); // Puede ser null si no tiene imagen

                                latch.countDown();
                                usuarioViewModel.getDatosUsuario().removeObserver((Observer<? super Map<String, String>>) this);
                            }
                        });
                        usuarioViewModel.cargarDatosUsuario();
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void testCerrarSesion() throws InterruptedException {
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        usuarioViewModel.cerrarSesion();
                        assertNull(auth.getCurrentUser());
                        latch.countDown();
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }
}