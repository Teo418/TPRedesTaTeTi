import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("IP del servidor: ");
        String ip = teclado.readLine();

        System.out.print("Puerto: ");
        int puerto = Integer.parseInt(teclado.readLine());

        Socket socket = new Socket(ip, puerto);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        Thread lector = new Thread(() -> {
            String serverMsg;
            try {
                while ((serverMsg = in.readLine()) != null) {
                    System.out.println(serverMsg);
                }
            } catch (IOException e) {
                System.out.println("Conexión cerrada.");
            }
        });
        lector.start();

        String input;
        while ((input = teclado.readLine()) != null) {
            out.println(input);
        }
    }
}
