package dev.dhev.fileuploadapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.dhev.fileuploadapi.api.FileMetadataResponse;
import dev.dhev.fileuploadapi.config.FileStorageProperties;
import dev.dhev.fileuploadapi.exception.FileStorageException;
import dev.dhev.fileuploadapi.model.FileMetadataEntity;
import dev.dhev.fileuploadapi.repository.FileMetadataRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class FileServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreFileAndPersistMetadata() throws Exception {
        FileMetadataRepository repository = mock(FileMetadataRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-04-12T08:30:00Z"), ZoneOffset.UTC);
        FileService fileService = new FileService(
                repository,
                new FileStorageProperties(
                        tempDir.toString(),
                        1024 * 1024,
                        java.util.Set.of(MediaType.IMAGE_PNG_VALUE)
                ),
                clock
        );

        when(repository.save(any(FileMetadataEntity.class))).thenAnswer(invocation -> {
            FileMetadataEntity entity = invocation.getArgument(0);
            return entity;
        });

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test-img.png",
                MediaType.IMAGE_PNG_VALUE,
                "png-content".getBytes(StandardCharsets.UTF_8)
        );

        FileMetadataResponse response = fileService.upload(multipartFile);

        assertThat(response.id()).hasSize(36);
        assertThat(response.filename()).isEqualTo("test-img.png");
        assertThat(response.type()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        assertThat(response.uploadedAt()).isEqualTo(Instant.parse("2026-04-12T08:30:00Z"));
        assertThat(Files.list(tempDir)).hasSize(1);
    }

    @Test
    void shouldRejectFileLargerThanConfiguredLimit() {
        FileMetadataRepository repository = mock(FileMetadataRepository.class);
        FileService fileService = new FileService(
                repository,
                new FileStorageProperties(
                        tempDir.toString(),
                        3,
                        java.util.Set.of(MediaType.IMAGE_PNG_VALUE)
                ),
                Clock.systemUTC()
        );

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test-img.png",
                MediaType.IMAGE_PNG_VALUE,
                "large".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> fileService.upload(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size limit");
    }

    @Test
    void shouldLoadFileAsResource() throws Exception {
        FileMetadataRepository repository = mock(FileMetadataRepository.class);
        Path storedFile = Files.writeString(tempDir.resolve("stored.png"), "data", StandardCharsets.UTF_8);

        FileMetadataEntity entity = new FileMetadataEntity();
        entity.setId("file-123");
        entity.setOriginalFilename("test-img.png");
        entity.setContentType(MediaType.IMAGE_PNG_VALUE);
        entity.setSize(4L);
        entity.setUploadedAt(Instant.parse("2026-04-12T08:30:00Z"));
        entity.setStoredPath(storedFile.getFileName().toString());

        when(repository.findById("file-123")).thenReturn(Optional.of(entity));

        FileService fileService = new FileService(
                repository,
                new FileStorageProperties(
                        tempDir.toString(),
                        1024 * 1024,
                        java.util.Set.of(MediaType.IMAGE_PNG_VALUE)
                ),
                Clock.systemUTC()
        );

        Resource resource = fileService.loadAsResource("file-123");

        assertThat(resource.exists()).isTrue();
        assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("data");
    }

    @Test
    void shouldListFiles() {
        FileMetadataRepository repository = mock(FileMetadataRepository.class);

        FileMetadataEntity entity = new FileMetadataEntity();
        entity.setId("file-123");
        entity.setOriginalFilename("test-img.png");
        entity.setContentType(MediaType.IMAGE_PNG_VALUE);
        entity.setSize(4L);
        entity.setUploadedAt(Instant.parse("2026-04-12T08:30:00Z"));

        when(repository.findAll()).thenReturn(List.of(entity));

        FileService fileService = new FileService(
                repository,
                new FileStorageProperties(
                        tempDir.toString(),
                        1024 * 1024,
                        java.util.Set.of(MediaType.IMAGE_PNG_VALUE)
                ),
                Clock.systemUTC()
        );

        List<FileMetadataResponse> files = fileService.listFiles();

        assertThat(files).hasSize(1);
        assertThat(files.getFirst().id()).isEqualTo("file-123");
    }

    @Test
    void shouldFailWhenStoredFileIsMissing() {
        FileMetadataRepository repository = mock(FileMetadataRepository.class);

        FileMetadataEntity entity = new FileMetadataEntity();
        entity.setId("file-123");
        entity.setStoredPath("missing.png");

        when(repository.findById("file-123")).thenReturn(Optional.of(entity));

        FileService fileService = new FileService(
                repository,
                new FileStorageProperties(
                        tempDir.toString(),
                        1024 * 1024,
                        java.util.Set.of(MediaType.IMAGE_PNG_VALUE)
                ),
                Clock.systemUTC()
        );

        assertThatThrownBy(() -> fileService.loadAsResource("file-123"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("missing or unreadable");
    }

    @Test
    void shouldRejectUnsupportedContentType() {
        FileMetadataRepository repository = mock(FileMetadataRepository.class);
        FileService fileService = new FileService(
                repository,
                new FileStorageProperties(
                        tempDir.toString(),
                        1024 * 1024,
                        java.util.Set.of(MediaType.IMAGE_PNG_VALUE)
                ),
                Clock.systemUTC()
        );

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> fileService.upload(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported content type");
    }
}
