import Excepciones.ComandoInvalidoException;
import Excepciones.FaltanArgumentosExcepcion;
import java.util.List;

public class Comando {
    private ClientHandler client;
    private Juego controladorJuego;

    public Comando(ClientHandler client) {
        this.client = client;
        this.controladorJuego = new Juego();
    }

    public void process(Mensaje Mensaje) {
        String comandoRaiz = Mensaje.getComandoRaiz();

        // Futuro: Verificar la firma aquí antes de procesar

        try {
            if (comandoRaiz.equals("listar")) {
                listUsers();
            } else if (comandoRaiz.equals("invitar")) {
                invite(Mensaje); // Pasamos el objeto completo
            } else if (comandoRaiz.equals("aceptar")) {
                accept(Mensaje); // Pasamos el objeto completo
            } else if (comandoRaiz.equals("jugar")) {
                play(Mensaje); // Pasamos el objeto completo
            } else {
                throw new ComandoInvalidoException("Comando desconocido: " + comandoRaiz);
            }
        } catch (Exception e) {
            client.getOut().println("Error: " + e.getMessage());
        }
    }

    private void listUsers() {
        client.getOut().println("Usuarios conectados:");
        for (String n : Server.clients.keySet()) {
            if (!n.equals(client.getName())) client.getOut().println("> " + n);
        }
    }

    // *** MODIFICACIÓN A invite: Recibe mensaje ***
    private void invite(Mensaje mensaje) throws FaltanArgumentosExcepcion {
        List<String> args = mensaje.getArgumentos();
        if (args.isEmpty()) throw new FaltanArgumentosExcepcion("Usá: invitar <usuario>");

        String oponente = args.getFirst().trim().toLowerCase(); // El primer argumento es el oponente
        ClientHandler invited = encontrarCliente(oponente);

        if (!chequearInvitacion(oponente, invited)) return;

        enviarInvitacion(invited, oponente);
    }

    private ClientHandler encontrarCliente(String oponente) {
        return Server.clients.get(oponente);
    }

    private boolean chequearInvitacion(String oponente, ClientHandler invited) {
        if (invited == null) {
            client.getOut().println("Usuario no encontrado.");
            return false;
        }
        if (oponente.equals(client.getName().toLowerCase())) {
            client.getOut().println("No te podés invitar a vos mismo.");
            return false;
        }
        return true;
    }

    private void enviarInvitacion(ClientHandler invited, String oponente) {
        invited.getOut().println(client.getName() + " quiere jugar. Escribí 'aceptar " + client.getName() + "' para empezar.");
        client.getOut().println("Invitación enviada a " + oponente);
    }

    // *** MODIFICACIÓN A accept: Recibe mensaje ***
    private void accept(Mensaje mensaje) throws FaltanArgumentosExcepcion {
        List<String> args = mensaje.getArgumentos();
        if (args.isEmpty()) throw new FaltanArgumentosExcepcion("Usá: aceptar <usuario>");

        String inviterName = args.getFirst();
        ClientHandler inviter = Server.clients.get(inviterName);
        if (inviter == null) throw new FaltanArgumentosExcepcion("El usuario no está disponible.");

        controladorJuego.startGame(inviter, client);
    }

    // *** MODIFICACIÓN A play: Recibe Mensaje ***
    private void play(Mensaje mensaje) throws Exception {
        // Enviar el comando completo original ("jugar fila columna") a makeMove
        // o, mejor aún, pasar los argumentos de forma estructurada.

        // Opción 1: Reconstruir el input String para no cambiar Juego.java (más fácil por ahora)
        // String input = "jugar " + Mensaje.getArgumentos().get(0) + " " + Mensaje.getArgumentos().get(1);
        // controladorJuego.makeMove(client, input);

        // Opción 2: Pasar los argumentos y modificar Juego.makeMove (MEJOR DISEÑO)
        List<String> args = mensaje.getArgumentos();
        if (args.size() < 2) throw new FaltanArgumentosExcepcion("Usá: jugar fila columna");

        int row = Integer.parseInt(args.get(0));
        int col = Integer.parseInt(args.get(1));

        // Asumimos que vas a modificar Juego.makeMove para recibir argumentos estructurados
        controladorJuego.makeMove(client, mensaje.getArgumentos());
    }
}