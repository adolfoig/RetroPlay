package com.example.retroplay.ViewModel;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.retroplay.Repository.RankingRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RankingRepositoryInstrumentedTest {

    private RankingRepository rankingRepository;
    private FirebaseAuth auth;
    private CountDownLatch latch;

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        auth = FirebaseAuth.getInstance();
        rankingRepository = new RankingRepository();
        latch = new CountDownLatch(1);
    }

    @Test
    public void testGetIdUsuarioActual_Autenticado() throws InterruptedException {
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = rankingRepository.getIdusuarioActual();
                        assertNotNull(userId);
                        assertFalse(userId.isEmpty());
                    } else {
                        fail("Error en autenticación");
                    }
                    latch.countDown();
                });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void testGetIdUsuarioActual_NoAutenticado() throws InterruptedException {
        auth.signOut();

        // Pequeño delay para asegurar el signOut
        Thread.sleep(1000);

        String userId = rankingRepository.getIdusuarioActual();
        assertNull(userId);
    }

    @Test
    public void testCargarJuegos() throws InterruptedException {
        rankingRepository.cargarJuegos()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        assertNotNull(task.getResult());
                        assertFalse(task.getResult().isEmpty());
                    } else {
                        fail("Error al cargar juegos: " + task.getException());
                    }
                    latch.countDown();
                });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void testCargarPuntuaciones() throws InterruptedException {
        // Asume que existe al menos un juego con ID "1" en tu base de datos
        String testGameId = "1";

        rankingRepository.cargarPuntuaciones(testGameId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        assertNotNull(task.getResult());

                        // Verifica que estén ordenadas descendentemente
                        if (!task.getResult().isEmpty()) {
                            int previousScore = Integer.MAX_VALUE;
                            for (DocumentSnapshot doc : task.getResult()) {
                                int currentScore = doc.getLong("puntuacionMaxima").intValue();
                                assertTrue(currentScore <= previousScore);
                                previousScore = currentScore;
                            }
                        }
                    } else {
                        fail("Error al cargar puntuaciones: " + task.getException());
                    }
                    latch.countDown();
                });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void testGetUsuarioPorId() throws InterruptedException {
        // Primero autenticamos para obtener un ID de usuario válido
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();

                        rankingRepository.getUsuarioPorId(userId)
                                .addOnCompleteListener(userTask -> {
                                    if (userTask.isSuccessful()) {
                                        assertNotNull(userTask.getResult());
                                        assertTrue(userTask.getResult().exists());
                                    } else {
                                        fail("Error al obtener usuario: " + userTask.getException());
                                    }
                                    latch.countDown();
                                });
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(20, TimeUnit.SECONDS)); // Más tiempo para este test
    }
}