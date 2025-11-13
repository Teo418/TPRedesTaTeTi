import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java Client <IP> <PUERTO>");
            return;
        }

        try {
            Socket socket = new Socket(args[0], Integer.parseInt(args[1]));
            CanalSeguro canalSeguro = new CanalSeguro(
                    socket.getInputStream(),
                    socket.getOutputStream()
            );

            // 1) Handshake
            canalSeguro.handshakeCliente();

            Scanner scanner = new Scanner(System.in);

            // 2) Esperar pedido de nombre
            Mensaje msg = canalSeguro.recibirMensaje();
            if (msg == null || !msg.getContenidoMensaje().equals("PEDIR_NOMBRE")) {
                throw new IllegalStateException("El servidor no pidió el nombre correctamente.");
            }

            // 3) Enviar nombre
            System.out.print("Ingrese su nombre: ");
            String nombre = scanner.nextLine();
            canalSeguro.enviarMensaje(Mensaje.crearMensaje(nombre));

            // 4) Esperar confirmación o rechazo
            Mensaje respuesta = canalSeguro.recibirMensaje();
            while (respuesta != null && respuesta.getContenidoMensaje().equals("NOMBRE_EN_USO")) {
                System.out.println("Ese nombre está en uso. Intente otro:");
                nombre = scanner.nextLine();
                canalSeguro.enviarMensaje(Mensaje.crearMensaje(nombre));
                respuesta = canalSeguro.recibirMensaje();
            }

            if (respuesta == null || !respuesta.getContenidoMensaje().equals("OK")) {
                throw new IllegalStateException("Error inesperado en autenticación.");
            }

            System.out.println("Conectado correctamente como: " + nombre);

            Thread listener = new Thread(() -> {
                try {
                    while (true) {
                        Mensaje recibido = canalSeguro.recibirMensaje();
                        if (recibido != null) {

                            String contenido = recibido.getContenidoMensaje();

                            if (contenido.startsWith("\n")) {
                                System.out.print(contenido);
                            } else {
                                System.out.println("[Servidor]: " + contenido);
                            }

                        } else {
                            System.out.println("Servidor desconectado.");
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[CLIENTE] Conexión cerrada: " + e.getMessage());
                }
            });
            listener.start();

            // 🔹 Hilo principal: leer y enviar mensajes al servidor
            while (true) {
                String texto = scanner.nextLine();
                if (texto.equalsIgnoreCase("salir")) break;
                canalSeguro.enviarMensaje(Mensaje.crearMensaje(texto));
            }

            socket.close();
            System.out.println("Cliente desconectado.");

        } catch (Exception e) {
            System.out.println("Error en cliente: " + e.getMessage());
        }
    }
}
