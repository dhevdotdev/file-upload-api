package dev.dhev.fileuploadapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.dhev.fileuploadapi.api.FileMetadataResponse;
import dev.dhev.fileuploadapi.service.FileService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FileUploadController.class)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileService fileService;

    @Test
    void shouldUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-img.png",
                MediaType.IMAGE_PNG_VALUE,
                "png-content".getBytes(StandardCharsets.UTF_8)
        );

        when(fileService.upload(any())).thenReturn(new FileMetadataResponse(
                "file-123",
                "test-img.png",
                MediaType.IMAGE_PNG_VALUE,
                file.getSize(),
                Instant.parse("2026-04-12T08:30:00Z")
        ));

        mockMvc.perform(multipart("/api/files").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("file-123"))
                .andExpect(jsonPath("$.filename").value("test-img.png"))
                .andExpect(jsonPath("$.type").value(MediaType.IMAGE_PNG_VALUE));
    }

    @Test
    void shouldListFiles() throws Exception {
        when(fileService.listFiles()).thenReturn(List.of(
                new FileMetadataResponse(
                        "file-123",
                        "test-img.png",
                        MediaType.IMAGE_PNG_VALUE,
                        10L,
                        Instant.parse("2026-04-12T08:30:00Z")
                )
        ));

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("file-123"));
    }

    @Test
    void shouldDownloadStoredFile() throws Exception {
        when(fileService.getMetadata("file-123")).thenReturn(new FileMetadataResponse(
                "file-123",
                "test-img.png",
                MediaType.IMAGE_PNG_VALUE,
                4L,
                Instant.parse("2026-04-12T08:30:00Z")
        ));
        when(fileService.loadAsResource("file-123")).thenReturn(new ByteArrayResource("data".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/files/file-123/content"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"test-img.png\""))
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
                .andExpect(content().bytes("data".getBytes(StandardCharsets.UTF_8)));
    }
}
