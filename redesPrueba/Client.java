import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class Client {
    private static BufferedReader teclado;//para leer lo que el usuario escribe
    private static String ip;
    private static int puerto;
    private static Socket socket;//conexión con el servidor
    private static CanalSeguro canalSeguro;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("IP_DEL_SERVER", 5000);

            CanalSeguro canalSeguro = new CanalSeguro(
                    socket.getInputStream(),
                    socket.getOutputStream()
            );

            // 1) Handshake
            canalSeguro.handshakeCliente();

            Scanner scanner = new Scanner(System.in);

            // 2) Esperar orden del servidor
            Mensaje msg = canalSeguro.recibirMensaje();
            if (!msg.getContenidoMensaje().equals("PEDIR_NOMBRE"))
                throw new IllegalStateException("El servidor no pidió el nombre correctamente.");

            // 3) Enviar nombre
            System.out.print("Ingrese su nombre: ");
            String nombre = scanner.nextLine();
            canalSeguro.enviarMensaje(Mensaje.crearMensaje(nombre));

            // 4) Esperar confirmación o rechazo
            Mensaje respuesta = canalSeguro.recibirMensaje();

            while (respuesta.getContenidoMensaje().equals("NOMBRE_EN_USO")) {
                System.out.println("Ese nombre está en uso. Intente otro:");
                nombre = scanner.nextLine();
                canalSeguro.enviarMensaje(Mensaje.crearMensaje(nombre));
                respuesta = canalSeguro.recibirMensaje();
            }

            if (!respuesta.getContenidoMensaje().equals("OK"))
                throw new IllegalStateException("Error inesperado en autenticación.");

            System.out.println("Conectado correctamente como: " + nombre);

            // A partir de acá ya podés enviar y recibir mensajes del chat normalmente
            // Ejemplo:
            while (true) {
                String texto = scanner.nextLine();
                canalSeguro.enviarMensaje(Mensaje.crearMensaje(texto));
            }

        } catch (Exception e) {
            System.out.println("Error en cliente: " + e.getMessage());
        }
    }
}

/*public void run() {
        String serverMsg;
        try {
            while ((serverMsg = mensajesEntrada.readLine()) != null) {
                System.out.println(serverMsg);
            }
        } catch (IOException e) {
            System.out.println("Conexión cerrada.");
        }
    }
}
Thread lector = new Thread(new LectorRunnable(mensajesEntrada));
lector.start();
*/
