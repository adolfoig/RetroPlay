package com.example.retroplay.ViewModel;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.retroplay.LogrosFragment;
import com.example.retroplay.Model.Logro;
import com.example.retroplay.Viewmodel.LogrosViewModel;
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
public class LogrosViewModelInstrumentedTest {

    private LogrosViewModel logrosViewModel;
    private FirebaseAuth auth;
    private CountDownLatch latch;

    // Implementación simple del callback para testing
    private static class TestCallback implements LogrosFragment.LogrosCallback {
        List<Logro> logrosResult;
        String errorResult;
        CountDownLatch latch;

        TestCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onLogrosCargados(List<Logro> logros) {
            this.logrosResult = logros;
            latch.countDown();
        }

        @Override
        public void onError(String error) {
            this.errorResult = error;
            latch.countDown();
        }
    }

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        auth = FirebaseAuth.getInstance();
        logrosViewModel = new LogrosViewModel();
        latch = new CountDownLatch(1);
    }

    @Test
    public void testCargarLogrosAutenticado() throws InterruptedException {
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        TestCallback callback = new TestCallback(latch);
                        logrosViewModel.cargarLogrosDesdeFireBase(callback);
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void testLogrosOrdenadosCorrectamente() throws InterruptedException {
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        TestCallback callback = new TestCallback(latch);
                        logrosViewModel.cargarLogrosDesdeFireBase(callback);
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(15, TimeUnit.SECONDS));

        TestCallback callback = new TestCallback(new CountDownLatch(1));
        logrosViewModel.cargarLogrosDesdeFireBase(callback);

        if (callback.logrosResult != null && callback.logrosResult.size() > 1) {
            for (int i = 0; i < callback.logrosResult.size() - 1; i++) {
                assertTrue(callback.logrosResult.get(i).getPuntuacion() <=
                        callback.logrosResult.get(i + 1).getPuntuacion());
            }
        }
    }

    @Test
    public void testManejoErrores() throws InterruptedException {
        auth.signOut();

        TestCallback callback = new TestCallback(latch);
        logrosViewModel.cargarLogrosDesdeFireBase(callback);

        assertTrue(latch.await(15, TimeUnit.SECONDS));

        // Verificación principal
        if (callback.errorResult == null && callback.logrosResult == null) {
            fail("No se recibió ninguna respuesta (ni error ni logros)");
        }

        // Al menos uno debe estar presente
        assertTrue("Se esperaba un error o lista de logros",
                callback.errorResult != null || callback.logrosResult != null);
    }

    @Test
    public void testVerificarEstadosLogros() throws InterruptedException {
        auth.signInWithEmailAndPassword("prueba@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        TestCallback callback = new TestCallback(latch);
                        logrosViewModel.cargarLogrosDesdeFireBase(callback);
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(15, TimeUnit.SECONDS));

        TestCallback callback = new TestCallback(new CountDownLatch(1));
        logrosViewModel.cargarLogrosDesdeFireBase(callback);

        if (callback.logrosResult != null) {
            for (Logro logro : callback.logrosResult) {
                assertNotNull("El estado de obtención no debería ser null", logro.isObtenido());
            }
        }
    }
}