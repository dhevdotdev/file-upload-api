package dev.dhev.fileuploadapi.repository;

import dev.dhev.fileuploadapi.model.FileMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMetadataRepository extends JpaRepository<FileMetadataEntity, String> {
}
