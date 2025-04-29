package com.example.retroplay.Repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class LogrosRepositoryInstrumentedTest {

    private LogrosRepository logrosRepository;
    private FirebaseAuth auth;
    private CountDownLatch latch;

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        auth = FirebaseAuth.getInstance();
        logrosRepository = new LogrosRepository();
        latch = new CountDownLatch(1);
    }

    @Test
    public void testGetLogrosDisponibles() throws InterruptedException {
        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        logrosRepository.getLogrosDisponibles()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        assertNotNull(task1.getResult());
                                        assertTrue(task1.getResult().size() >= 0);
                                    } else {
                                        fail("Error al obtener logros disponibles");
                                    }
                                    latch.countDown();
                                });
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testGetLogrosObtenidos() throws InterruptedException {
        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        assertNotNull(user);

                        logrosRepository.getLogrosObtenidos(user.getUid())
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        assertNotNull(task1.getResult());
                                    } else {
                                        fail("Error al obtener logros obtenidos");
                                    }
                                    latch.countDown();
                                });
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testGetUsuarioActual() throws InterruptedException {
        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = logrosRepository.getUsuarioActual();
                        assertNotNull(user);
                        assertEquals(auth.getCurrentUser().getUid(), user.getUid());
                    } else {
                        fail("Error en autenticación");
                    }
                    latch.countDown();
                });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }
}