import Excepciones.MovimientoInvalidoException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Juego {
    private Map<ClientHandler, Sesion> sessions = new HashMap<>();

    public synchronized void startGame(ClientHandler player1, ClientHandler player2) {
        // Verificar que ninguno esté ya en una partida
        if (sessions.containsKey(player1)) {
            throw new IllegalStateException(player1.getNombreCliente() + " ya está en una partida.");
        }
        if (sessions.containsKey(player2)) {
            throw new IllegalStateException(player2.getNombreCliente() + " ya está en una partida.");
        }

        Sesion session = new Sesion(player1, player2);
        sessions.put(player1, session);
        sessions.put(player2, session);

        System.out.println("[JUEGO] Partida iniciada entre " +
                player1.getNombreCliente() + " y " + player2.getNombreCliente());
    }

    public synchronized void makeMove(ClientHandler player, List<String> argumentos) throws Exception {
        Sesion session = sessions.get(player);
        if (session == null) {
            throw new MovimientoInvalidoException("No estás en una partida.");
        }

        boolean juegoTerminado = session.playMove(player, argumentos);

        if (juegoTerminado) {
            endGame(session.getPlayerX(), session.getPlayerO());
        }
    }

    public synchronized void endGame(ClientHandler player1, ClientHandler player2) {
        sessions.remove(player1);
        sessions.remove(player2);
        System.out.println("[JUEGO] Partida finalizada entre " +
                player1.getNombreCliente() + " y " + player2.getNombreCliente());
    }
}