package com.julianw03.rcls;

import com.julianw03.rcls.model.security.DefaultCryptoHandler;
import com.julianw03.rcls.model.security.EncryptedData;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

public class Test {

    public static void main(String[] args) throws Exception {
        String examplePassword = "password";
        String exampleData = "{\"email\":\"samplemail@gmail.com\",\"loginName\":\"someloginname\",\"password\":\"testpassword\"}";

        byte[] password = examplePassword.getBytes(StandardCharsets.UTF_8);
        byte[] salt = generateSaltBytes(64);
        byte[] data = exampleData.getBytes(StandardCharsets.UTF_8);

        DefaultCryptoHandler handler = new DefaultCryptoHandler();
        Optional<String> encrypted = handler.encrypt(data, salt, password);
        if (encrypted.isEmpty()) return;
        String b64Encrypted = encrypted.get();
        byte[] bytes = Base64.getDecoder().decode(b64Encrypted);

        EncryptedData encryptedData = new EncryptedData(salt, bytes);

        System.out.println("Salt: " + encryptedData.getBase64Salt());
        System.out.println("Data: " + encryptedData.getBase64Data());
        Optional<String> decrypted = handler.decrypt(encryptedData.getData(), encryptedData.getSalt(), password);
        if (decrypted.isEmpty()) {
            System.out.println("Failed");
            return;
        }
        System.out.println(decrypted.get());
    }

    private static byte[] generateSaltBytes(int length) throws NoSuchAlgorithmException {
        if (length < 0) throw new IllegalArgumentException();
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[length];
        secureRandom.nextBytes(salt);

        return salt;
    }

    // AES-GCM encryption method
    public static String encryptGCM(String data, byte[] keyBytes) throws Exception {
        // Generate a random IV (12 bytes recommended for GCM)
        byte[] iv = new byte[12];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        // Create GCMParameterSpec with a 128-bit authentication tag length
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

        // Initialize AES-GCM cipher for encryption
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

        // Perform encryption
        byte[] ciphertext = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Combine the IV and ciphertext (and authentication tag)
        byte[] encryptedData = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, encryptedData, 0, iv.length);
        System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);

        // Return as Base64 string (easy to store or transfer)
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    // AES-GCM decryption method
    public static String decryptGCM(String encryptedDataBase64, byte[] keyBytes) throws Exception {
        // Decode the Base64-encoded encrypted data
        byte[] encryptedData = Base64.getDecoder().decode(encryptedDataBase64);

        // Extract the IV and ciphertext
        byte[] iv = new byte[12]; // 12-byte IV
        byte[] ciphertext = new byte[encryptedData.length - iv.length];
        System.arraycopy(encryptedData, 0, iv, 0, iv.length);
        System.arraycopy(encryptedData, iv.length, ciphertext, 0, ciphertext.length);

        // Create GCMParameterSpec with the IV
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

        // Initialize AES-GCM cipher for decryption
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

        // Perform decryption
        byte[] decryptedData = cipher.doFinal(ciphertext);

        // Convert decrypted bytes to a string
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

}
