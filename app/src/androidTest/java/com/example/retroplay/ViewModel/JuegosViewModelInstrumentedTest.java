package com.example.retroplay.ViewModel;

import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.retroplay.Model.Juego;
import com.example.retroplay.Viewmodel.JuegosViewModel;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class JuegosViewModelInstrumentedTest {

    private JuegosViewModel juegosViewModel;
    private FirebaseAuth auth;
    private CountDownLatch latch;

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        auth = FirebaseAuth.getInstance();
        juegosViewModel = new JuegosViewModel(ApplicationProvider.getApplicationContext());
        latch = new CountDownLatch(1);
    }

    @Test
    public void testGetJuegos() throws InterruptedException {
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        juegosViewModel.getJuegos().observeForever(new Observer<List<Juego>>() {
                            @Override
                            public void onChanged(List<Juego> juegos) {
                                if (juegos != null) {
                                    assertNotNull(juegos);
                                    assertFalse(juegos.isEmpty());
                                    latch.countDown();
                                    juegosViewModel.getJuegos().removeObserver(this);
                                }
                            }
                        });
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void testFetchScore() throws InterruptedException {
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Probamos con un ID de juego conocido
                        String testGameId = "1";

                        juegosViewModel.getScore().observeForever(score -> {
                            if (score != null) {
                                assertNotNull(score);
                                latch.countDown();
                            }
                        });

                        juegosViewModel.getError().observeForever(error -> {
                            if (error != null) {
                                fail("Error al obtener puntuación: " + error);
                                latch.countDown();
                            }
                        });

                        juegosViewModel.fetchScore(testGameId);
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void testErrorHandling() throws InterruptedException {
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Probamos con un ID de juego inválido para forzar error
                        String invalidGameId = "invalid_id";

                        juegosViewModel.getError().observeForever(error -> {
                            if (error != null) {
                                assertNotNull(error);
                                assertFalse(error.isEmpty());
                                latch.countDown();
                            }
                        });

                        juegosViewModel.fetchScore(invalidGameId);
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }
}