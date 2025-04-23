package com.example.retroplay;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.retroplay.Model.Juego;
import com.example.retroplay.Repository.FavoritosRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FavoritosRepositoryInstrumentedTest {

    private FavoritosRepository repository;
    private FirebaseAuth auth;
    private CountDownLatch latch;

    @Before
    public void setUp() {
        // Inicializa Firebase en el contexto de prueba
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        auth = FirebaseAuth.getInstance();
        repository = new FavoritosRepository();
        latch = new CountDownLatch(1); // Para sincronizar pruebas asíncronas
    }

    @Test
    public void testAgregarYEliminarFavorito() throws InterruptedException {
        // Autentica un usuario de prueba (ejemplo: correo/contraseña)
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String idJuegoTest = "1"; // ID de un juego de prueba

                        // Crear un juego de prueba con el mismo ID
                        Juego juegoTest = new Juego();
                        juegoTest.setId(idJuegoTest);
                        juegoTest.setNombre("Pacman");
                        juegoTest.setDescripcion("Descripción de prueba");
                        juegoTest.setRutaImagen("ruta/imagen.jpg");
                        juegoTest.setRutaArchivo("ruta/archivo.rom");

                        // Test: Agregar a favoritos
                        repository.agregarFavorito(idJuegoTest, success -> {
                            assertTrue(success);

                            // Test: Verificar que está en favoritos
                            repository.verificarEstadoFavorito(juegoTest, esFavorito -> {
                                assertTrue(esFavorito);

                                // Test: Eliminar de favoritos
                                repository.eliminarFavorito(idJuegoTest, successDelete -> {
                                    assertTrue(successDelete);

                                    // Verificar que ya no está en favoritos
                                    repository.verificarEstadoFavorito(juegoTest, yaNoEsFavorito -> {
                                        assertFalse(yaNoEsFavorito);
                                        latch.countDown(); // Libera el latch al finalizar
                                    });
                                });
                            });
                        });
                    } else {
                        fail("Error en autenticación de prueba");
                        latch.countDown();
                    }
                });

        latch.await(10, TimeUnit.SECONDS); // Espera máxima para evitar bloqueos
    }

    @Test
    public void testCargarFavoritos() throws InterruptedException {
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        repository.cargarFavoritos();
                        repository.getFavoritosLiveData().observeForever(juegos -> {
                            assertNotNull(juegos);
                            latch.countDown();
                        });
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        latch.await(10, TimeUnit.SECONDS);
    }
}
