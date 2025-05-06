package com.example.retroplay.Repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
    public void testGetIdUsuarioActual() throws InterruptedException {
        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = rankingRepository.getIdUsuarioActual();
                        assertNotNull(userId);
                        assertEquals(auth.getCurrentUser().getUid(), userId);
                    } else {
                        fail("Error en autenticación");
                    }
                    latch.countDown();
                });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testCargarJuegos() throws InterruptedException {
        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        rankingRepository.cargarJuegos()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        QuerySnapshot result = task1.getResult();
                                        assertNotNull(result);
                                        assertTrue(result.size() >= 0);
                                    } else {
                                        fail("Error al cargar juegos");
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
    public void testCargarPuntuaciones() throws InterruptedException {
        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Usamos un juego de prueba (asegúrate que exista en tu Firestore)
                        String testGameId = "1";
                        rankingRepository.cargarPuntuaciones(testGameId)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        QuerySnapshot result = task1.getResult();
                                        assertNotNull(result);

                                        // Verificar que está ordenado descendentemente
                                        if (result.size() > 1) {
                                            long previousScore = -1;
                                            for (DocumentSnapshot doc : result.getDocuments()) {
                                                long currentScore = doc.getLong("puntuacionMaxima");
                                                assertTrue(currentScore <= previousScore || previousScore == -1);
                                                previousScore = currentScore;
                                            }
                                        }
                                    } else {
                                        fail("Error al cargar puntuaciones");
                                    }
                                    latch.countDown();
                                });
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(15, TimeUnit.SECONDS)); // Más tiempo para esta prueba
    }

    @Test
    public void testGetUsuarioPorId() throws InterruptedException {
        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Usamos el ID del usuario actual para la prueba
                        String testUserId = auth.getCurrentUser().getUid();
                        rankingRepository.getUsuarioPorId(testUserId)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        DocumentSnapshot result = task1.getResult();
                                        assertNotNull(result);
                                        assertTrue(result.exists());
                                    } else {
                                        fail("Error al obtener usuario");
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
}