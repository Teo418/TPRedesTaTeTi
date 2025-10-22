public class Tateti {
    private char[][] tablero = new char[3][3];
    private Servidor.ManejadorCliente jugador1;
    private Servidor.ManejadorCliente jugador2;
    private Servidor.ManejadorCliente turno;

    public Tateti(Servidor.ManejadorCliente j1, Servidor.ManejadorCliente j2) {
        jugador1 = j1;
        jugador2 = j2;
        turno = jugador1;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                tablero[i][j] = ' ';
    }

    public void iniciar() {
        jugador1.enviarMensaje("Inicias la partida contra " + jugador2.nombre + ". Tú eres X.");
        jugador2.enviarMensaje("Inicias la partida contra " + jugador1.nombre + ". Tú eres O.");
        mostrarTablero();
        turno.enviarMensaje("Tu turno (formato: fila columna):");
    }

    public void movimiento(Servidor.ManejadorCliente jugador, String entrada) {
        if (jugador != turno) {
            jugador.enviarMensaje("No es tu turno.");
            return;
        }
        try {
            String[] partes = entrada.split(" ");
            int fila = Integer.parseInt(partes[0]) - 1;
            int col = Integer.parseInt(partes[1]) - 1;

            if (fila < 0 || fila > 2 || col < 0 || col > 2 || tablero[fila][col] != ' ') {
                jugador.enviarMensaje("Movimiento inválido.");
                return;
            }

            tablero[fila][col] = (jugador == jugador1) ? 'X' : 'O';
            mostrarTablero();

            if (ganador()) {
                jugador1.enviarMensaje("¡Partida terminada! Ganador: " + jugador.nombre);
                jugador2.enviarMensaje("¡Partida terminada! Ganador: " + jugador.nombre);
                terminar();
                return;
            }
            if (empate()) {
                jugador1.enviarMensaje("Empate.");
                jugador2.enviarMensaje("Empate.");
                terminar();
                return;
            }

            turno = (turno == jugador1) ? jugador2 : jugador1;
            turno.enviarMensaje("Tu turno (fila columna):");

        } catch (Exception e) {
            jugador.enviarMensaje("Formato inválido. Usa: fila columna");
        }
    }

    private void mostrarTablero() {
        StringBuilder sb = new StringBuilder();
        for (char[] fila : tablero) {
            sb.append("|");
            for (char c : fila) sb.append(c).append("|");
            sb.append("\n");
        }
        jugador1.enviarMensaje(sb.toString());
        jugador2.enviarMensaje(sb.toString());
    }

    private boolean ganador() {
        for (int i = 0; i < 3; i++) {
            if (tablero[i][0] != ' ' && tablero[i][0] == tablero[i][1] && tablero[i][1] == tablero[i][2])
                return true;
            if (tablero[0][i] != ' ' && tablero[0][i] == tablero[1][i] && tablero[1][i] == tablero[2][i])
                return true;
        }
        return (tablero[0][0] != ' ' && tablero[0][0] == tablero[1][1] && tablero[1][1] == tablero[2][2]) ||
                (tablero[0][2] != ' ' && tablero[0][2] == tablero[1][1] && tablero[1][1] == tablero[2][0]);
    }

    private boolean empate() {
        for (char[] fila : tablero)
            for (char c : fila)
                if (c == ' ') return false;
        return true;
    }

    public void terminar() {
        jugador1.juego = null;
        jugador2.juego = null;
        jugador1.enPartida = false;
        jugador2.enPartida = false;
    }

}
