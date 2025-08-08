import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    static Map<String, ClientHandler> clients = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Servidor iniciado en puerto 5000...");

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket);
            handler.start();
        }
    }
}
