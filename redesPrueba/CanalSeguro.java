import java.io.*;

public class CanalSeguro {
    private final BufferedReader inputReader;
    private final PrintWriter outputWriter;
    private final Encriptacion crypto;
    private byte[] clavePublicaRemota;
    private boolean handshakeCompleted;
    public CanalSeguro(InputStream socketInput, OutputStream socketOutput) throws Exception {
        this.inputReader = new BufferedReader(new InputStreamReader(socketInput));
        this.outputWriter = new PrintWriter(socketOutput, true);
        this.crypto = new Encriptacion();
        this.handshakeCompleted = false;
    }
    public String getClavePublica(){
        try{
            return inputReader.readLine();
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
        return null;
    }
    public void enviarClavePublica(){
        byte[] miPublica = crypto.getPublicKeyBytes();
        outputWriter.println("PUBKEY:" + miPublica.toString());
    }
    public String getClaveAsimetrica(){
        try {
            return inputReader.readLine();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
    public byte[] desencriptarClaveAsimetrica(byte[] claveAsimetricaEncriptada){
        try {
            return crypto.decryptWithRSA(claveAsimetricaEncriptada);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
    public void confirmacion(){
        outputWriter.println("READY");
    }
    public void handshakeServidor() throws Exception {
        System.out.println("[CanalSeguro] Iniciando handshake del servidor...");

        String lineaCliente = getClavePublica();

        if (lineaCliente == null || !lineaCliente.startsWith("PUBKEY:")) {
            throw new SecurityException("Esperaba PUBKEY del cliente o es nula");
        }
        clavePublicaRemota = lineaCliente.getBytes("UTF-8");
        System.out.println("[CanalSeguro] ✓ Clave pública del cliente recibida");

        enviarClavePublica();
        System.out.println("[CanalSeguro] ✓ Clave pública enviada");

        String lineaAES = getClaveAsimetrica();
        if (lineaAES == null || !lineaAES.startsWith("AESKEY:")) {
            throw new SecurityException("Esperaba AESKEY");
        }
        byte[] aesKeyCifrada = lineaAES.getBytes();

        byte[] aesKey = desencriptarClaveAsimetrica(aesKeyCifrada);
        crypto.setAESKey(aesKey);
        System.out.println("[CanalSeguro] ✓ Clave AES establecida");
        confirmacion();
        handshakeCompleted = true;
        System.out.println("[CanalSeguro] ✓ Handshake del servidor completado");
    }
}
