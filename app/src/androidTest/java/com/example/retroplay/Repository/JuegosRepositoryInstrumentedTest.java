package com.example.retroplay.Repository;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.lifecycle.MutableLiveData;
import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class JuegosRepositoryInstrumentedTest {

    private JuegosRepository juegosRepository;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CountDownLatch latch;

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        juegosRepository = new JuegosRepository();
    }

    @After
    public void tearDown() {
        juegosRepository.cleanup();
    }

    @Test
    public void testGetJuegos() throws InterruptedException {
        latch = new CountDownLatch(1);
        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        juegosRepository.getJuegos().observeForever(juegos -> {
                            assertNotNull(juegos);
                            assertTrue(juegos.size() > 0);
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
    public void testObtenerPuntuacion() throws InterruptedException {
        latch = new CountDownLatch(1);
        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        MutableLiveData<Integer> puntuacionLiveData = new MutableLiveData<>();
                        MutableLiveData<String> errorLiveData = new MutableLiveData<>();

                        String idJuegoTest = "1";

                        juegosRepository.obtenerPuntuacion(idJuegoTest, puntuacionLiveData, errorLiveData);

                        puntuacionLiveData.observeForever(score -> {
                            if (score != null) {
                                assertNotNull(score);
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

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testCargarPuntuacionDelServidor() throws InterruptedException {
        latch = new CountDownLatch(1);

        new Thread(() -> {
            try {
                String result = juegosRepository.cargarPuntuacionDelServidor();
                assertNotNull(result);
                assertTrue(result.contains("score") || result.isEmpty());
                latch.countDown();
            } catch (Exception e) {
                fail("Error en cargarPuntuacionDelServidor: " + e.getMessage());
                latch.countDown();
            }
        }).start();

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testGuardarPuntuacion() throws InterruptedException {
        latch = new CountDownLatch(1);
        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
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

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testActualizarPuntuacion() throws InterruptedException {
        latch = new CountDownLatch(1);

        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        MutableLiveData<Boolean> successLiveData = new MutableLiveData<>();

                        // Primero creamos una puntuación para luego actualizarla
                        FirebaseUser user = auth.getCurrentUser();
                        String idJuego = "juegoTestActualizar";
                        int puntuacionInicial = 50;
                        String fecha = "2025-04-24 12:00:00";

                        // Creamos una puntuación para actualizar
                        Map<String, Object> data = new HashMap<>();
                        data.put("idUsuario", user.getUid());
                        data.put("idJuego", idJuego);
                        data.put("puntuacionActual", puntuacionInicial);
                        data.put("fechaPuntuacionActual", fecha);
                        data.put("puntuacionMaxima", puntuacionInicial);
                        data.put("fechaPuntuacionMaxima", fecha);

                        db.collection("Puntuaciones")
                                .add(data)
                                .addOnSuccessListener(documentReference -> {
                                    // Ahora obtenemos el documento para actualizarlo
                                    documentReference.get().addOnSuccessListener(documentSnapshot -> {
                                        int nuevaPuntuacion = 75;
                                        juegosRepository.actualizarPuntuacion(
                                                documentSnapshot,
                                                nuevaPuntuacion,
                                                "2025-04-24 12:30:00",
                                                successLiveData
                                        );

                                        successLiveData.observeForever(success -> {
                                            if (success != null) {
                                                assertTrue(success);
                                                // Limpieza: eliminar el documento de prueba
                                                documentReference.delete();
                                                latch.countDown();
                                            }
                                        });
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    fail("Error creando documento de prueba: " + e.getMessage());
                                    latch.countDown();
                                });
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(20, TimeUnit.SECONDS)); // Más tiempo para esta prueba
    }

    @Test
    public void testCrearPuntuacion() throws InterruptedException {
        latch = new CountDownLatch(1);

        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        FirebaseUser usuario = auth.getCurrentUser();
                        assertNotNull(usuario);

                        MutableLiveData<Boolean> successLiveData = new MutableLiveData<>();

                        String idUsuario = usuario.getUid();
                        String idJuego = "juegoTestCrear" + System.currentTimeMillis(); // ID único
                        int puntuacion = 120;
                        String fecha = "2025-04-24 12:00:00";

                        juegosRepository.crearPuntuacion(idUsuario, idJuego, puntuacion, fecha, successLiveData);

                        successLiveData.observeForever(success -> {
                            if (success != null) {
                                assertTrue(success);

                                // Verificar que realmente se creó
                                db.collection("Puntuaciones")
                                        .whereEqualTo("idUsuario", idUsuario)
                                        .whereEqualTo("idJuego", idJuego)
                                        .get()
                                        .addOnSuccessListener(querySnapshot -> {
                                            assertTrue(!querySnapshot.isEmpty());
                                            // Limpieza: eliminar el documento de prueba
                                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                                doc.getReference().delete();
                                            }
                                            latch.countDown();
                                        })
                                        .addOnFailureListener(e -> {
                                            fail("Error verificando creación: " + e.getMessage());
                                            latch.countDown();
                                        });
                            }
                        });

                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue("El test superó el tiempo de espera", latch.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void testVerificarLogro() throws InterruptedException {
        latch = new CountDownLatch(1);

        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();

                        // Primero necesitamos un juego con logros definidos
                        String idJuegoConLogros = "1"; // Asume que el juego 1 tiene logros definidos
                        int puntuacionAlta = 1000; // Asume que esto desbloquea un logro

                        // Verificamos el logro
                        juegosRepository.verificarLogro(idJuegoConLogros, puntuacionAlta);

                        // Esperamos un momento para que se complete la operación
                        new Thread(() -> {
                            try {
                                Thread.sleep(3000); // Espera suficiente para la operación

                                // Verificamos si se guardó el logro
                                db.collection("LogrosObtenidos")
                                        .whereEqualTo("idUsuario", user.getUid())
                                        .whereEqualTo("idJuego", idJuegoConLogros)
                                        .get()
                                        .addOnSuccessListener(querySnapshot -> {
                                            assertTrue(!querySnapshot.isEmpty());
                                            latch.countDown();
                                        })
                                        .addOnFailureListener(e -> {
                                            fail("Error verificando logro: " + e.getMessage());
                                            latch.countDown();
                                        });
                            } catch (InterruptedException e) {
                                fail("Error en espera: " + e.getMessage());
                                latch.countDown();
                            }
                        }).start();
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void testVerificarYGuardarLogro() throws InterruptedException {
        latch = new CountDownLatch(1);

        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();

                        // Necesitamos un logro existente para probar
                        String idLogroExistente = "logro1"; // Asume que existe este ID en LogrosDisponibles
                        String idJuego = "1";

                        // Primero eliminamos cualquier logro previo para este usuario
                        db.collection("LogrosObtenidos")
                                .whereEqualTo("idUsuario", user.getUid())
                                .whereEqualTo("idLogro", idLogroExistente)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                        doc.getReference().delete();
                                    }

                                    // Ahora probamos verificarYGuardarlogro
                                    juegosRepository.verificarYGuardarlogro(user.getUid(), idLogroExistente, idJuego);

                                    // Esperamos y verificamos
                                    new Thread(() -> {
                                        try {
                                            Thread.sleep(3000);

                                            db.collection("LogrosObtenidos")
                                                    .whereEqualTo("idUsuario", user.getUid())
                                                    .whereEqualTo("idLogro", idLogroExistente)
                                                    .get()
                                                    .addOnSuccessListener(newQuerySnapshot -> {
                                                        assertTrue(!newQuerySnapshot.isEmpty());
                                                        latch.countDown();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        fail("Error verificando logro: " + e.getMessage());
                                                        latch.countDown();
                                                    });
                                        } catch (InterruptedException e) {
                                            fail("Error en espera: " + e.getMessage());
                                            latch.countDown();
                                        }
                                    }).start();
                                })
                                .addOnFailureListener(e -> {
                                    fail("Error limpiando logros previos: " + e.getMessage());
                                    latch.countDown();
                                });
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(20, TimeUnit.SECONDS));
    }
}