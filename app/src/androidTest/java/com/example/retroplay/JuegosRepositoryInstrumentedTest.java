package com.example.retroplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.lifecycle.MutableLiveData;
import androidx.test.core.app.ApplicationProvider;

import com.example.retroplay.Repository.JuegosRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class JuegosRepositoryInstrumentedTest {

    private JuegosRepository juegosRepository;
    private FirebaseAuth auth;
    private CountDownLatch latch;

    @Before
    public void setUp() {
        // Inicializa Firebase en el contexto de prueba
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        auth = FirebaseAuth.getInstance();
        juegosRepository = new JuegosRepository();
        latch = new CountDownLatch(1); // Para sincronizar pruebas asíncronas
    }

    @Test
    public void testGetJuegos() throws InterruptedException {
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        juegosRepository.getJuegos().observeForever(juegos -> {
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

    @Test
    public void testObtenerPuntuacion() throws InterruptedException {
        // Primero nos autenticamos
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        MutableLiveData<Integer> puntuacionLiveData = new MutableLiveData<>();
                        MutableLiveData<String> errorLiveData = new MutableLiveData<>();

                        String idJuegoTest = "1";

                        // Llamamos al método real (sin mocking)
                        juegosRepository.obtenerPuntuacion(idJuegoTest, puntuacionLiveData, errorLiveData);

                        // Observamos los resultados
                        puntuacionLiveData.observeForever(score -> {
                            if (score != null) {
                                assertNotNull(score);
                                // Como no mockeamos, no sabemos el valor exacto esperado
                                // pero podemos verificar que es un número válido
                                assertTrue(score >= 0);
                                latch.countDown();
                            }
                        });

                        errorLiveData.observeForever(error -> {
                            if (error != null) {
                                fail("Error obteniendo puntuación: " + error);
                                latch.countDown();
                            }
                        });
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void testGuardarPuntuacion() throws InterruptedException {
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        MutableLiveData<Boolean> successLiveData = new MutableLiveData<>();

                        String idJuegoTest = "1";
                        int puntuacionTest = 100;

                        juegosRepository.guardarPuntuacion(idJuegoTest, puntuacionTest, successLiveData);

                        successLiveData.observeForever(success -> {
                            if (success != null) {
                                assertTrue(success);
                                latch.countDown();
                            }
                        });
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void testActualizarPuntuacion() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        MutableLiveData<Boolean> successLiveData = new MutableLiveData<>();

                        // Suponiendo que ya existe un documento con este ID en la colección "Puntuaciones"
                        String docId = "YBhSkaMPdyCijdOi5kBu"; // Cambia esto por el ID real del documento
                        int nuevaPuntuacion = 200;
                        String fecha = "2025-04-24";

                        db.collection("Puntuaciones").document(docId).get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        juegosRepository.actualizarPuntuacion(doc, nuevaPuntuacion, fecha, successLiveData);

                                        successLiveData.observeForever(success -> {
                                            if (success != null) {
                                                assertTrue(success);
                                                latch.countDown();
                                            }
                                        });
                                    } else {
                                        fail("Documento de prueba no encontrado");
                                        latch.countDown();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    fail("Error obteniendo documento: " + e.getMessage());
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
    public void testCrearPuntuacion() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        FirebaseUser usuario = auth.getCurrentUser();
                        assertNotNull(usuario);

                        MutableLiveData<Boolean> successLiveData = new MutableLiveData<>();

                        String idUsuario = usuario.getUid();
                        String idJuego = "juegoTestCrear";
                        int puntuacion = 120;
                        String fecha = "2025-04-24";

                        juegosRepository.crearPuntuacion(idUsuario, idJuego, puntuacion, fecha, successLiveData);

                        successLiveData.observeForever(success -> {
                            if (success != null) {
                                assertTrue(success);
                                latch.countDown();
                            }
                        });

                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue("El test superó el tiempo de espera", latch.await(10, TimeUnit.SECONDS));
    }

    private void esperarLogroGuardado(String idUsuario, String idLogro, CountDownLatch latch, FirebaseFirestore db, int intentosRestantes) {
        if (intentosRestantes <= 0) {
            fail("El logro no se guardó a tiempo");
            latch.countDown();
            return;
        }

        db.collection("LogrosUsuario")
                .whereEqualTo("idUsuario", idUsuario)
                .whereEqualTo("idLogro", idLogro)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        assertTrue(true);
                        latch.countDown();
                    } else {
                        // Reintenta luego de 500 ms
                        new Thread(() -> {
                            try {
                                Thread.sleep(500);
                                esperarLogroGuardado(idUsuario, idLogro, latch, db, intentosRestantes - 1);
                            } catch (InterruptedException e) {
                                fail("Error esperando logro: " + e.getMessage());
                                latch.countDown();
                            }
                        }).start();
                    }
                })
                .addOnFailureListener(e -> {
                    fail("Error consultando logro: " + e.getMessage());
                    latch.countDown();
                });
    }


    @Test
    public void testVerificarYGuardarLogro() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        FirebaseUser usuario = auth.getCurrentUser();
                        assertNotNull(usuario);

                        String idUsuario = usuario.getUid();
                        String logro = "logro_test";
                        String idJuego = "juego_test";

                        // Verificamos que no exista el logro primero (para limpieza)
                        db.collection("LogrosObtenidos")
                                .whereEqualTo("idUsuario", idUsuario)
                                .whereEqualTo("idLogro", logro)
                                .get()
                                .addOnCompleteListener(cleanupTask -> {
                                    if (cleanupTask.isSuccessful()) {
                                        // Eliminamos si ya existe (para empezar limpio)
                                        for (QueryDocumentSnapshot doc : cleanupTask.getResult()) {
                                            doc.getReference().delete();
                                        }

                                        // Ejecutamos el test
                                        juegosRepository.verificarYGuardarLogro(idUsuario, logro, idJuego);

                                        // Verificamos después de un breve retardo
                                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                            db.collection("LogrosObtenidos")
                                                    .whereEqualTo("idUsuario", idUsuario)
                                                    .whereEqualTo("idLogro", logro)
                                                    .get()
                                                    .addOnCompleteListener(verificationTask -> {
                                                        if (verificationTask.isSuccessful()) {
                                                            assertFalse("El logro debería haberse creado",
                                                                    verificationTask.getResult().isEmpty());
                                                            latch.countDown();
                                                        } else {
                                                            fail("Error al verificar creación de logro");
                                                            latch.countDown();
                                                        }
                                                    });
                                        }, 2000); // Espera 2 segundos para la operación async
                                    } else {
                                        fail("Error en limpieza inicial");
                                        latch.countDown();
                                    }
                                });
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue("El test superó el tiempo de espera", latch.await(10, TimeUnit.SECONDS));
    }

}
