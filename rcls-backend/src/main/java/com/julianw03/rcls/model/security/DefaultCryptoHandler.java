package com.julianw03.rcls.model.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

public class DefaultCryptoHandler extends AbstractCryptoHandler {
    static final int HASH_LENGTH = 32;

    @NoArgsConstructor
    @Getter
    public static class Params {
        static final int ITERATIONS  = 3;
        static final int MEM_LIMIT   = Short.MAX_VALUE;
        static final int PARALLELISM = 1;

        private int id = Argon2Parameters.ARGON2_id;
        private int version = Argon2Parameters.ARGON2_VERSION_13;
        private int iterations = ITERATIONS;
        private int memLimitKb = MEM_LIMIT;
        private int parallelism = PARALLELISM;
    }


    private final Params       params;

    public DefaultCryptoHandler(JsonNode configurationData) {
        super(configurationData);
        try {
            this.params = mapper.treeToValue(configurationData, Params.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DefaultCryptoHandler() {
        this(createDefaultConfiguration());
    }

    protected static JsonNode createDefaultConfiguration() {
        return new ObjectMapper().valueToTree(new Params());
    }


    @Override
    public Optional<String> encrypt(byte[] data, byte[] salt,  byte[] password) {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(params.getId())
                .withVersion(params.getVersion())
                .withIterations(params.getIterations())
                .withMemoryAsKB(params.getMemLimitKb())
                .withParallelism(params.getParallelism())
                .withSalt(salt);


        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());
        byte[] generatedKey = new byte[HASH_LENGTH];
        generator.generateBytes(password, generatedKey, 0, generatedKey.length);

        try {
            byte[] iv = new byte[12];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec secretKey = new SecretKeySpec(generatedKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] ciphertext = cipher.doFinal(data);

            byte[] encryptedData = new byte[iv.length + ciphertext.length];

            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);

            return Optional.ofNullable(Base64.getEncoder().encodeToString(encryptedData));
        } catch (Exception e) {

        }

        return Optional.empty();
    }

    @Override
    public Optional<String> decrypt(byte[] data, byte[] salt, byte[] password) {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(params.getId())
                .withVersion(params.getVersion())
                .withIterations(params.getIterations())
                .withMemoryAsKB(params.getMemLimitKb())
                .withParallelism(params.getParallelism())
                .withSalt(salt);


        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());
        byte[] generatedKey = new byte[HASH_LENGTH];
        generator.generateBytes(password, generatedKey, 0, generatedKey.length);

        try {
            // Extract the IV and ciphertext
            byte[] iv = new byte[12]; // 12-byte IV
            byte[] ciphertext = new byte[data.length - iv.length];
            System.arraycopy(data, 0, iv, 0, iv.length);
            System.arraycopy(data, iv.length, ciphertext, 0, ciphertext.length);

            // Create GCMParameterSpec with the IV
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec secretKey = new SecretKeySpec(generatedKey, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // Perform decryption
            byte[] decryptedData = cipher.doFinal(ciphertext);

            return Optional.of(new String(decryptedData, StandardCharsets.UTF_8));
        } catch (Exception e) {

        }
        return Optional.empty();
    }

    private static byte[] generateSaltBytes(int length) {
        if (length < 0) throw new IllegalArgumentException();
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[length];
        secureRandom.nextBytes(salt);

        return salt;
    }
}
