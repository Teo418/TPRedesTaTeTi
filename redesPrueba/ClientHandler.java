import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String name;
    private ClientHandler opponent;
    private char[][] board = null;
    private char symbol = ' ';

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    private void sendBoard() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (int i = 0; i < 3; i++) {
            sb.append(Arrays.toString(board[i])).append("\n");
        }
        out.println(sb.toString());
    }
    private boolean checkWin(char s) {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == s && board[i][1] == s && board[i][2] == s) return true;//filas columnas
            if (board[0][i] == s && board[1][i] == s && board[2][i] == s) return true;
        }
        if (board[0][0] == s && board[1][1] == s && board[2][2] == s) return true;// diagonales
        if (board[0][2] == s && board[1][1] == s && board[2][0] == s) return true;
        return false;
    }
    private boolean boardFull() {
        for (char[] row : board) {
            for (char cell : row)
                if (cell == ' ')
                    return false;
        }
        return true;
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Bienvenido! Ingrese su nombre:");
            name = in.readLine();
            Server.clients.put(name, this);
            out.println("Registrado como " + name);
            out.println("listar: Muestra usuarios conectados, invitar: invita jugador");
            String input;
            while ((input = in.readLine()) != null) {
                if (input.equals("listar")) {
                    out.println("Usuarios conectados:");
                    for (String n : Server.clients.keySet()) {
                        if (!n.equals(name))
                            out.println("> " + n);
                    }
                } else if (input.startsWith("invitar ")) {
                    String target = input.split(" ")[1];
                    ClientHandler invited = Server.clients.get(target);// busca dentro del hash los clientes del server con key target que introducio el usuario
                    if (invited != null) {
                        invited.out.println(name + " quiere jugar tateti contigo. Escribe 'aceptar " + name + "' para comenzar.");
                        out.println("Invitación enviada a " + target);
                    } else {
                        out.println("Usuario no encontrado.");
                    }
                } else if (input.startsWith("aceptar ")) {
                    String inviterName = input.split(" ")[1];
                    ClientHandler inviter = Server.clients.get(inviterName);
                    if (inviter != null) {
                        this.opponent = inviter;
                        inviter.opponent = this;

                        this.symbol = 'O';
                        inviter.symbol = 'X';

                        board = new char[3][3];
                        for (char[] row : board)
                            Arrays.fill(row, ' ');

                        inviter.board = board;

                        inviter.out.println("Partida iniciada. Vos sos X.");
                        this.out.println("Partida iniciada. Vos sos O.");

                        inviter.sendBoard();
                        inviter.out.println("Tu turno. Usá: jugar [fila] [columna]");
                    }
                } else if (input.startsWith("jugar ")) {
                    if (board == null || opponent == null) {
                        out.println("No estás en una partida.");
                        continue;
                    }

                    String[] parts = input.split(" ");
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);

                    if (row < 0 || row > 2 || col < 0 || col > 2 || board[row][col] != ' ') {
                        System.out.println("Hiciste un movimiento invalido");
                        continue;
                    }                 
                    
                    else board[row][col] = symbol;
                    sendBoard();
                    opponent.sendBoard();

                    if (checkWin(symbol)) {
                        out.println("¡Ganaste!");
                        opponent.out.println("Perdiste :(");
                        board = null;
                        opponent.board = null;
                    } else if (boardFull()) {
                        out.println("Empate.");
                        opponent.out.println("Empate.");
                        board = null;
                        opponent.board = null;
                    } else {
                        opponent.out.println("Tu turno. Usá: jugar fila columna");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(name + " se desconectó.");
        } 
        
        finally {
            try {
                socket.close();
                
            } catch (IOException e) {
                    
            }
            Server.clients.remove(name);
        }
    }
}
