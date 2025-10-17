import Excepciones.MovimientoInvalidoException;

import java.util.HashMap;
import java.util.Map;

public class Juego {
    private Map<ClientHandler, Sesion> sessions = new HashMap<>();

    public void startGame(ClientHandler player1, ClientHandler player2) {
        Sesion session = new Sesion(player1, player2);
        sessions.put(player1, session);
        sessions.put(player2, session);
    }

    public void makeMove(ClientHandler player, String input) throws Exception {
        Sesion session = sessions.get(player);
        if (session == null) throw new MovimientoInvalidoException("No estás en una partida.");
        session.playMove(player, input);
    }
}
