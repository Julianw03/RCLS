package com.julianw03.rcls.service.modules.standalone.userMedia;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/rcls/media/v1")
public class UserMediaController {

    private final UserMediaService userMediaService;

    public UserMediaController(
            @Autowired UserMediaService userMediaService
    ) {
        this.userMediaService = userMediaService;
    }

    @DeleteMapping("/background")
    public ResponseEntity<Void> resetBackground() {
        try {
            userMediaService.resetBackground();
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/background/type")
    public ResponseEntity<MediaType> loadCurrentBackgroundType() {
        UserMediaConfig videoResource;
        try {
            videoResource = userMediaService.loadConfig();
        } catch (IllegalArgumentException | IOException e) {
            return ResponseEntity.of(
                            ProblemDetail.forStatusAndDetail(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Your configuration seems to be corrupted. Please upload a new background.")
                    )
                    .build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.of(Optional.ofNullable(videoResource).map(UserMediaConfig::getMediaType));
    }

    @GetMapping("/background")
    public ResponseEntity<Resource> loadCurrentBackground() {
        UserMediaRessource videoResource;
        try {
            videoResource = userMediaService.loadCurrentBackground();
        } catch (IllegalArgumentException | IOException e) {
            return ResponseEntity.of(
                            ProblemDetail.forStatusAndDetail(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Your configuration seems to be corrupted. Please upload a new background.")
                    )
                    .build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }


        if (videoResource == null) {
            return ResponseEntity.of(
                    ProblemDetail.forStatusAndDetail(
                            HttpStatus.NOT_FOUND,
                            "No background has been set. Please upload a new background."
                    )
            ).build();
        }

        final Resource resource = videoResource.getResource();
        final String contentType = videoResource.getContentType();

        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(resource);
        } catch (Exception e) {
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/background")
    public ResponseEntity<Void> uploadNewBackground(
            @RequestParam("file") List<MultipartFile> files
    ) throws UnsupportedFileTypeException, IOException {
        for (MultipartFile file : files) {
            log.info("Uploading file {}", file.getOriginalFilename());
        }
        userMediaService.storeFile(files.getFirst());
        return ResponseEntity.noContent().build();
    }
}
