import java.io.*;
import java.net.*;

public class Cliente {
    public static void main(String[] args) {
        String servidor = "localhost";
        int puerto = 5000;

        try (Socket socket = new Socket(servidor, puerto)) {
            BufferedReader entradaServidor = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

            // Hilo para recibir mensajes
            new Thread(() -> {
                String msg;
                try {
                    while ((msg = entradaServidor.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    System.out.println("Conexión cerrada.");
                }
            }).start();

            // Enviar mensajes
            String texto;
            while ((texto = teclado.readLine()) != null) {
                salida.println(texto);
                if (texto.equalsIgnoreCase("/salir")) break;
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
