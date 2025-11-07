import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;

public class Client {
    private static BufferedReader teclado;//para leer lo que el usuario escribe
    private static String ip;
    private static int puerto;
    private static Socket socket;//conexión con el servidor
    private static CanalSeguro canalSeguro;

    public static void main(String[] args) throws IOException {
        teclado = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("IP del servidor: ");
        ip = teclado.readLine();
        System.out.print("Puerto: ");
        puerto = Integer.parseInt(teclado.readLine());//para poder pasar a ints
        socket = new Socket(ip, puerto);
        try {
            canalSeguro = new CanalSeguro(socket.getInputStream(), socket.getOutputStream());
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        }
        try {
            canalSeguro.handshakeCliente();
        } catch (Exception e) {
            System.out.println(e.getMessage());;
        }
        Thread lector = new Thread(() -> {
            try {
                Mensaje mensajeRecibido;
                while ((mensajeRecibido = canalSeguro.recibirMensaje())  != null) {
                    System.out.println("Mensaje entrante: " + mensajeRecibido.toString());
                }
            } catch (Exception e) {
                System.out.println(e.getMessage() + "<<<>>> Conexión cerrada");
            }
        });
        lector.start();

        String input;
        while ((input= teclado.readLine())  != null) {
            Mensaje mensajeAEnviar = Mensaje.crearMensaje(input);
            try {
                canalSeguro.enviarMensaje(mensajeAEnviar);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
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
