package dev.dhev.fileuploadapi.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.storage")
public record FileStorageProperties(
        @NotBlank String uploadDir,
        @Positive long maxFileSizeBytes,
        Set<String> allowedContentTypes
) {
}
