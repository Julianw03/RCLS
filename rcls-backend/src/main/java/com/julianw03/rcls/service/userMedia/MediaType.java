package com.julianw03.rcls.service.userMedia;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

enum MediaType {
    VIDEO(List.of("mp4", "webm")),
    IMAGE(List.of("png", "jpg", "jpeg", "gif")),
    AUDIO(List.of("mp3", "wav", "ogg"));

    private final List<String> allowedExtensions;

    MediaType(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public static Optional<MediaType> fromExtension(String extension) {
        return Arrays.stream(values())
                .filter(type -> type.getAllowedExtensions().contains(extension.toLowerCase()))
                .findFirst();
    }


    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }
}
