package com.autotrader.autotraderbackend.service.storage;

import com.autotrader.autotraderbackend.config.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceIntegrationTest {

    private LocalStorageService storageService;
    private StorageProperties properties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setLocation(tempDir.toString());
        properties.setBaseUrl("http://localhost:8080/api/files");
        storageService = new LocalStorageService(properties);
        storageService.init();
    }

    @Test
    void testStoreAndLoadFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "test-file",
            "test-file.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "Hello, World!".getBytes()
        );
        String url = storageService.store(file, "test-folder/test-file.txt");
        assertEquals("http://localhost:8080/api/files/test-folder/test-file.txt", url);
        Path storedFile = tempDir.resolve("test-folder/test-file.txt");
        assertTrue(Files.exists(storedFile));
        String content = new String(Files.readAllBytes(storedFile));
        assertEquals("Hello, World!", content);
        var resource = storageService.loadAsResource("test-folder/test-file.txt");
        assertTrue(resource.exists());
    }

    @Test
    void testDelete() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "test-file",
            "test-file.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "Hello, World!".getBytes()
        );
        storageService.store(file, "test-file.txt");
        Path storedFile = tempDir.resolve("test-file.txt");
        assertTrue(Files.exists(storedFile));
        boolean deleted = storageService.delete("test-file.txt");
        assertTrue(deleted);
        assertFalse(Files.exists(storedFile));
    }

    @Test
    void testGetSignedUrl() {
        String url = storageService.getSignedUrl("test-file.txt", 3600);
        assertEquals("http://localhost:8080/api/files/test-file.txt", url);
    }
}
