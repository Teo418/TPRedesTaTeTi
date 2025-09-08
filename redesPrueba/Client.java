import java.io.*;
import java.net.*;

import java.io.*;
import java.net.*;

public class Client {
    private static BufferedReader teclado;//para leer lo que el usuario escribe
    private static String ip;
    private static int puerto;
    private static Socket socket;//conexión con el servidor
    private static BufferedReader mensajesEntrada;//para leer mensajes del servidor
    private static PrintWriter mensajeSalida;//para enviar mensajes al servidor

    public static void main(String[] args) throws IOException {
        teclado = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("IP del servidor: ");
        ip = teclado.readLine();
        System.out.print("Puerto: ");
        puerto = Integer.parseInt(teclado.readLine());//para poder pasar a ints
        socket = new Socket(ip, puerto);
        mensajesEntrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));//por aca puedo recibir los mensajes del server
        mensajeSalida = new PrintWriter(socket.getOutputStream(), true);// con esto los muestro
        // auto flush me permite que los mensajes se envien en el momento y borra la "cache"
        Thread lector = new Thread(() -> {
            String serverMsg;
            try {
                serverMsg= mensajesEntrada.readLine();
                while (serverMsg  != null) {
                    System.out.println(serverMsg);
                }
            } catch (IOException e) {
                System.out.println("Conexión cerrada.");
            }
        });
        lector.start();
        String input= teclado.readLine();
        while (input  != null) {
            mensajeSalida.println(input);
        }
    }}

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
