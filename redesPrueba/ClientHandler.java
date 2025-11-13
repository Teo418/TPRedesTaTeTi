import java.io.*;
import java.net.*;

public class ClientHandler extends Thread {
    private final Socket socket;
    private CanalSeguro canalSeguro;
    private String nombreCliente;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.nombreCliente = null; // Inicialmente null
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

            String nombrePropuesto = nombreMsg.getContenidoMensaje().trim();

            // 3) Confirmamos que el nombre no esté repetido
            synchronized (Server.clients) {
                while (Server.clients.containsKey(nombrePropuesto)) {
                    canalSeguro.enviarMensaje(Mensaje.crearMensaje("NOMBRE_EN_USO"));
                    nombreMsg = canalSeguro.recibirMensaje();
                    nombrePropuesto = nombreMsg.getContenidoMensaje().trim();
                }

                // 4) Asignar el nombre ANTES de registrar en el mapa
                this.nombreCliente = nombrePropuesto;

                // 5) Registrar cliente en el servidor
                Server.clients.put(nombreCliente, this);
            }

            System.out.println("[SERVER] Cliente identificado como: " + nombreCliente);

            // 6) Avisar al cliente que todo está OK
            canalSeguro.enviarMensaje(Mensaje.crearMensaje("OK"));

            // 7) Procesar mensajes que envía el cliente
            Comando commandProcessor = new Comando(this, Server.controladorJuegoGlobal);
            Mensaje mensaje;

            while ((mensaje = canalSeguro.recibirMensaje()) != null) {
                commandProcessor.process(mensaje);
            }

        } catch (Exception e) {
            System.out.println("[SERVER] Cliente " +
                    (nombreCliente != null ? nombreCliente : "desconocido") +
                    " se desconectó. Motivo: " + e.getMessage());

        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            if (nombreCliente != null) {
                Server.clients.remove(nombreCliente);
                System.out.println("[SERVER] Cliente removido: " + nombreCliente);
            }
        }
    }

    public String getNombreCliente() {
        return this.nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setCanalSeguro(CanalSeguro canalSeguro) {
        this.canalSeguro = canalSeguro;
    }

    public CanalSeguro getCanalSeguro() {
        return canalSeguro;
    }

    @Override
    public String toString() {
        return nombreCliente != null ? nombreCliente : "Usuario sin nombre";
    }
}