import java.util.Arrays;
import java.util.List;

public class Mensaje {
    private String comandoRaiz; // "listar", "invitar", "jugar"
    private List<String> argumentos;

    public Mensaje(String comandoRaiz, List<String> argumentos) {
        this.comandoRaiz = comandoRaiz;
        this.argumentos = argumentos;
    }

    public String getComandoRaiz() {
        return comandoRaiz;
    }

    public void setComandoRaiz(String comandoRaiz) {
        this.comandoRaiz = comandoRaiz;
    }

    public List<String> getArgumentos() {
        return argumentos;
    }

    public void setArgumentos(List<String> argumentos) {
        this.argumentos = argumentos;
    }
    public static Mensaje crearMensaje(String input) {
        String[] parts = input.trim().split(" ");
        if (parts.length == 0) return new Mensaje("", Arrays.asList());

        String comandoRaiz = parts[0];
        // Los argumentos son el resto de las partes (desde el índice 1)
        List<String> argumentos = Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length));

        return new Mensaje(comandoRaiz, argumentos);
    }
    // Para enviar el objeto serializado como un String (comando + args ||| firma)
    @Override
    public String toString() {
        String comandoConArgs = comandoRaiz;
        if (argumentos != null) {
            for (String arg : argumentos) {
                comandoConArgs += " " + arg;
            }
        }
        return comandoConArgs + "|||";
    }
}
