package com.example.retroplay.ViewModel;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.retroplay.Repository.RankingRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RankingViewModelInstrumentedTest {

    private RankingRepository rankingRepository;
    private FirebaseAuth auth;
    private CountDownLatch latch;

    private final String emailTest = "pruebaTest@gmail.com";
    private final String passwordTest = "123456";

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        auth = FirebaseAuth.getInstance();
        rankingRepository = new RankingRepository();
        latch = new CountDownLatch(1);
    }

    private void signInOrCreateTestUser(Runnable onSuccess, Runnable onFailure) {
        auth.signInWithEmailAndPassword(emailTest, passwordTest)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        onSuccess.run();
                    } else {
                        // Si falla login, intentamos crear
                        auth.createUserWithEmailAndPassword(emailTest, passwordTest)
                                .addOnCompleteListener(createTask -> {
                                    if (createTask.isSuccessful()) {
                                        createUserDocument(createTask.getResult().getUser(), onSuccess);
                                    } else {
                                        // Puede fallar por colisión (usuario ya creado), entonces volvemos a intentar login
                                        if (createTask.getException() instanceof FirebaseAuthUserCollisionException) {
                                            auth.signInWithEmailAndPassword(emailTest, passwordTest)
                                                    .addOnCompleteListener(secondLogin -> {
                                                        if (secondLogin.isSuccessful()) {
                                                            onSuccess.run();
                                                        } else {
                                                            onFailure.run();
                                                        }
                                                    });
                                        } else {
                                            onFailure.run();
                                        }
                                    }
                                });
                    }
                });
    }

    private void createUserDocument(FirebaseUser user, Runnable onSuccess) {
        if (user != null) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("nombre", "Usuario Prueba");
            userData.put("email", emailTest);
            userData.put("fotoPerfil", "https://example.com/foto.jpg");

            FirebaseFirestore.getInstance().collection("Usuarios").document(user.getUid())
                    .set(userData)
                    .addOnCompleteListener(task -> onSuccess.run());
        } else {
            fail("Usuario es null después de crear");
            latch.countDown();
        }
    }

    @Test
    public void testGetIdUsuarioActual_Autenticado() throws InterruptedException {
        signInOrCreateTestUser(() -> {
            String userId = rankingRepository.getIdUsuarioActual();
            assertNotNull(userId);
            assertFalse(userId.isEmpty());
            latch.countDown();
        }, () -> {
            fail("Error en autenticación");
            latch.countDown();
        });

        assertTrue(latch.await(20, TimeUnit.SECONDS));
    }

    @Test
    public void testGetIdUsuarioActual_NoAutenticado() throws InterruptedException {
        auth.signOut();
        Thread.sleep(1000);
        String userId = rankingRepository.getIdUsuarioActual();
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
        String testGameId = "1";

        rankingRepository.cargarPuntuaciones(testGameId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        assertNotNull(task.getResult());

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
        signInOrCreateTestUser(() -> {
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
        }, () -> {
            fail("Error en autenticación");
            latch.countDown();
        });

        assertTrue(latch.await(20, TimeUnit.SECONDS));
    }
}
