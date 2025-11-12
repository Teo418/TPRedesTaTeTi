import Excepciones.ComandoInvalidoException;
import Excepciones.FaltanArgumentosExcepcion;
import java.util.List;

public class Comando {
    private final ClientHandler client;
    private final Juego controladorJuego;

    public Comando(ClientHandler client) {
        this.client = client;
        this.controladorJuego = new Juego();
    }

    public void process(Mensaje mensaje) {
        String comandoRaiz = mensaje.getComandoRaiz();

        try {
            switch (comandoRaiz) {
                case "listar" -> listUsers();
                case "invitar" -> invite(mensaje);
                case "aceptar" -> accept(mensaje);
                case "jugar" -> play(mensaje);
                default -> throw new ComandoInvalidoException("Comando desconocido: " + comandoRaiz);
            }

        } catch (FaltanArgumentosExcepcion | ComandoInvalidoException e) {
            enviarMensajeSeguro("Error: " + e.getMessage());
        } catch (Exception e) {
            enviarMensajeSeguro("Error inesperado: " + e.getMessage());
        }
    }

    // 🔹 Manda un mensaje cifrado al cliente actual
    private void enviarMensajeSeguro(String contenido) {
        try {
            Mensaje respuesta = Mensaje.crearMensaje(contenido);
            client.getCanalSeguro().enviarMensaje(respuesta);
        } catch (Exception e) {
            System.out.println("[SERVER] Error al enviar mensaje a " + client.getName() + ": " + e.getMessage());
        }
    }

    private void listUsers() {
        try {
            StringBuilder sb = new StringBuilder("Usuarios conectados:");
            for (String n : Server.clients.keySet()) {
                if (!n.equals(client.getName())) sb.append("\n> ").append(n);
            }
            Mensaje respuesta = Mensaje.crearMensaje(sb.toString());
            client.getCanalSeguro().enviarMensaje(respuesta);
        } catch (Exception e) {
            System.out.println("[SERVER] Error en listar: " + e.getMessage());
        }
    }

    private void invite(Mensaje mensaje) throws FaltanArgumentosExcepcion {
        List<String> args = mensaje.getArgumentos();
        if (args.isEmpty()) throw new FaltanArgumentosExcepcion("Usá: invitar <usuario>");

        String oponente = args.getFirst().trim().toLowerCase();
        ClientHandler invited = Server.clients.get(oponente);

        if (invited == null) {
            enviarMensajeSeguro("Usuario no encontrado.");
            return;
        }

        if (oponente.equals(client.getName().toLowerCase())) {
            enviarMensajeSeguro("No te podés invitar a vos mismo.");
            return;
        }

        // Enviar invitación cifrada al oponente
        try {
            invited.getCanalSeguro().enviarMensaje(
                    Mensaje.crearMensaje(client.getName() + " quiere jugar. Escribí 'aceptar " + client.getName() + "' para empezar.")
            );
            enviarMensajeSeguro("Invitación enviada a " + oponente);
        } catch (Exception e) {
            enviarMensajeSeguro("Error al enviar la invitación: " + e.getMessage());
        }
    }

    private void accept(Mensaje mensaje) throws FaltanArgumentosExcepcion {
        List<String> args = mensaje.getArgumentos();
        if (args.isEmpty()) throw new FaltanArgumentosExcepcion("Usá: aceptar <usuario>");

        String inviterName = args.get(0);
        ClientHandler inviter = Server.clients.get(inviterName);

        if (inviter == null) {
            enviarMensajeSeguro("El usuario no está disponible.");
            return;
        }

        try {
            controladorJuego.startGame(inviter, client);
        } catch (Exception e) {
            enviarMensajeSeguro("Error al iniciar el juego: " + e.getMessage());
        }
    }

    private void play(Mensaje mensaje) throws Exception {
        List<String> args = mensaje.getArgumentos();
        if (args.size() < 2) throw new FaltanArgumentosExcepcion("Usá: jugar fila columna");

        try {
            controladorJuego.makeMove(client, args);
        } catch (Exception e) {
            enviarMensajeSeguro("Error al jugar: " + e.getMessage());
        }
    }
}
