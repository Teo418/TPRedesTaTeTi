import Excepciones.FaltanArgumentosExcepcion;
import Excepciones.MovimientoInvalidoException;

import java.util.Arrays;
import java.util.List;

public class Sesion {
    private final char[][] board = new char[3][3];
    private final ClientHandler playerX;
    private final ClientHandler playerO;
    private char currentTurn = 'X';

    public Sesion(ClientHandler p1, ClientHandler p2) {
        this.playerX = p1;
        this.playerO = p2;
        for (char[] row : board) Arrays.fill(row, ' ');

        enviarMensajeSeguro(p1, "Partida iniciada. Sos X.");
        enviarMensajeSeguro(p2, "Partida iniciada. Sos O.");
        sendBoard();
        enviarMensajeSeguro(playerX, "Tu turno. Usá: jugar fila columna");
    }

    // 🔹 Método auxiliar para enviar mensajes cifrados
    private void enviarMensajeSeguro(ClientHandler jugador, String contenido) {
        try {
            Mensaje msg = Mensaje.crearMensaje(contenido);
            jugador.getCanalSeguro().enviarMensaje(msg);
        } catch (Exception e) {
            System.out.println("[SERVER] Error al enviar mensaje a " + jugador.getName() + ": " + e.getMessage());
        }
    }

    private boolean checkWin(char s) {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == s && board[i][1] == s && board[i][2] == s) return true;
            if (board[0][i] == s && board[1][i] == s && board[2][i] == s) return true;
        }
        return (board[0][0] == s && board[1][1] == s && board[2][2] == s) ||
                (board[0][2] == s && board[1][1] == s && board[2][0] == s);
    }

    private boolean boardFull() {
        for (char[] row : board)
            for (char cell : row)
                if (cell == ' ') return false;
        return true;
    }

    public boolean checkBoard(ClientHandler player) {
        char symbol = (player == playerX) ? 'X' : 'O';
        if (checkWin(symbol)) {
            enviarMensajeSeguro(player, "¡Ganaste!");
            enviarMensajeSeguro(getOpponent(player), "Perdiste :(");
            return true;
        } else if (boardFull()) {
            enviarMensajeSeguro(playerX, "Empate.");
            enviarMensajeSeguro(playerO, "Empate.");
            return true;
        }
        return false;
    }

    private void sendBoard() {
        StringBuilder sb = new StringBuilder("\n");
        for (int i = 0; i < 3; i++) {
            sb.append(Arrays.toString(board[i])).append("\n");
        }

        String tablero = sb.toString();
        enviarMensajeSeguro(playerX, tablero);
        enviarMensajeSeguro(playerO, tablero);
    }

    public void playMove(ClientHandler player, List<String> argumentos) throws Exception {
        if (argumentos.size() < 3) {
            throw new FaltanArgumentosExcepcion("Usá: jugar fila columna");
        }

        int row = Integer.parseInt(argumentos.get(1));
        int col = Integer.parseInt(argumentos.get(2));

        char symbol = (player == playerX) ? 'X' : 'O';
        if (symbol != currentTurn) {
            enviarMensajeSeguro(player, "No es tu turno.");
            return;
        }

        if (row < 0 || row > 2 || col < 0 || col > 2 || board[row][col] != ' ') {
            throw new MovimientoInvalidoException("Movimiento inválido.");
        }

        board[row][col] = symbol;
        sendBoard();

        if (checkBoard(player)) {
            return; // terminó el juego
        }

        currentTurn = (currentTurn == 'X') ? 'O' : 'X';
        enviarMensajeSeguro(getOpponent(player), "Tu turno. Usá: jugar fila columna");
    }

    private ClientHandler getOpponent(ClientHandler player) {
        return (player == playerX) ? playerO : playerX;
    }
}
