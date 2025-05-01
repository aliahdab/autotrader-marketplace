package com.autotrader.autotraderbackend.validation;

import static org.junit.jupiter.api.Assertions.*;

import com.autotrader.autotraderbackend.payload.request.CreateListingRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Set;

class ModelYearValidationTest {

    private static ValidatorFactory validatorFactory;
    private Validator validator;
    
    @BeforeAll
    static void setUpValidatorFactory() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }
    
    @AfterAll
    static void closeValidatorFactory() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }
    
    @BeforeEach
    void setUp() {
        validator = validatorFactory.getValidator();
    }
    
    @Test
    void shouldValidateCurrentYear() {
        // Arrange
        CreateListingRequest request = createValidRequest();
        request.setModelYear(Year.now().getValue());
        
        // Act
        Set<ConstraintViolation<CreateListingRequest>> violations = validator.validate(request);
        
        // Assert
        assertTrue(violations.isEmpty(), "Current year should be valid");
    }
    
    @Test
    void shouldValidateOldYear() {
        // Arrange
        CreateListingRequest request = createValidRequest();
        request.setModelYear(1920);
        
        // Act
        Set<ConstraintViolation<CreateListingRequest>> violations = validator.validate(request);
        
        // Assert
        assertTrue(violations.isEmpty(), "1920 should be valid");
    }
    
    @Test
    void shouldRejectYearTooOld() {
        // Arrange
        CreateListingRequest request = createValidRequest();
        request.setModelYear(1919);
        
        // Act
        Set<ConstraintViolation<CreateListingRequest>> violations = validator.validate(request);
        
        // Assert
        assertFalse(violations.isEmpty(), "1919 should be invalid");
        assertEquals(1, violations.size());
        assertEquals("Year must be 1920 or later", 
                     violations.iterator().next().getMessage());
    }
    
    @Test
    void shouldRejectFutureYear() {
        // Arrange
        CreateListingRequest request = createValidRequest();
        request.setModelYear(Year.now().getValue() + 1);
        
        // Act
        Set<ConstraintViolation<CreateListingRequest>> violations = validator.validate(request);
        
        // Assert
        assertFalse(violations.isEmpty(), "Future year should be invalid");
        assertEquals(1, violations.size());
        assertEquals("Year must not be later than the current year", 
                     violations.iterator().next().getMessage());
    }
    
    @Test
    void shouldRejectNon4DigitYear() {
        // Arrange
        CreateListingRequest request = createValidRequest();
        request.setModelYear(12345); // 5 digits
        
        // Act
        Set<ConstraintViolation<CreateListingRequest>> violations = validator.validate(request);
        
        // Assert
        assertFalse(violations.isEmpty(), "5-digit year should be invalid");
        assertEquals(1, violations.size());
        assertEquals("Year must be a 4-digit number", 
                     violations.iterator().next().getMessage());
    }
    
    private CreateListingRequest createValidRequest() {
        CreateListingRequest request = new CreateListingRequest();
        request.setTitle("Test Car");
        request.setBrand("Test Brand");
        request.setModel("Test Model");
        request.setModelYear(2023);
        request.setPrice(new BigDecimal("15000.00"));
        request.setMileage(5000);
        request.setLocation("Test Location");
        return request;
    }
}
