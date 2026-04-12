package dev.dhev.fileuploadapi.service;

import dev.dhev.fileuploadapi.api.FileMetadataResponse;
import dev.dhev.fileuploadapi.config.FileStorageProperties;
import dev.dhev.fileuploadapi.exception.FileMetadataNotFoundException;
import dev.dhev.fileuploadapi.exception.FileStorageException;
import dev.dhev.fileuploadapi.model.FileMetadataEntity;
import dev.dhev.fileuploadapi.repository.FileMetadataRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final Path uploadPath;
    private final long maxFileSizeBytes;
    private final List<String> allowedContentTypes;
    private final Clock clock;

    @Autowired
    public FileService(
            FileMetadataRepository fileMetadataRepository,
            FileStorageProperties fileStorageProperties
    ) {
        this(fileMetadataRepository, fileStorageProperties, Clock.systemUTC());
    }

    FileService(
            FileMetadataRepository fileMetadataRepository,
            FileStorageProperties fileStorageProperties,
            Clock clock
    ) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.uploadPath = Path.of(fileStorageProperties.uploadDir()).toAbsolutePath().normalize();
        this.maxFileSizeBytes = fileStorageProperties.maxFileSizeBytes();
        this.allowedContentTypes = fileStorageProperties.allowedContentTypes() == null
                ? List.of()
                : fileStorageProperties.allowedContentTypes().stream().toList();
        this.clock = clock;
    }

    public FileMetadataResponse upload(MultipartFile multipartFile) {
        validateFile(multipartFile);
        createUploadDirectory();

        String originalFilename = sanitizeFilename(multipartFile.getOriginalFilename());
        String storedFilename = buildStoredFilename(originalFilename);
        Path relativePath = Path.of(storedFilename);
        Path destination = uploadPath.resolve(relativePath).normalize();

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new FileStorageException("Failed to store file on disk", exception);
        }

        FileMetadataEntity document = new FileMetadataEntity();
        document.setId(UUID.randomUUID().toString());
        document.setOriginalFilename(originalFilename);
        document.setContentType(resolveContentType(multipartFile));
        document.setSize(multipartFile.getSize());
        document.setUploadedAt(Instant.now(clock));
        document.setStoredPath(relativePath.toString());

        try {
            FileMetadataEntity savedDocument = fileMetadataRepository.save(document);
            return FileMetadataResponse.from(savedDocument);
        } catch (RuntimeException exception) {
            deleteStoredFile(destination);
            throw exception;
        }
    }

    public List<FileMetadataResponse> listFiles() {
        return fileMetadataRepository.findAll()
                .stream()
                .map(FileMetadataResponse::from)
                .toList();
    }

    public FileMetadataResponse getMetadata(String id) {
        return FileMetadataResponse.from(getDocument(id));
    }

    public Resource loadAsResource(String id) {
        FileMetadataEntity document = getDocument(id);
        Path filePath = uploadPath.resolve(document.getStoredPath()).normalize();
        if (!filePath.startsWith(uploadPath)) {
            throw new FileStorageException("Stored file path is invalid for id: " + id);
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageException("Stored file is missing or unreadable for id: " + id);
            }
            return resource;
        } catch (IOException exception) {
            throw new FileStorageException("Failed to read stored file for id: " + id, exception);
        }
    }

    private FileMetadataEntity getDocument(String id) {
        return fileMetadataRepository.findById(id)
                .orElseThrow(() -> new FileMetadataNotFoundException(id));
    }

    private void validateFile(MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (multipartFile.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("File exceeds the configured size limit");
        }
        String contentType = resolveContentType(multipartFile);
        if (!allowedContentTypes.isEmpty() && !allowedContentTypes.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }
    }

    private void createUploadDirectory() {
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException exception) {
            throw new FileStorageException("Failed to create upload directory", exception);
        }
    }

    private String sanitizeFilename(String filename) {
        String cleaned = StringUtils.cleanPath(filename == null ? "" : filename);
        if (!StringUtils.hasText(cleaned)) {
            throw new IllegalArgumentException("Original filename is required");
        }
        if (cleaned.contains("..")) {
            throw new IllegalArgumentException("Filename contains an invalid path sequence");
        }
        return Path.of(cleaned).getFileName().toString();
    }

    private String buildStoredFilename(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String suffix = StringUtils.hasText(extension) ? "." + extension : "";
        return UUID.randomUUID() + suffix;
    }

    private String resolveContentType(MultipartFile multipartFile) {
        String contentType = multipartFile.getContentType();
        return StringUtils.hasText(contentType) ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private void deleteStoredFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
