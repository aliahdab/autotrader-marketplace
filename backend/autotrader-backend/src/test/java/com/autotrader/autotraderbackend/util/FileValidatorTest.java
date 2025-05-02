package com.autotrader.autotraderbackend.util;

import com.autotrader.autotraderbackend.exception.InvalidFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class FileValidatorTest {

    private FileValidator standardValidator;
    private FileValidator strictValidator;

    @BeforeEach
    void setUp() {
        // Standard validator allows all image types up to 5MB
        standardValidator = new FileValidator(
            "image/jpeg,image/png,image/gif,image/webp",
            5242880L // 5MB
        );

        // Strict validator only allows JPEG and PNG up to 1MB
        strictValidator = new FileValidator(
            "image/jpeg,image/png",
            1048576L // 1MB
        );
    }

    // --- Standard Validator Tests --- 

    @Test
    void validateImageFile_withValidJpeg_shouldPass() {
        // Create a mock JPEG file with valid magic numbers
        byte[] content = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}; // JPEG magic numbers
        MultipartFile file = new MockMultipartFile(
            "test.jpg",
            "test.jpg",
            "image/jpeg",
            content
        );

        assertDoesNotThrow(() -> standardValidator.validateImageFile(file));
    }

    @Test
    void validateImageFile_withValidPng_shouldPass() {
        // Create a mock PNG file with valid magic numbers
        byte[] content = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}; // PNG magic numbers
        MultipartFile file = new MockMultipartFile(
            "test.png",
            "test.png",
            "image/png",
            content
        );

        assertDoesNotThrow(() -> standardValidator.validateImageFile(file));
    }

    @Test
    void validateImageFile_withValidGif_shouldPass() {
        byte[] content = new byte[]{'G', 'I', 'F', '8', '9', 'a'}; // GIF magic numbers
        MultipartFile file = new MockMultipartFile(
            "test.gif",
            "test.gif",
            "image/gif",
            content
        );

        assertDoesNotThrow(() -> standardValidator.validateImageFile(file));
    }

    @Test
    void validateImageFile_withValidWebp_shouldPass() {
        byte[] content = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}; // WebP magic numbers
        MultipartFile file = new MockMultipartFile(
            "test.webp",
            "test.webp",
            "image/webp",
            content
        );

        assertDoesNotThrow(() -> standardValidator.validateImageFile(file));
    }

    @Test
    void validateImageFile_withEmptyFile_shouldThrowException() {
        MultipartFile file = new MockMultipartFile(
            "empty.jpg",
            "empty.jpg",
            "image/jpeg",
            new byte[]{}
        );

        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> standardValidator.validateImageFile(file)
        );
        assertEquals("File is empty", exception.getMessage());
    }

    @Test
    void validateImageFile_withInvalidType_shouldThrowException() {
        MultipartFile file = new MockMultipartFile(
            "test.txt",
            "test.txt",
            "text/plain",
            "test content".getBytes()
        );

        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> standardValidator.validateImageFile(file)
        );
        assertTrue(exception.getMessage().contains("not allowed"));
    }

    @Test
    void validateImageFile_withSpoofedContentType_shouldThrowException() {
        // File claims to be JPEG but has PDF content
        byte[] pdfContent = new byte[]{'%', 'P', 'D', 'F', '-'};
        MultipartFile file = new MockMultipartFile(
            "fake.jpg",
            "fake.jpg",
            "image/jpeg", // Claimed content type
            pdfContent    // Actual content
        );

        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> standardValidator.validateImageFile(file)
        );
        assertTrue(exception.getMessage().contains("not allowed"));
    }

    @Test
    void validateImageFile_withExactSizeLimit_shouldPass() {
        byte[] content = new byte[5242880]; // Exactly 5MB
        content[0] = (byte) 0xFF; // JPEG magic number
        content[1] = (byte) 0xD8;
        content[2] = (byte) 0xFF;
        
        MultipartFile file = new MockMultipartFile(
            "exact-size.jpg",
            "exact-size.jpg",
            "image/jpeg",
            content
        );

        assertDoesNotThrow(() -> standardValidator.validateImageFile(file));
    }

    @Test
    void validateImageFile_withOversizedFile_shouldThrowException() {
        // Create a file larger than 5MB
        byte[] largeContent = new byte[5242881]; // 5MB + 1 byte
        largeContent[0] = (byte) 0xFF; // JPEG magic number
        largeContent[1] = (byte) 0xD8;
        largeContent[2] = (byte) 0xFF;

        MultipartFile file = new MockMultipartFile(
            "large.jpg",
            "large.jpg",
            "image/jpeg",
            largeContent
        );

        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> standardValidator.validateImageFile(file)
        );
        assertTrue(exception.getMessage().contains("exceeds maximum limit"));
    }

    @Test
    void getFileExtension_withValidMimeTypes_shouldReturnCorrectExtension() {
        assertEquals(".jpg", standardValidator.getFileExtension("image/jpeg"));
        assertEquals(".png", standardValidator.getFileExtension("image/png"));
        assertEquals(".gif", standardValidator.getFileExtension("image/gif"));
        assertEquals(".webp", standardValidator.getFileExtension("image/webp"));
    }

    @Test
    void getFileExtension_withInvalidMimeType_shouldThrowException() {
        assertThrows(
            InvalidFileException.class,
            () -> standardValidator.getFileExtension("application/pdf")
        );
    }

    // --- Strict Validator Tests --- 

    @Test
    void validateImageFile_withGifInStrictMode_shouldThrowException() {
        byte[] content = new byte[]{'G', 'I', 'F', '8', '9', 'a'};
        MultipartFile file = new MockMultipartFile(
            "test.gif",
            "test.gif",
            "image/gif",
            content
        );

        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> strictValidator.validateImageFile(file)
        );
        assertTrue(exception.getMessage().contains("not allowed"));
    }

    @Test
    void validateImageFile_withJpegInStrictMode_shouldPass() {
        byte[] content = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        MultipartFile file = new MockMultipartFile(
            "test.jpg",
            "test.jpg",
            "image/jpeg",
            content
        );

        assertDoesNotThrow(() -> strictValidator.validateImageFile(file));
    }

    @Test
    void validateImageFile_withLargeFileInStrictMode_shouldThrowException() {
        byte[] content = new byte[1048577]; // Just over 1MB
        content[0] = (byte) 0xFF;
        content[1] = (byte) 0xD8;
        content[2] = (byte) 0xFF;

        MultipartFile file = new MockMultipartFile(
            "test.jpg",
            "test.jpg",
            "image/jpeg",
            content
        );

        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> strictValidator.validateImageFile(file)
        );
        assertTrue(exception.getMessage().contains("exceeds maximum limit"));
    }

    // --- Default Configuration Test --- 

    @Test
    void validateImageFile_withDefaultConfiguration_shouldUseDefaults() {
        // Create validator with null allowed types to test defaults (uses 10MB default size)
        FileValidator defaultValidator = new FileValidator(null, 10485760L);
        
        byte[] content = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}; // JPEG
        MultipartFile file = new MockMultipartFile(
            "test.jpg",
            "test.jpg",
            "image/jpeg",
            content
        );

        assertDoesNotThrow(() -> defaultValidator.validateImageFile(file));
    }
}
