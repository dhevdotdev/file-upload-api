package dev.dhev.fileuploadapi.controller;

import dev.dhev.fileuploadapi.api.FileMetadataResponse;
import dev.dhev.fileuploadapi.service.FileService;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final FileService fileService;

    public FileUploadController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileMetadataResponse> upload(@RequestParam("file") MultipartFile file) {
        FileMetadataResponse response = fileService.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FileMetadataResponse>> listFiles() {
        return ResponseEntity.ok(fileService.listFiles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileMetadataResponse> getMetadata(@PathVariable String id) {
        return ResponseEntity.ok(fileService.getMetadata(id));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        FileMetadataResponse metadata = fileService.getMetadata(id);
        Resource resource = fileService.loadAsResource(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.filename() + "\"")
                .contentType(MediaType.parseMediaType(metadata.type()))
                .contentLength(metadata.size())
                .body(resource);
    }
}
