import Excepciones.ComandoInvalidoException;
import Excepciones.FaltanArgumentosExcepcion;

public class Comando {
    private ClientHandler client;
    private Juego controladorJuego;

    public Comando(ClientHandler client) {
        this.client = client;
        this.controladorJuego = new Juego();
    }

    public void process(String input) {
        try {
            if (input.equals("listar")) {
                listUsers();
            } else if (input.startsWith("invitar ")) {
                invite(input);
            } else if (input.startsWith("aceptar ")) {
                accept(input);
            } else if (input.startsWith("jugar ")) {
                play(input);
            } else {
                throw new ComandoInvalidoException("Comando desconocido: " + input);
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

    private void invite(String input) throws FaltanArgumentosExcepcion {
        String target = parseTarget(input);
        ClientHandler invited = encontrarCliente(target);

        if (!chequearInvitacion(target, invited)) return;

        enviarInvitacion(invited, target);
    }
    private String parseTarget(String input) throws FaltanArgumentosExcepcion {
        String[] parts = input.split(" ");
        if (parts.length < 2)
            throw new FaltanArgumentosExcepcion("Usá: invitar <usuario>");
        return parts[1].trim().toLowerCase();
    }

    private ClientHandler encontrarCliente(String target) {
        return Server.clients.get(target);
    }

    private boolean chequearInvitacion(String target, ClientHandler invited) {
        if (invited == null) {
            client.getOut().println("Usuario no encontrado.");
            return false;
        }
        if (target.equals(client.getName().toLowerCase())) {
            client.getOut().println("No te podés invitar a vos mismo.");
            return false;
        }
        return true;
    }

    private void enviarInvitacion(ClientHandler invited, String target) {
        invited.getOut().println(client.getName() + " quiere jugar. Escribí 'aceptar " + client.getName() + "' para empezar.");
        client.getOut().println("Invitación enviada a " + target);
    }


    private void accept(String input) throws FaltanArgumentosExcepcion {
        String[] parts = input.split(" ");
        if (parts.length < 2) throw new FaltanArgumentosExcepcion("Usá: aceptar <usuario>");

        String inviterName = parts[1];
        ClientHandler inviter = Server.clients.get(inviterName);
        if (inviter == null) throw new FaltanArgumentosExcepcion("El usuario no está disponible.");

        controladorJuego.startGame(inviter, client);
    }

    private void play(String input) throws Exception {
        controladorJuego.makeMove(client, input);
    }
}
