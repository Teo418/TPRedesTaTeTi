import java.util.Arrays;
import java.util.List;

public class Mensaje {
    private String comandoRaiz;
    private List<String> argumentos;
    private byte[] firma; // solo se usa al enviar

    public Mensaje(String comandoRaiz, List<String> argumentos) {
        this.comandoRaiz = comandoRaiz;
        this.argumentos = argumentos;
    }

    public String getComandoRaiz() {
        return comandoRaiz;
    }

    public List<String> getArgumentos() {
        return argumentos;
    }

    public byte[] getFirma() {
        return firma;
    }

    public void setFirma(byte[] firma) {
        this.firma = firma;
    }

    // Construye el texto "comando arg1 arg2 ..."
    public String getContenidoMensaje() {
        StringBuilder sb = new StringBuilder(comandoRaiz);
        if (argumentos != null && !argumentos.isEmpty()) {
            for (String arg : argumentos) {
                sb.append(" ").append(arg);
            }
        }
        return sb.toString();
    }

    // NO reconstruye ni concatena firma: solo firma el contenido
    public void firmar(Encriptacion encriptacion) throws Exception {
        this.firma = encriptacion.firmar(getContenidoMensaje().getBytes("UTF-8"));
    }

    public boolean verificarFirma(byte[] clavePublicaEmisor, Encriptacion encriptacion) throws Exception {
        if (this.firma == null || this.firma.length == 0) {
            throw new IllegalStateException("El mensaje no tiene firma.");
        }
        return encriptacion.verificarFirma(
                getContenidoMensaje().getBytes("UTF-8"),
                this.firma,
                clavePublicaEmisor
        );
    }

    // Reconstruye un mensaje desde el texto plano "comando arg1 arg2"
    public static Mensaje crearMensaje(String input) {
        String[] parts = input.trim().split(" ");
        if (parts.length == 0) return new Mensaje("", Arrays.asList());

        String comandoRaiz = parts[0];
        List<String> argumentos = Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length));

        return new Mensaje(comandoRaiz, argumentos);
    }
}
