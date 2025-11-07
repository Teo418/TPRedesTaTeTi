import java.io.*;
import java.security.NoSuchAlgorithmException;

public class CanalSeguro {
    private final DataInputStream dataIn;
    private final DataOutputStream dataOut;
    private final Encriptacion crypto;
    private byte[] clavePublicaRemota;
    private boolean handshakeCompleted;
    public CanalSeguro(InputStream socketInput, OutputStream socketOutput) throws NoSuchAlgorithmException{
        this.dataIn = new DataInputStream(socketInput);
        this.dataOut = new DataOutputStream(socketOutput);
        this.crypto = new Encriptacion();
        this.handshakeCompleted = false;
    }
    public void enviarDatos(byte[] data) throws IOException{
        if (data == null || data.length == 0)
            throw new IllegalArgumentException("No se pueden enviar datos nulos o vacíos.");
        try {
            this.dataOut.writeInt(data.length);
            this.dataOut.write(data);
            this.dataOut.flush();
        } catch (IOException e) {
            throw new IOException("Error de entrada/salida de datos");
        }
    }
    public byte[] getDatosEntrantes(){
        try {
            int length = this.dataIn.readInt();
            byte[] data = new byte[length];
            this.dataIn.readFully(data);
            return data;
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
    public byte[] encriptarConSimetrica(byte[] bytesParaDesencriptar){
        try {
            return this.crypto.encriptarConSimetrica(bytesParaDesencriptar);
        } catch (Exception e) {
            System.out.println(e.getMessage());;
        }
        return null;
    }
    public byte[] desencriptarConSimetrica(byte[] bytesParaDesencriptar){
        try {
            return this.crypto.desencriptarConSimetrica(bytesParaDesencriptar);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
    public byte[] desencriptarConPrivada(byte[] bytesParaDesencriptar){
        try {
            return this.crypto.desencriptarConPrivada(bytesParaDesencriptar);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
    public void confirmacion(){
        try {
            this.dataOut.write("READY".getBytes());
        } catch (IOException e) {
            System.out.println(e.getMessage());;
        }
    }

    public void handshakeServidor() throws Exception {
        System.out.println("[CanalSeguro] Handshake servidor...");

        this.clavePublicaRemota = getDatosEntrantes();
        if (clavePublicaRemota == null || clavePublicaRemota.length == 0)
            throw new SecurityException("Clave pública del cliente inválida.");
        System.out.println("[CanalSeguro] ✓ Clave pública del cliente recibida");
        byte[] miPublica = crypto.getPublicKeyBytes();
        if (miPublica == null || miPublica.length == 0)
            throw new IllegalStateException("Clave pública propia no está inicializada.");
        enviarDatos(crypto.getPublicKeyBytes());
        System.out.println("[CanalSeguro] ✓ Clave pública enviada al cliente");

        byte[] aesKeyCifrada = getDatosEntrantes();
        if (aesKeyCifrada == null || aesKeyCifrada.length == 0)
            throw new SecurityException("Clave AES cifrada inválida.");
        byte[] aesKey = desencriptarConPrivada(aesKeyCifrada);
        if (aesKey == null || aesKey.length == 0)
            throw new SecurityException("Fallo al descifrar clave AES.");
        crypto.setAESKey(aesKey);
        System.out.println("[CanalSeguro] ✓ Clave AES establecida");
        confirmacion();
        handshakeCompleted = true;
        System.out.println("[CanalSeguro] ✓ Handshake servidor completado");
    }

    public void handshakeCliente() throws Exception {
        System.out.println("[CanalSeguro] Handshake cliente...");

        byte[] miPublica = crypto.getPublicKeyBytes();
        if (miPublica == null || miPublica.length == 0)
            throw new IllegalStateException("Clave pública propia no está inicializada.");
        enviarDatos(miPublica);
        System.out.println("[CanalSeguro] ✓ Clave pública enviada");

        this.clavePublicaRemota = getDatosEntrantes();
        if (clavePublicaRemota == null || clavePublicaRemota.length == 0)
            throw new SecurityException("Clave pública del servidor inválida.");
        System.out.println("[CanalSeguro] ✓ Clave pública del servidor recibida");

        crypto.generateAESKey();
        byte[] aesKey = crypto.getAESKeyBytes();
        if (aesKey == null || aesKey.length == 0)
            throw new IllegalStateException("Clave AES no generada correctamente.");

        byte[] aesKeyCifrada = crypto.encriptarConPublicaDelReceptor(aesKey, clavePublicaRemota);
        enviarDatos(aesKeyCifrada);
        System.out.println("[CanalSeguro] ✓ Clave AES enviada cifrada");

        byte[] confirm = getDatosEntrantes();
        if (!new String(confirm).equals("READY"))
            throw new SecurityException("No se recibió confirmación READY.");

        handshakeCompleted = true;
        System.out.println("[CanalSeguro] ✓ Handshake cliente completado");
    }

    public void enviarMensaje(Mensaje mensaje) throws Exception{
        if (!this.handshakeCompleted){
            throw new IllegalStateException("Error, no se realizo el handshake");
        }
        mensaje.firmar(this.crypto);
        byte[] firma = mensaje.getFirma();
        if (firma == null || firma.length == 0) throw new IllegalStateException("Firma no generada.");

        String contenido = mensaje.getContenidoMensaje();
        byte[] contenidoBytes = contenido.getBytes("UTF-8");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(firma.length);
        dos.write(firma);
        dos.write(contenidoBytes);
        dos.flush();
        byte[] paquetePlano = baos.toByteArray();

        byte[] mensajeEncriptado = encriptarConSimetrica(mensaje.getBytes());

        enviarDatos(mensajeEncriptado);
    }
    public Mensaje recibirMensaje() throws Exception{
        if (!this.handshakeCompleted){
            throw new IllegalStateException("Error, no se realizo el handshake");
        }
        byte[] paqueteRecibido = getDatosEntrantes();
        byte[] paqueteDesencriptado = crypto.desencriptarConSimetrica(paqueteRecibido);
        String textoPlano = new String(paqueteDesencriptado, "UTF-8");
        int separador = textoPlano.lastIndexOf("|||");
        if (separador == -1) {
            throw new SecurityException("Mensaje no tiene firma (falta '|||').");
        }
        String contenidoPlano = textoPlano.substring(0, separador).trim();
        String firmaHex = textoPlano.substring(separador + 3).trim(); // lo que está después de |||

        if (contenidoPlano.isEmpty() || firmaHex.isEmpty()) {
            throw new SecurityException("Contenido o firma vacíos.");
        }
        byte[] firma = crypto.hexToBytes(firmaHex);
        if (!crypto.verificarFirma(contenidoPlano.getBytes("UTF-8"), firma, clavePublicaRemota)) {
            throw new SecurityException("Firma inválida. El mensaje fue alterado o el remitente no es auténtico.");
        }
        Mensaje mensaje = Mensaje.crearMensaje(contenidoPlano);
        mensaje.firmar(this.crypto);
        return mensaje;
    }
}