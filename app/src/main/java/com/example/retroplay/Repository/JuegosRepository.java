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
    public void fetchScore(String gameId, MutableLiveData<Integer> scoreLiveData, MutableLiveData<String> errorLiveData) {
        executorService.execute(() -> {
            try {
                String result = getScoreFromServer();
                JSONObject jsonObject = new JSONObject(result);
                int score = jsonObject.getInt("score");
                scoreLiveData.postValue(score);
                saveScore(gameId, score, new MutableLiveData<>());
            } catch (JSONException e) {
                errorLiveData.postValue("Error al parsear JSON: " + e.getMessage());
            }
        });
    }

    private String getScoreFromServer() {
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

    public void saveScore(String gameId, int score, MutableLiveData<Boolean> successLiveData) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        String fechaActual = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        db.collection("Puntuaciones")
                .whereEqualTo("idUsuario", userId)
                .whereEqualTo("idJuego", gameId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        updateExistingScore(task.getResult().getDocuments().get(0), score, fechaActual, successLiveData);
                    } else {
                        createNewScore(userId, gameId, score, fechaActual, successLiveData);
                    }
                });
    }

    private void updateExistingScore(DocumentSnapshot doc, int score, String fecha, MutableLiveData<Boolean> successLiveData) {
        int maxScore = doc.getLong("puntuacionMaxima").intValue();
        String maxScoreDate = doc.getString("fechaPuntuacionMaxima");

        int newMaxScore = Math.max(score, maxScore);
        HashMap<String, Object> data = new HashMap<>();
        data.put("puntuacionActual", (long) score);
        data.put("fechaPuntuacionActual", fecha);
        data.put("puntuacionMaxima", (long) newMaxScore);
        data.put("fechaPuntuacionMaxima", newMaxScore > maxScore ? fecha : maxScoreDate);

        db.collection("Puntuaciones")
                .document(doc.getId())
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    checkAchievements(doc.getString("idJuego"), score);
                    successLiveData.postValue(true);
                })
                .addOnFailureListener(e -> successLiveData.postValue(false));
    }

    private void createNewScore(String userId, String gameId, int score, String fecha, MutableLiveData<Boolean> successLiveData) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("idUsuario", userId);
        data.put("idJuego", gameId);
        data.put("puntuacionActual", (long) score);
        data.put("fechaPuntuacionActual", fecha);
        data.put("puntuacionMaxima", (long) score);
        data.put("fechaPuntuacionMaxima", fecha);

        db.collection("Puntuaciones")
                .add(data)
                .addOnSuccessListener(ref -> successLiveData.postValue(true))
                .addOnFailureListener(e -> successLiveData.postValue(false));
    }

    private void checkAchievements(String gameId, int score) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("LogrosDisponibles")
                .whereEqualTo("idJuego", gameId)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Logro logro = doc.toObject(Logro.class);
                        if (logro != null && score >= logro.getPuntuacion()) {
                            verifyAndSaveAchievement(user.getUid(), doc.getId(), gameId);
                        }
                    }
                });
    }

    private void verifyAndSaveAchievement(String userId, String achievementId, String gameId) {
        db.collection("LogrosObtenidos")
                .whereEqualTo("idUsuario", userId)
                .whereEqualTo("idLogro", achievementId)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("idUsuario", userId);
                        data.put("idLogro", achievementId);
                        data.put("fechaObtencion", new Date());
                        data.put("idJuego", gameId);

                        db.collection("LogrosObtenidos").add(data);
                    }
                });
    }

    public void cleanup() {
        executorService.shutdown();
    }
}