const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');

const app = express();

app.use(cors()); // Habilita CORS para evitar bloqueos
app.use(bodyParser.json()); // Permite leer JSON en requests

let latestScore = 0; // Variable para almacenar la puntuación más reciente

// ✅ Ruta GET para obtener el score
app.get('/score', (req, res) => {
    res.json({ score: latestScore }); // Devuelve la puntuación almacenada
});

// ✅ Ruta POST para recibir el score
app.post('/score', (req, res) => {
    console.log('Datos recibidos:', req.body); // Verifica que la puntuación sea la correcta
    if (!req.body || typeof req.body.score !== 'number') {
        return res.status(400).json({ error: 'Formato JSON incorrecto' });
    }
    latestScore = req.body.score; // Actualiza la puntuación almacenada
    res.json({ message: 'Puntuación recibida', data: req.body });
});

// Inicia el servidor en todas las interfaces de red
app.listen(3000, '0.0.0.0', () => {
    console.log('Servidor corriendo en el puerto 3000');
});