package com.example.retroplay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.retroplay.databinding.FragmentJugarJuegoBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JugarJuegoFragment extends Fragment {

    FragmentJugarJuegoBinding binding;
    private WebView gameWebView;
    String idJuego;
    private ExecutorService executorService;
    private FirebaseFirestore firestore;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executorService = Executors.newSingleThreadExecutor();
        firestore = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentJugarJuegoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gameWebView = binding.gameWebView;
        WebSettings webSettings = gameWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // Recoger el id del juego
        Bundle args = getArguments();
        if (args != null) {
            idJuego = args.getString("idJuego");
        }

        // Cargar la URL correspondiente al juego
        if (idJuego != null) {
            switch (idJuego) {
                case "1":
                    gameWebView.loadUrl("file:///android_asset/pacman/index.html");
                    break;
                case "2":
                    gameWebView.loadUrl("file:///android_asset/classic-tetris-js-master/index.html");
                    break;
                case "3":
                    gameWebView.loadUrl("file:///android_asset/flappy-bird-master/index.html");
                    break;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (gameWebView != null) {
            gameWebView.destroy();

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).mostrarInterfaz();
            }

            Log.d("Puntuación", "onDestroy ejecutado");
            fetchScore();  // Obtener y guardar la puntuación al destruir la vista
            executorService.shutdown();
        }
    }

    private void fetchScore() {
        executorService.execute(() -> {
            Log.d("Puntuacion", "Intentando obtener puntuación...");
            String result = getScore("http://192.168.1.42:3000/score");  // Obtener la puntuación desde el servidor

            try {
                JSONObject jsonObject = new JSONObject(result);
                int score = jsonObject.getInt("score");  // Extraer el valor numérico de "score"
                Log.d("Puntuacion", "Resultado de getScore: " + score);

                // Guardar la puntuación en la base de datos Firebase Firestore
                guardarPuntuacion(score);

                // Redirigir al usuario de vuelta a la pantalla principal con la puntuación, si el fragmento está adjunto
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        Intent intent = new Intent(requireActivity(), MainActivity.class);
                        intent.putExtra("puntuacion", result);  // Enviar la puntuación como un extra
                        startActivity(intent);
                    });
                } else {
                    Log.e("Error", "Fragmento no adjunto a la actividad. No se puede iniciar MainActivity");
                }

            } catch (JSONException e) {
                Log.e("Error", "Error al parsear JSON: " + e.getMessage());
            }
        });
    }

    private String getScore(String urlString) {
        StringBuilder result = new StringBuilder();
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            Log.d("Puntuacion", "Código de respuesta HTTP: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                // Parsear el JSON para extraer el valor de "score"
                JSONObject jsonObject = new JSONObject(result.toString());
                int score = jsonObject.getInt("score"); // Extraer el valor numérico
                Log.d("Puntuacion", "Puntuacion: " + score);
                return String.valueOf(score); // Devolver solo el número como String
            } else {
                result.append("Error: Código de respuesta no OK (" + responseCode + ")");
                Log.d("Puntuacion", "Error: Código de respuesta no OK (" + responseCode + ")");
            }
        } catch (Exception e) {
            Log.e("Error", "Ocurrió un error al intentar obtener la puntuación: " + e.getMessage());
            result.append("Error: " + e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (Exception e) {
                Log.e("Error", "Error al cerrar los recursos: " + e.getMessage());
            }
            return result.toString();
        }
    }

    private void guardarPuntuacion(int puntuacion) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String idUsuario = user.getUid();

            // Obtener la puntuación máxima almacenada para este usuario
            db.collection("Puntuaciones")
                    .whereEqualTo("idUsuario", idUsuario)
                    .whereEqualTo("idJuego", idJuego) // Asegurarse de que el juego sea el correcto
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            // Si existe el documento para este usuario y juego
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);  // Obtener el primer documento (debería ser único)
                            int puntuacionMaxima = document.getLong("puntuacionMaxima").intValue();
                            String fechaPuntuacionMaxima = document.getString("fechaPuntuacionMaxima");

                            // Verificar si la puntuación actual es mayor que la máxima
                            int puntuacionMayor = Math.max(puntuacion, puntuacionMaxima);
                            String fechaPuntuacionActual = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                            // Actualizar el documento con la nueva puntuación
                            HashMap<String, Object> datosPuntuacion = new HashMap<>();
                            datosPuntuacion.put("puntuacionActual", (long) puntuacion);
                            datosPuntuacion.put("fechaPuntuacionActual", fechaPuntuacionActual);  // Fecha de la puntuación actual
                            datosPuntuacion.put("puntuacionMaxima", (long) puntuacionMayor);  // Guardar la puntuación máxima
                            datosPuntuacion.put("fechaPuntuacionMaxima", puntuacionMayor > puntuacionMaxima ? fechaPuntuacionActual : fechaPuntuacionMaxima);  // Fecha de la puntuación máxima

                            // Actualizar el documento en Firestore
                            db.collection("Puntuaciones")
                                    .document(document.getId())  // Obtener el ID del documento existente
                                    .update(datosPuntuacion)  // Actualizar el documento con los nuevos datos
                                    .addOnSuccessListener(aVoid -> {
                                        if (getActivity() != null) {
                                            Toast.makeText(getActivity(), "Puntuación actualizada exitosamente", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        if (getActivity() != null) {
                                            Toast.makeText(getActivity(), "Error al actualizar la puntuación: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            // Si no existe puntuación para este usuario y juego, guardarla como nueva
                            guardarPrimeraPuntuacion(idUsuario, puntuacion);
                        }
                    });
        }
    }

    private void guardarPrimeraPuntuacion(String idUsuario, int puntuacion) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String fechaActual = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // Guardar la puntuación como un nuevo documento en Firestore
        HashMap<String, Object> datosPuntuacion = new HashMap<>();
        datosPuntuacion.put("idUsuario", idUsuario);
        datosPuntuacion.put("idJuego", idJuego);  // El ID del juego cargado previamente
        datosPuntuacion.put("puntuacionActual", (long) puntuacion);
        datosPuntuacion.put("fechaPuntuacionActual", fechaActual);  // La fecha de la puntuación actual
        datosPuntuacion.put("puntuacionMaxima", (long) puntuacion);  // La primera puntuación es también la máxima
        datosPuntuacion.put("fechaPuntuacionMaxima", fechaActual);  // La primera puntuación también tiene la misma fecha

        db.collection("Puntuaciones")
                .add(datosPuntuacion)  // Crear nuevo documento
                .addOnSuccessListener(documentReference -> {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Puntuación guardada exitosamente", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Error al guardar puntuación: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


}
