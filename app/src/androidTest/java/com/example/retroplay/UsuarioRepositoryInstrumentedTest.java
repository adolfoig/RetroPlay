package com.example.retroplay;

import static org.junit.Assert.*;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.retroplay.Repository.UsuarioRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class UsuarioRepositoryInstrumentedTest {

    private UsuarioRepository usuarioRepository;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CountDownLatch latch;
    private final String CONTRASENA_PRUEBA = "123456";
    private String idUsuario;
    private String email;

    @Before
    public void setUp() {
        try {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            usuarioRepository = new UsuarioRepository();
            email = "usuario" + System.currentTimeMillis() + "@example.com";
        } catch (Exception e) {
            fail("Error en la configuración inicial: " + e.getMessage());
        }
    }

    @After
    public void tearDown() throws InterruptedException {
        CountDownLatch cleanupLatch = new CountDownLatch(1);
        if (auth.getCurrentUser() != null) {
            auth.getCurrentUser().delete()
                    .addOnCompleteListener(task -> {
                        if (idUsuario != null) {
                            db.collection("Usuarios").document(idUsuario).delete()
                                    .addOnCompleteListener(t -> cleanupLatch.countDown());
                        } else {
                            cleanupLatch.countDown();
                        }
                    });
        } else {
            cleanupLatch.countDown();
        }
        cleanupLatch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void testRegistrarUsuarioFirebase() throws InterruptedException {
        latch = new CountDownLatch(1);

        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            usuarioRepository.registrarUsuarioFirebase(email, CONTRASENA_PRUEBA);

            usuarioRepository.getRegistroExitoso().observeForever(success -> {
                if (success != null) {
                    if (success) {
                        FirebaseUser user = auth.getCurrentUser();
                        assertNotNull("El usuario no debería ser nulo después del registro", user);
                        assertEquals("El email no coincide", email, user.getEmail());
                        idUsuario = user.getUid();
                    }
                    latch.countDown();
                }
            });

            usuarioRepository.getErrorRegistro().observeForever(error -> {
                if (error != null) {
                    fail("Error en el registro: " + error);
                    latch.countDown();
                }
            });
        });

        assertTrue("Tiempo de espera agotado", latch.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void testGuardarDatosUsuarioFirestore() throws InterruptedException {
        latch = new CountDownLatch(1);

        auth.createUserWithEmailAndPassword(email, CONTRASENA_PRUEBA)
                .addOnCompleteListener(createTask -> {
                    if (createTask.isSuccessful()) {
                        idUsuario = auth.getCurrentUser().getUid();

                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            usuarioRepository.guardarDatosUsuarioFirestore(
                                    idUsuario,
                                    "Usuario de Prueba",
                                    email,
                                    "https://example.com/profile.jpg"
                            );

                            db.collection("Usuarios").document(idUsuario)
                                    .get()
                                    .addOnCompleteListener(verifyTask -> {
                                        if (verifyTask.isSuccessful()) {
                                            DocumentSnapshot doc = verifyTask.getResult();
                                            assertTrue("El documento debería existir", doc.exists());
                                            assertEquals("El nombre no coincide",
                                                    "Usuario de Prueba", doc.getString("nombre"));
                                            assertEquals("El email no coincide",
                                                    email, doc.getString("email"));
                                        } else {
                                            fail("Error al verificar los datos: " +
                                                    verifyTask.getException().getMessage());
                                        }
                                        latch.countDown();
                                    });
                        });
                    } else {
                        fail("Error al crear usuario de prueba: " +
                                createTask.getException().getMessage());
                        latch.countDown();
                    }
                });

        assertTrue("Tiempo de espera agotado", latch.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void testCargarDatosUsuario() throws InterruptedException {
        latch = new CountDownLatch(1);

        auth.createUserWithEmailAndPassword(email, CONTRASENA_PRUEBA)
                .addOnCompleteListener(createTask -> {
                    if (createTask.isSuccessful()) {
                        idUsuario = auth.getCurrentUser().getUid();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("nombre", "Usuario de Prueba");
                        userData.put("email", email);
                        userData.put("UrlImagenPerfil", "https://example.com/profile.jpg");

                        db.collection("Usuarios").document(idUsuario)
                                .set(userData)
                                .addOnCompleteListener(setTask -> {
                                    if (setTask.isSuccessful()) {
                                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                            usuarioRepository.cargarDatosUsuario();

                                            usuarioRepository.getDatosUsuario().observeForever(userInfo -> {
                                                if (userInfo != null) {
                                                    assertEquals("El nombre no coincide",
                                                            "Usuario de Prueba", userInfo.get("nombre"));
                                                    assertEquals("El email no coincide",
                                                            email, userInfo.get("email"));
                                                    assertEquals("La URL de la imagen no coincide",
                                                            "https://example.com/profile.jpg",
                                                            userInfo.get("urlImagen"));
                                                    latch.countDown();
                                                }
                                            });
                                        });
                                    } else {
                                        fail("Error al configurar datos de prueba: " +
                                                setTask.getException().getMessage());
                                        latch.countDown();
                                    }
                                });
                    } else {
                        fail("Error al crear usuario de prueba: " +
                                createTask.getException().getMessage());
                        latch.countDown();
                    }
                });

        assertTrue("Tiempo de espera agotado", latch.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void testActualizarUsuario() throws InterruptedException {
        latch = new CountDownLatch(1);

        auth.createUserWithEmailAndPassword(email, CONTRASENA_PRUEBA)
                .addOnCompleteListener(createTask -> {
                    if (createTask.isSuccessful()) {
                        idUsuario = auth.getCurrentUser().getUid();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("nombre", "Nombre Antiguo");
                        userData.put("email", email);

                        db.collection("Usuarios").document(idUsuario)
                                .set(userData)
                                .addOnCompleteListener(setTask -> {
                                    if (setTask.isSuccessful()) {
                                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                            usuarioRepository.actualizarUsuario(
                                                    CONTRASENA_PRUEBA,
                                                    "Nombre Nuevo",
                                                    email,
                                                    null,
                                                    "https://example.com/nueva_foto.jpg"
                                            );

                                            usuarioRepository.getResultadoActualizacionUsuario()
                                                    .observeForever(result -> {
                                                        if (result != null) {
                                                            if (result.equals("Datos actualizados correctamente")) {
                                                                db.collection("Usuarios").document(idUsuario)
                                                                        .get()
                                                                        .addOnCompleteListener(verifyTask -> {
                                                                            if (verifyTask.isSuccessful()) {
                                                                                DocumentSnapshot doc = verifyTask.getResult();
                                                                                assertEquals("El nombre no se actualizó correctamente",
                                                                                        "Nombre Nuevo", doc.getString("nombre"));
                                                                                assertEquals("La URL de la foto no se actualizó correctamente",
                                                                                        "https://example.com/nueva_foto.jpg",
                                                                                        doc.getString("UrlImagenPerfil"));
                                                                            } else {
                                                                                fail("Error al verificar la actualización: " +
                                                                                        verifyTask.getException().getMessage());
                                                                            }
                                                                            latch.countDown();
                                                                        });
                                                            } else if (result.startsWith("Error")) {
                                                                fail(result);
                                                                latch.countDown();
                                                            }
                                                        }
                                                    });
                                        });
                                    } else {
                                        fail("Error al configurar datos de prueba: " +
                                                setTask.getException().getMessage());
                                        latch.countDown();
                                    }
                                });
                    } else {
                        fail("Error al crear usuario de prueba: " +
                                createTask.getException().getMessage());
                        latch.countDown();
                    }
                });

        assertTrue("Tiempo de espera agotado", latch.await(40, TimeUnit.SECONDS));
    }

    @Test
    public void testGetUsuarioPorId() throws InterruptedException {
        latch = new CountDownLatch(1);

        auth.createUserWithEmailAndPassword(email, CONTRASENA_PRUEBA)
                .addOnCompleteListener(createTask -> {
                    if (createTask.isSuccessful()) {
                        idUsuario = auth.getCurrentUser().getUid();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("nombre", "Usuario de Prueba");
                        userData.put("email", email);

                        db.collection("Usuarios").document(idUsuario)
                                .set(userData)
                                .addOnCompleteListener(setTask -> {
                                    if (setTask.isSuccessful()) {
                                        usuarioRepository.getUsuarioPorId(idUsuario)
                                                .addOnCompleteListener(getTask -> {
                                                    if (getTask.isSuccessful()) {
                                                        DocumentSnapshot doc = getTask.getResult();
                                                        assertTrue("El documento debería existir", doc.exists());
                                                        assertEquals("El nombre no coincide",
                                                                "Usuario de Prueba", doc.getString("nombre"));
                                                        assertEquals("El email no coincide",
                                                                email, doc.getString("email"));
                                                    } else {
                                                        fail("Error al obtener usuario: " +
                                                                getTask.getException().getMessage());
                                                    }
                                                    latch.countDown();
                                                });
                                    } else {
                                        fail("Error al configurar datos de prueba: " +
                                                setTask.getException().getMessage());
                                        latch.countDown();
                                    }
                                });
                    } else {
                        fail("Error al crear usuario de prueba: " +
                                createTask.getException().getMessage());
                        latch.countDown();
                    }
                });

        assertTrue("Tiempo de espera agotado", latch.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void testCerrarSesion() throws InterruptedException {
        latch = new CountDownLatch(1);

        auth.createUserWithEmailAndPassword(email, CONTRASENA_PRUEBA)
                .addOnCompleteListener(createTask -> {
                    if (createTask.isSuccessful()) {
                        idUsuario = auth.getCurrentUser().getUid();

                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            usuarioRepository.cerrarSesion();

                            usuarioRepository.getResultadoActualizacionUsuario()
                                    .observeForever(result -> {
                                        if (result != null && result.equals("logout_success")) {
                                            assertNull("El usuario debería ser nulo después de cerrar sesión",
                                                    auth.getCurrentUser());
                                            latch.countDown();
                                        }
                                    });
                        });
                    } else {
                        fail("Error al crear usuario de prueba: " +
                                createTask.getException().getMessage());
                        latch.countDown();
                    }
                });

        assertTrue("Tiempo de espera agotado", latch.await(30, TimeUnit.SECONDS));
    }
}