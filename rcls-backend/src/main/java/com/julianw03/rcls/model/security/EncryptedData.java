package com.julianw03.rcls.model.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.util.Base64;

@Getter
@JsonDeserialize(using = EncryptedDataDeserializer.class)
public class EncryptedData {
    @JsonIgnore
    private final byte[] salt;
    @JsonIgnore
    private final byte[] data;

    private final String base64Salt;
    private final String base64Data;

    public EncryptedData(
            byte[] salt,
            byte[] data
    ) {
        if (salt == null || data == null) throw new IllegalArgumentException();
        this.salt = salt;
        this.data = data;

        this.base64Salt = Base64.getEncoder().encodeToString(salt);
        this.base64Data = Base64.getEncoder().encodeToString(data);
    }

    public EncryptedData(
            String base64Salt,
            String base64Data
    ) {
        if (base64Salt == null || base64Data == null) throw new IllegalArgumentException();
        this.base64Salt = base64Salt;
        this.base64Data = base64Data;

        this.salt = Base64.getDecoder().decode(base64Salt);
        this.data = Base64.getDecoder().decode(base64Data);
    }
}
