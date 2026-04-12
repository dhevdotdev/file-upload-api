package dev.dhev.fileuploadapi.api;

import dev.dhev.fileuploadapi.model.FileMetadataEntity;
import java.time.Instant;

public record FileMetadataResponse(
        String id,
        String filename,
        String type,
        long size,
        Instant uploadedAt
) {

    public static FileMetadataResponse from(FileMetadataEntity document) {
        return new FileMetadataResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSize(),
                document.getUploadedAt()
        );
    }
}
