import java.io.*;
import java.net.*;
import java.util.*;

public class Servidor {
    private static final int PUERTO = 5000;
    private static Set<ManejadorCliente> clientes = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado en el puerto " + PUERTO);

            while (true) {
                Socket socket = servidor.accept();
                ManejadorCliente cliente = new ManejadorCliente(socket);
                clientes.add(cliente);
                new Thread(cliente).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(String mensaje, ManejadorCliente remitente) {
        synchronized (clientes) {
            for (ManejadorCliente cliente : clientes) {
                if (cliente != remitente) {
                    cliente.enviarMensaje(mensaje);
                }
            }
        }
    }

    public static String obtenerListaUsuarios() {
        StringBuilder lista = new StringBuilder("Usuarios conectados:\n");
        synchronized (clientes) {
            for (ManejadorCliente cliente : clientes) {
                lista.append("- ").append(cliente.nombre);
                if (cliente.enPartida) lista.append(" (en partida)");
                lista.append("\n");
            }
        }
        return lista.toString();
    }

    static class ManejadorCliente implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        String nombre;
        private ManejadorCliente oponente;
        private ManejadorCliente invitador;
        boolean enPartida = false;
        Tateti juego;

        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        public void enviarMensaje(String mensaje) {
            out.println(mensaje);
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("Ingresa tu nombre:");
                nombre = in.readLine();

                broadcast(">> " + nombre + " se ha unido al chat.", this);
                enviarMensaje(Servidor.obtenerListaUsuarios());

                String mensaje;
                while ((mensaje = in.readLine()) != null) {

                    if (mensaje.equalsIgnoreCase("/usuarios")) {
                        enviarMensaje(Servidor.obtenerListaUsuarios());

                    } else if (mensaje.equalsIgnoreCase("/jugar ")) {
                        String oponenteNombre = mensaje.substring(7);
                        invitarJugador(oponenteNombre);

                    } else if (mensaje.equalsIgnoreCase("/aceptar")) {
                        aceptarInvitacion();

                    } else if (mensaje.equalsIgnoreCase("/rechazar")) {
                        rechazarInvitacion();

                    } else if (mensaje.equalsIgnoreCase("/rendirse")) {
                        rendirse();

                    } else if (mensaje.equalsIgnoreCase("/salir")) {
                        enviarMensaje("Desconectándote del servidor...");
                        break;

                    } else if (juego != null) {
                        juego.movimiento(this, mensaje);

                    } else {
                        broadcast(nombre + ": " + mensaje, this);
                    }
                }
            } 
            catch (IOException e) {
                System.out.println(nombre + " se ha desconectado.");
                desconectar(); // Debes llamar aquí manualmente
        }

        }

        private void invitarJugador(String nombreOponente) {
            if (enPartida) {
                enviarMensaje("Ya estás en una partida y no puedes invitar a otros jugadores.");
                return;
            }
            synchronized (clientes) {
                for (ManejadorCliente c : clientes) {
                    if (c.nombre.equals(nombreOponente) && c != this) {
                        if (c.enPartida) {
                            enviarMensaje(nombreOponente + " está en una partida y no puede aceptar invitaciones.");
                            return;
                        }
                        c.invitador = this;
                        enviarMensaje("Invitación enviada a " + c.nombre + ". Esperando respuesta...");
                        c.enviarMensaje(nombre + " te ha invitado a jugar. Escribe /aceptar o /rechazar.");
                        return;
                    }
                }
            }
            enviarMensaje("Jugador no encontrado.");
        }

        private void aceptarInvitacion() {
            if (invitador == null) {
                enviarMensaje("No tienes ninguna invitación pendiente.");
                return;
            }
            if (enPartida || invitador.enPartida) {
                enviarMensaje("No puedes aceptar la invitación porque uno de los dos está en partida.");
                invitador.enviarMensaje("La invitación a " + nombre + " no se pudo completar.");
                invitador = null;
                return;
            }
            this.oponente = invitador;
            invitador.oponente = this;
            this.enPartida = true;
            invitador.enPartida = true;
            this.juego = new Tateti(this, invitador);
            invitador.juego = this.juego;
            this.juego.iniciar();
            invitador.invitador = null;
            invitador = null;
        }

        private void rechazarInvitacion() {
            if (invitador == null) {
                enviarMensaje("No tienes ninguna invitación pendiente.");
                return;
            }
            invitador.enviarMensaje(nombre + " ha rechazado tu invitación.");
            enviarMensaje("Has rechazado la invitación de " + invitador.nombre + ".");
            invitador = null;
        }

        private void rendirse() {
            if (juego != null) {
                if (oponente != null) {
                    oponente.enviarMensaje(nombre + " se ha rendido. ¡Ganaste!");
                }
                enviarMensaje("Te has rendido. Fin de la partida.");
                juego.terminar();
            } else {
                enviarMensaje("No estás en ninguna partida.");
            }
        }

        private void desconectar() {
            try { socket.close(); } catch (IOException ignored) {}
            clientes.remove(this);
            broadcast(">> " + nombre + " ha salido del chat.", this);

            if (oponente != null && oponente.juego != null) {
                oponente.enviarMensaje(nombre + " se ha desconectado. Fin de la partida.");
                oponente.juego.terminar();
            }
        }
    }
}
