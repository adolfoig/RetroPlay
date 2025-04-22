package com.example.retroplay.Repository;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.retroplay.Model.Juego;
import com.example.retroplay.Model.Logro;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JuegosRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final String IP = "172.25.0.1";  // Dirección IP del servidor
    private final int PUERTO = 3000;  // Puerto del servidor
    private final String URL = "http://" + IP + ":" + PUERTO + "/score";  // Crear la URL

    // Métodos existentes para obtener juegos
    public MutableLiveData<List<Juego>> getJuegos() {
        MutableLiveData<List<Juego>> juegosLiveData = new MutableLiveData<>();
        List<Juego> listaJuegos = new ArrayList<>();

        db.collection("Juegos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Juego juego = document.toObject(Juego.class);
                            listaJuegos.add(juego);
                        }
                        juegosLiveData.setValue(listaJuegos);
                    }
                });
        return juegosLiveData;
    }

    // Nuevos métodos para manejar puntuaciones
    public void obtenerPuntuacion(String idJuego, MutableLiveData<Integer> puntuacionLiveData, MutableLiveData<String> errorLiveData) {
        executorService.execute(() -> {
            try {
                String result = cargarPuntuacionDelServidor();
                JSONObject jsonObject = new JSONObject(result);
                int score = jsonObject.getInt("score");
                puntuacionLiveData.postValue(score);
                guardarPuntuacion(idJuego, score, new MutableLiveData<>());
            } catch (JSONException e) {
                Log.d("Puntuacion","Error al parsear JSON: " + e.getMessage());
            }
        });
    }

    private String cargarPuntuacionDelServidor() {
        StringBuilder result = new StringBuilder();
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            }

        } catch (Exception e) {
            Log.e("Error", "Error al obtener puntuación: " + e.getMessage());
        } finally {
            try {
                if (reader != null) reader.close();
                if (conn != null) conn.disconnect();
            } catch (Exception e) {
                Log.e("Error", "Error al cerrar conexión: " + e.getMessage());
            }
        }
        return "";
    }

    public void guardarPuntuacion(String idJuego, int puntuacion, MutableLiveData<Boolean> successLiveData) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) return;

        String idUsuario = usuario.getUid();
        String fechaActual = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        db.collection("Puntuaciones")
                .whereEqualTo("idUsuario", idUsuario)
                .whereEqualTo("idJuego", idJuego)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        actualizarPuntuacion(task.getResult().getDocuments().get(0), puntuacion, fechaActual, successLiveData);
                    } else {
                        crearPuntuacion(idUsuario, idJuego, puntuacion, fechaActual, successLiveData);
                    }
                });
    }

    private void actualizarPuntuacion(DocumentSnapshot doc, int puntuacion, String fecha, MutableLiveData<Boolean> successLiveData) {
        int puntuacionMaxima = doc.getLong("puntuacionMaxima").intValue();
        String fechaPuntuacionMaxima = doc.getString("fechaPuntuacionMaxima");

        int nuevaPuntuacionMaxima = Math.max(puntuacion, puntuacionMaxima);
        HashMap<String, Object> data = new HashMap<>();
        data.put("puntuacionActual", (long) puntuacion);
        data.put("fechaPuntuacionActual", fecha);
        data.put("puntuacionMaxima", (long) nuevaPuntuacionMaxima);
        data.put("fechaPuntuacionMaxima", nuevaPuntuacionMaxima > puntuacionMaxima ? fecha : fechaPuntuacionMaxima);

        db.collection("Puntuaciones")
                .document(doc.getId())
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    verificarLogro(doc.getString("idJuego"), puntuacion);
                    successLiveData.postValue(true);
                })
                .addOnFailureListener(e -> successLiveData.postValue(false));
    }

    private void crearPuntuacion(String idUsuario, String idJuego, int puntuacion, String fecha, MutableLiveData<Boolean> successLiveData) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("idUsuario", idUsuario);
        data.put("idJuego", idJuego);
        data.put("puntuacionActual", (long) puntuacion);
        data.put("fechaPuntuacionActual", fecha);
        data.put("puntuacionMaxima", (long) puntuacion);
        data.put("fechaPuntuacionMaxima", fecha);

        db.collection("Puntuaciones")
                .add(data)
                .addOnSuccessListener(ref -> successLiveData.postValue(true))
                .addOnFailureListener(e -> successLiveData.postValue(false));
    }

    private void verificarLogro(String idJuego, int puntuacion) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) return;

        db.collection("LogrosDisponibles")
                .whereEqualTo("idJuego", idJuego)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Logro logro = doc.toObject(Logro.class);
                        if (logro != null && puntuacion >= logro.getPuntuacion()) {
                            verificarYGuardarlogro(usuario.getUid(), doc.getId(), idJuego);
                        }
                    }
                });
    }

    private void verificarYGuardarlogro(String idUsuario, String logro, String idJuego) {
        db.collection("LogrosObtenidos")
                .whereEqualTo("idUsuario", idUsuario)
                .whereEqualTo("idLogro", logro)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("idUsuario", idUsuario);
                        data.put("idLogro", logro);
                        data.put("fechaObtencion", new Date());
                        data.put("idJuego", idJuego);

                        db.collection("LogrosObtenidos").add(data);
                    }
                });
    }

    public void cleanup() {
        executorService.shutdown();
    }
}