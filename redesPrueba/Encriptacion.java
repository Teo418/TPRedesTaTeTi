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

    private static final int RSA_KEY_SIZE = 2048;
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private KeyPair clavePublicaPrivada;
    private SecretKey aesKey;

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
    }

    public void setAESKey(byte[] keyBytes) throws NoSuchAlgorithmException {
        this.aesKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, AES_ALGORITHM);
    }

    public byte[] getPublicKeyBytes() {
        return clavePublicaPrivada.getPublic().getEncoded();
    }

    public byte[] getAESKeyBytes() {
        if (aesKey == null) {
            throw new IllegalStateException("AES key not generated yet");
        }
        return aesKey.getEncoded();
    }

    public byte[] encriptarConPublicaDelReceptor(byte[] data, byte[] publicKeyBytes)
            throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    public byte[] encriptarConPrivada(byte[] data, byte[] privateKeyBytes)
        throws Exception {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(privateKeyBytes);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return cipher.doFinal(data);
    }

    public byte[] desencriptarConPrivada(byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, clavePublicaPrivada.getPrivate());
        return cipher.doFinal(encryptedData);
    }

    public byte[] encriptarConSimetrica(byte[] plaintext) throws Exception {
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
        byte[] ciphertext = cipher.doFinal(plaintext);
        // Concatenamos: IV + Ciphertext (que ya incluye el tag GCM)
        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

        return result;
    }

    public byte[] desencriptarConSimetrica(byte[] encryptedData) throws Exception {
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

    public byte[] firmar(byte[] data) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(clavePublicaPrivada.getPrivate());
        signature.update(data);
        return signature.sign();
    }

    public boolean verificarFirma(byte[] data, byte[] signatureBytes, byte[] publicKeyBytes)
            throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(signatureBytes);
    }
    public byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}