import java.io.*;
import java.net.*;
import java.util.Arrays;

public class ClientHandler extends Thread {
    private final Socket socket;
    private CanalSeguro canalSeguro;
    private String name;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Crear canal seguro con el socket del cliente
            canalSeguro = new CanalSeguro(
                    socket.getInputStream(),
                    socket.getOutputStream()
            );

            // Handshake del lado del servidor (intercambio de claves)
            canalSeguro.handshakeServidor();

            // 1) El servidor solicita el nombre
            canalSeguro.enviarMensaje(Mensaje.crearMensaje("PEDIR_NOMBRE"));

            // 2) Cliente envía el nombre cifrado
            Mensaje nombreMsg = canalSeguro.recibirMensaje();
            if (nombreMsg == null)
                throw new IOException("No se recibió nombre del cliente.");

            this.name = nombreMsg.getContenidoMensaje();

            // 3) Confirmamos que el nombre no esté repetido
            while (Server.clients.containsKey(name)) {
                canalSeguro.enviarMensaje(Mensaje.crearMensaje("NOMBRE_EN_USO"));
                nombreMsg = canalSeguro.recibirMensaje();
                this.name = nombreMsg.getContenidoMensaje();
            }

            // 4) Registrar cliente en el servidor
            Server.clients.put(name, this);
            System.out.println("[SERVER] Cliente identificado como: " + name);

            // 5) Avisar al cliente que todo está OK
            canalSeguro.enviarMensaje(Mensaje.crearMensaje("OK"));

            // 6) Procesar mensajes que envía el cliente
            Comando commandProcessor = new Comando(this);
            Mensaje mensaje;

            while ((mensaje = canalSeguro.recibirMensaje()) != null) {
                commandProcessor.process(mensaje);
            }

        } catch (Exception e) {
            System.out.println("[SERVER] Cliente " + name + " se desconectó. Motivo: " + e.getMessage());

        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            Server.clients.remove(name);
            System.out.println("[SERVER] Cliente removido: " + name);
        }
    }

    public CanalSeguro getCanalSeguro() {
        return canalSeguro;
    }
}