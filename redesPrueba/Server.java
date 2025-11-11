import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    static Map<String, ClientHandler> clients = Collections.synchronizedMap(new HashMap<>());
    //me permite "bloquear el hasmap", me sirve para modificar la info del hash con un cliente, es decir que no haya dos cliente modificando la info al mismo tiempo sino se puede corromper el codigo(chat gpt)
    public static void main(String[] args) throws IOException {
        if(args.length < 1){
            System.out.println("No se ingreso un puerto (java Server <puerto>)");
            return;
        }
        try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]))){
            System.out.println("Servidor iniciado en puerto " + args[0] + "...");
            while (true) {
                Socket socket = serverSocket.accept();// esto es un metodo bloqueante que me ayuda a
                // esperar a que se conecta un cliente por eso el while(true),
                // porque sino podria hacer solo una conexion
                ClientHandler handler = new ClientHandler(socket);// por cada cliente hago un
                // client handler y inicio el thread
                handler.start();// inicio el thread de cada cliente y le paso el socket
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }
}
