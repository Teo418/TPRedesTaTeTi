import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

public class Encriptacion {
    private static final String RSA_ALGORITHM = "RSA";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private static final int RSA_KEY_SIZE = 2048;
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private KeyPair clavePublicaPrivada;
    private SecretKey aesKey;
    private SecretKey hmacKey;

    public Encriptacion() throws NoSuchAlgorithmException {
        generateRSAKeyPair();
    }

    private void generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyGen.initialize(RSA_KEY_SIZE);
        this.clavePublicaPrivada = keyGen.generateKeyPair();
    }

    public void generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGen.init(AES_KEY_SIZE);
        this.aesKey = keyGen.generateKey();

        // Generamos también una clave HMAC derivada
        generateHMACKey();
    }


    private void generateHMACKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(HMAC_ALGORITHM);
        keyGen.init(256);
        this.hmacKey = keyGen.generateKey();
    }

    /**
     * Establece la clave AES recibida del otro extremo
     */
    public void setAESKey(byte[] keyBytes) throws NoSuchAlgorithmException {
        this.aesKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, AES_ALGORITHM);
        generateHMACKey();
    }

    /**
     * Obtiene la clave pública RSA en formato byte[]
     */
    public byte[] getPublicKeyBytes() {
        return clavePublicaPrivada.getPublic().getEncoded();
    }

    /**
     * Obtiene los bytes de la clave AES
     */
    public byte[] getAESKeyBytes() {
        if (aesKey == null) {
            throw new IllegalStateException("AES key not generated yet");
        }
        return aesKey.getEncoded();
    }

    /**
     * Cifra datos con RSA usando la clave pública proporcionada
     */
    public byte[] encryptWithRSA(byte[] data, byte[] publicKeyBytes)
            throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /**
     * Descifra datos con RSA usando la clave privada propia
     */
    public byte[] decryptWithRSA(byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, clavePublicaPrivada.getPrivate());
        return cipher.doFinal(encryptedData);
    }

    /**
     * Cifra datos con AES-GCM (incluye autenticación)
     * Formato: [IV(12 bytes)][Datos cifrados][Tag de autenticación]
     */
    public byte[] encryptWithAES(byte[] plaintext) throws Exception {
        if (aesKey == null) {
            throw new IllegalStateException("AES key not set");
        }

        // Generamos un IV aleatorio
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // Configuramos el cifrado
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);

        // Ciframos
        byte[] ciphertext = cipher.doFinal(plaintext);

        // Concatenamos: IV + Ciphertext (que ya incluye el tag GCM)
        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

        return result;
    }

    /**
     * Descifra datos con AES-GCM
     */
    public byte[] decryptWithAES(byte[] encryptedData) throws Exception {
        if (aesKey == null) {
            throw new IllegalStateException("AES key not set");
        }

        // Extraemos el IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);

        // Extraemos el ciphertext
        byte[] ciphertext = new byte[encryptedData.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedData, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        // Desciframos
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

        return cipher.doFinal(ciphertext);
    }

    /**
     * Genera una firma HMAC de los datos
     */
    public byte[] generateHMAC(byte[] data) throws Exception {
        if (hmacKey == null) {
            throw new IllegalStateException("HMAC key not generated");
        }

        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(hmacKey);
        return mac.doFinal(data);
    }

    /**
     * Verifica la firma HMAC de los datos
     */
    public boolean verifyHMAC(byte[] data, byte[] hmac) throws Exception {
        byte[] calculatedHmac = generateHMAC(data);
        return MessageDigest.isEqual(calculatedHmac, hmac);
    }

    // genera firma encriptando con la privada (RSA)
    public byte[] signData(byte[] data) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(clavePublicaPrivada.getPrivate());
        signature.update(data);
        return signature.sign();
    }

    /**
     * Verifica una firma RSA con la clave pública proporcionada
     */
    public boolean verifySignature(byte[] data, byte[] signatureBytes, byte[] publicKeyBytes)
            throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(signatureBytes);
    }
}