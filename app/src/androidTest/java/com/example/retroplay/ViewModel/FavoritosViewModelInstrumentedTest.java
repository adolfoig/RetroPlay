package com.example.retroplay.ViewModel;

import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.retroplay.Model.Juego;
import com.example.retroplay.Viewmodel.FavoritosViewModel;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

import android.os.Looper;

@RunWith(AndroidJUnit4.class)
public class FavoritosViewModelInstrumentedTest {

    private FavoritosViewModel favoritosViewModel;
    private FirebaseAuth auth;
    private CountDownLatch latch;

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        auth = FirebaseAuth.getInstance();
        favoritosViewModel = new FavoritosViewModel();
        latch = new CountDownLatch(1);
    }

    @Test
    public void testAlternarFavorito() throws InterruptedException {
        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Juego juegoTest = new Juego();
                        juegoTest.setId("1");
                        juegoTest.setNombre("Pacman");
                        juegoTest.setFavorito(false);

                        // Observar los mensajes del ViewModel
                        favoritosViewModel.getFavoritoAgregado().observeForever(message -> {
                            if (message != null && message.equals("Juego añadido a favoritos")) {
                                assertTrue(juegoTest.isFavorito());
                                favoritosViewModel.alternarFavorito(juegoTest);
                            }
                        });

                        favoritosViewModel.getJuegoEliminado().observeForever(message -> {
                            if (message != null && message.equals("Juego eliminado de favoritos")) {
                                assertFalse(juegoTest.isFavorito());
                                latch.countDown();
                            }
                        });
                        favoritosViewModel.alternarFavorito(juegoTest);

                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void testCargarFavoritos() throws InterruptedException {
        auth.signInWithEmailAndPassword("pruebaTest@gmail.com", "123456")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        favoritosViewModel.getFavoritos().observeForever(juegos -> {
                            if (juegos != null) {
                                assertNotNull(juegos);
                                latch.countDown();
                            }
                        });

                        favoritosViewModel.cargarFavoritos();
                    } else {
                        fail("Error en autenticación");
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testSeleccionarJuego() throws InterruptedException {
        Juego juegoTest = new Juego();
        juegoTest.setId("1");
        juegoTest.setNombre("Pacman");

        runOnUiThread(() -> {
            favoritosViewModel.seleccionarJuego(juegoTest);

            favoritosViewModel.getJuegoSeleccionado().observeForever(new Observer<Juego>() {
                @Override
                public void onChanged(Juego juego) {
                    if (juego != null) {
                        assertEquals(juegoTest.getId(), juego.getId());
                        assertEquals(juegoTest.getNombre(), juego.getNombre());
                        latch.countDown();

                        favoritosViewModel.getJuegoSeleccionado().removeObserver(this);
                    }
                }
            });
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    private void runOnUiThread(Runnable action) {
        if (isMainThread()) {
            action.run();
        } else {
            CountDownLatch uiLatch = new CountDownLatch(1);
            ApplicationProvider.getApplicationContext().getMainExecutor().execute(() -> {
                action.run();
                uiLatch.countDown();
            });
            try {
                uiLatch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                fail("Interrupción en el hilo UI");
            }
        }
    }

    // Método helper para verificar si estamos en el hilo principal
    private boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}