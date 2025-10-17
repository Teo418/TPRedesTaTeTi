import java.io.*;
import java.net.*;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String name;


    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Bienvenido! Ingrese su nombre:");
            name = in.readLine();

            while (Server.clients.containsKey(name)) {
                out.println("Este nombre ya está en uso, ingrese otro:");
                name = in.readLine();
            }

            Server.clients.put(name, this);
            Comando commandProcessor = new Comando(this);

            String input;
            while ((input = in.readLine()) != null) {
                commandProcessor.process(input);
            }

        } catch (IOException e) {
            System.out.println(name + " se desconectó.");
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            Server.clients.remove(name);
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public BufferedReader getIn() {
        return in;
    }

    public void setIn(BufferedReader in) {
        this.in = in;
    }

    public PrintWriter getOut() {
        return out;
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

  
}
