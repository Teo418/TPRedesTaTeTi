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
    public byte[] getDatosEntrantes() throws IOException {
        int length = dataIn.readInt();
        if (length <= 0 || length > 65536) {
            throw new IOException("Longitud de datos inválida: " + length);
        }

        byte[] data = new byte[length];
        dataIn.readFully(data);
        return data;
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
    public void confirmacion() throws IOException{
        enviarDatos("READY".getBytes("UTF-8"));
    }

    public void handshakeServidor() throws Exception {
        System.out.println("[CanalSeguro] Handshake servidor...");

        this.clavePublicaRemota = getDatosEntrantes();
        System.out.println("[CanalSeguro] ✓ Clave pública del cliente recibida");

        enviarDatos(crypto.getPublicKeyBytes());
        System.out.println("[CanalSeguro] ✓ Clave pública enviada al cliente");

        byte[] aesKeyCifrada = getDatosEntrantes();
        byte[] aesKey = desencriptarConPrivada(aesKeyCifrada);
        crypto.setAESKey(aesKey);
        System.out.println("[CanalSeguro] ✓ Clave AES establecida");

        confirmacion();
        handshakeCompleted = true;

        System.out.println("[CanalSeguro] ✓ Handshake servidor completado");
    }


    public void handshakeCliente() throws Exception {
        System.out.println("[CanalSeguro] Handshake cliente...");

        enviarDatos(crypto.getPublicKeyBytes());
        System.out.println("[CanalSeguro] ✓ Clave pública enviada");

        this.clavePublicaRemota = getDatosEntrantes();
        System.out.println("[CanalSeguro] ✓ Clave pública del servidor recibida");

        crypto.generateAESKey();
        byte[] aesKeyCifrada = crypto.encriptarConPublicaDelReceptor(crypto.getAESKeyBytes(), clavePublicaRemota);
        enviarDatos(aesKeyCifrada);
        System.out.println("[CanalSeguro] ✓ Clave AES enviada cifrada");

        byte[] confirm = getDatosEntrantes();
        if (!new String(confirm).equals("READY"))
            throw new SecurityException("No se recibió READY");

        handshakeCompleted = true;
        System.out.println("[CanalSeguro] ✓ Handshake cliente completado");
    }


    public void enviarMensaje(Mensaje mensaje) throws Exception {
        if (!this.handshakeCompleted) {
            throw new IllegalStateException("Error: no se realizó el handshake.");
        }

        // 1) Obtener contenido
        String contenido = mensaje.getContenidoMensaje();
        if (contenido == null || contenido.isEmpty()) {
            throw new IllegalArgumentException("No se puede enviar un mensaje vacío.");
        }

        // 2) Generar firma digital
        byte[] firmaBytes = crypto.firmar(contenido.getBytes("UTF-8"));

        // 3) Convertir firma a HEX (para poder concatenarla sin que se rompa el texto)
        String firmaHex = crypto.bytesToHex(firmaBytes);

        // 4) Armar paquete plano texto → "contenido|||firmaHex"
        String textoPlano = contenido + "|||" + firmaHex;
        byte[] textoPlanoBytes = textoPlano.getBytes("UTF-8");

        // 5) Cifrar con AES
        byte[] mensajeCifrado = crypto.encriptarConSimetrica(textoPlanoBytes);

        // 6) Enviar por el canal seguro binario
        enviarDatos(mensajeCifrado);
    }

    public Mensaje recibirMensaje() throws Exception {
        if (!this.handshakeCompleted) {
            throw new IllegalStateException("Error: no se realizó el handshake.");
        }

        // 1) Recibir paquete binario
        byte[] paqueteRecibido = getDatosEntrantes();
        if (paqueteRecibido == null) {
            return null; // cliente desconectado
        }

        // 2) Desencriptar con AES
        byte[] paqueteDesencriptado = crypto.desencriptarConSimetrica(paqueteRecibido);
        String textoPlano = new String(paqueteDesencriptado, "UTF-8");

        // 3) Separar contenido y firma
        int separador = textoPlano.lastIndexOf("|||");
        if (separador == -1) {
            throw new SecurityException("Mensaje recibido sin firma (falta '|||').");
        }

        String contenidoPlano = textoPlano.substring(0, separador).trim();
        String firmaHex = textoPlano.substring(separador + 3).trim();

        if (contenidoPlano.isEmpty()) {
            throw new SecurityException("Contenido vacío en el mensaje.");
        }
        if (firmaHex.isEmpty()) {
            throw new SecurityException("Firma vacía en el mensaje.");
        }

        // 4) Convertir firma desde HEX a bytes
        byte[] firma = crypto.hexToBytes(firmaHex);

        // 5) Verificar firma con la clave pública remota
        boolean firmaValida = crypto.verificarFirma(
                contenidoPlano.getBytes("UTF-8"),
                firma,
                clavePublicaRemota
        );

        if (!firmaValida) {
            throw new SecurityException("Firma inválida: mensaje adulterado o remitente falso.");
        }

        // 6) Crear mensaje VALIDADO (sin volver a firmar)
        return Mensaje.crearMensaje(contenidoPlano);
    }

}