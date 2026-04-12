package dev.dhev.fileuploadapi.exception;

public class FileMetadataNotFoundException extends RuntimeException {

    public FileMetadataNotFoundException(String id) {
        super("File metadata not found for id: " + id);
    }
}
