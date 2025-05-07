// (Removed invalid top-level test methods)

import com.autotrader.autotraderbackend.controller.CarListingController;
import com.autotrader.autotraderbackend.payload.request.CreateListingRequest;
import com.autotrader.autotraderbackend.payload.request.ListingFilterRequest;
import com.autotrader.autotraderbackend.payload.response.CarListingResponse;
import com.autotrader.autotraderbackend.payload.response.LocationResponse;
import com.autotrader.autotraderbackend.payload.response.PageResponse;
import com.autotrader.autotraderbackend.service.CarListingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CarListingControllerTest {

    @Mock
    private CarListingService carListingService;

    @InjectMocks
    private CarListingController carListingController;

    private CreateListingRequest createRequest;
    private CarListingResponse carListingResponse;
    private UserDetails userDetails;
    private MockMultipartFile mockImage;

    @BeforeEach
    void setUp() {
        // Setup test data
        createRequest = new CreateListingRequest();
        createRequest.setTitle("Test Car");
        createRequest.setBrand("Toyota");
        createRequest.setModel("Camry");
        createRequest.setModelYear(2022);
        createRequest.setMileage(5000);
        createRequest.setPrice(new BigDecimal("25000.00"));
        createRequest.setLocationId(1L);
        createRequest.setDescription("Test Description");

        // Create a LocationResponse for the response object
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setId(1L);
        locationResponse.setDisplayNameEn("Test Location");
        // Set other necessary fields for LocationResponse if needed

        carListingResponse = new CarListingResponse();
        carListingResponse.setId(1L);
        carListingResponse.setTitle("Test Car");
        carListingResponse.setBrand("Toyota");
        carListingResponse.setModel("Camry");
        carListingResponse.setModelYear(2022);
        carListingResponse.setMileage(5000);
        carListingResponse.setPrice(new BigDecimal("25000.00"));
        carListingResponse.setLocationDetails(locationResponse); // Use setLocationDetails instead of setLocation
        carListingResponse.setDescription("Test Description");
        carListingResponse.setSellerId(1L);
        carListingResponse.setSellerUsername("testuser");
        carListingResponse.setApproved(false);

        mockImage = new MockMultipartFile(
            "image",
            "test.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );

        userDetails = mock(UserDetails.class);
        lenient().when(userDetails.getUsername()).thenReturn("testuser");
    }

    @Test
    void createListing_ShouldReturnCreatedResponse() {
        // Arrange
        when(carListingService.createListing(any(CreateListingRequest.class), isNull(), anyString()))
                .thenReturn(carListingResponse);

        // Act
        ResponseEntity<CarListingResponse> response = carListingController.createListing(createRequest, userDetails);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(carListingResponse, response.getBody());
    }

    @Test
    void createListingWithImage_ShouldReturnCreatedResponse() {
        // Arrange
        when(carListingService.createListing(any(CreateListingRequest.class), any(MultipartFile.class), anyString()))
                .thenReturn(carListingResponse);

        // Act
        ResponseEntity<?> response = carListingController.createListingWithImage(createRequest, mockImage, userDetails);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(carListingResponse, response.getBody());
    }

    @Test
    void getAllListings_ShouldReturnListingsPage() {
        // Arrange
        List<CarListingResponse> listings = new ArrayList<>();
        listings.add(carListingResponse);
        Page<CarListingResponse> page = new PageImpl<>(listings);
        when(carListingService.getAllApprovedListings(any(Pageable.class))).thenReturn(page);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").descending());
        // Act
        ResponseEntity<PageResponse<CarListingResponse>> response = carListingController.getAllListings(pageable);
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(listings, Objects.requireNonNull(response.getBody()).getContent());
    }

    @Test
    void filterListings_ShouldReturnFilteredListings() {
        // Arrange
        List<CarListingResponse> listings = new ArrayList<>();
        listings.add(carListingResponse);
        Page<CarListingResponse> page = new PageImpl<>(listings);
        ListingFilterRequest filterRequest = new ListingFilterRequest();
        filterRequest.setBrand("Toyota");
        when(carListingService.getFilteredListings(any(ListingFilterRequest.class), any(Pageable.class))).thenReturn(page);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").descending());
        // Act
        ResponseEntity<PageResponse<CarListingResponse>> response = carListingController.getFilteredListings(filterRequest, pageable);
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(listings, Objects.requireNonNull(response.getBody()).getContent());
    }
    @Test
    void getAllListings_ShouldReturnListingsSortedByPriceAscAndDesc() {
        // Arrange: create listings with different prices
        CarListingResponse listing1 = new CarListingResponse();
        listing1.setId(1L);
        listing1.setPrice(new BigDecimal("10000.00"));
        CarListingResponse listing2 = new CarListingResponse();
        listing2.setId(2L);
        listing2.setPrice(new BigDecimal("20000.00"));
        CarListingResponse listing3 = new CarListingResponse();
        listing3.setId(3L);
        listing3.setPrice(new BigDecimal("15000.00"));

        // Ascending order
        List<CarListingResponse> ascList = List.of(listing1, listing3, listing2);
        Page<CarListingResponse> ascPage = new PageImpl<>(ascList);
        Pageable ascPageable = org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("price").ascending());
        when(carListingService.getAllApprovedListings(ascPageable)).thenReturn(ascPage);
        // Act
        ResponseEntity<PageResponse<CarListingResponse>> ascResponse = carListingController.getAllListings(ascPageable);
        // Assert
        assertNotNull(ascResponse.getBody());
        List<CarListingResponse> ascResult = ascResponse.getBody().getContent();
        assertEquals(3, ascResult.size());
        assertEquals(new BigDecimal("10000.00"), ascResult.get(0).getPrice());
        assertEquals(new BigDecimal("15000.00"), ascResult.get(1).getPrice());
        assertEquals(new BigDecimal("20000.00"), ascResult.get(2).getPrice());

        // Descending order
        List<CarListingResponse> descList = List.of(listing2, listing3, listing1);
        Page<CarListingResponse> descPage = new PageImpl<>(descList);
        Pageable descPageable = org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("price").descending());
        when(carListingService.getAllApprovedListings(descPageable)).thenReturn(descPage);
        // Act
        ResponseEntity<PageResponse<CarListingResponse>> descResponse = carListingController.getAllListings(descPageable);
        // Assert
        assertNotNull(descResponse.getBody());
        List<CarListingResponse> descResult = descResponse.getBody().getContent();
        assertEquals(3, descResult.size());
        assertEquals(new BigDecimal("20000.00"), descResult.get(0).getPrice());
        assertEquals(new BigDecimal("15000.00"), descResult.get(1).getPrice());
        assertEquals(new BigDecimal("10000.00"), descResult.get(2).getPrice());
    }

    @Test
    void getAllListings_ShouldReturnListingsSortedByCreatedAtAscAndDesc() {
        // Arrange: create listings with different createdAt timestamps
        CarListingResponse listing1 = new CarListingResponse();
        listing1.setId(1L);
        listing1.setCreatedAt(java.time.LocalDateTime.of(2023, 1, 1, 10, 0));
        CarListingResponse listing2 = new CarListingResponse();
        listing2.setId(2L);
        listing2.setCreatedAt(java.time.LocalDateTime.of(2023, 1, 2, 10, 0));
        CarListingResponse listing3 = new CarListingResponse();
        listing3.setId(3L);
        listing3.setCreatedAt(java.time.LocalDateTime.of(2023, 1, 3, 10, 0));

        // Ascending order
        List<CarListingResponse> ascList = List.of(listing1, listing2, listing3);
        Page<CarListingResponse> ascPage = new PageImpl<>(ascList);
        Pageable ascPageable = org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").ascending());
        when(carListingService.getAllApprovedListings(ascPageable)).thenReturn(ascPage);
        // Act
        ResponseEntity<PageResponse<CarListingResponse>> ascResponse = carListingController.getAllListings(ascPageable);
        // Assert
        assertNotNull(ascResponse.getBody());
        List<CarListingResponse> ascResult = ascResponse.getBody().getContent();
        assertEquals(3, ascResult.size());
        assertEquals(listing1.getCreatedAt(), ascResult.get(0).getCreatedAt());
        assertEquals(listing2.getCreatedAt(), ascResult.get(1).getCreatedAt());
        assertEquals(listing3.getCreatedAt(), ascResult.get(2).getCreatedAt());

        // Descending order
        List<CarListingResponse> descList = List.of(listing3, listing2, listing1);
        Page<CarListingResponse> descPage = new PageImpl<>(descList);
        Pageable descPageable = org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").descending());
        when(carListingService.getAllApprovedListings(descPageable)).thenReturn(descPage);
        // Act
        ResponseEntity<PageResponse<CarListingResponse>> descResponse = carListingController.getAllListings(descPageable);
        // Assert
        assertNotNull(descResponse.getBody());
        List<CarListingResponse> descResult = descResponse.getBody().getContent();
        assertEquals(3, descResult.size());
        assertEquals(listing3.getCreatedAt(), descResult.get(0).getCreatedAt());
        assertEquals(listing2.getCreatedAt(), descResult.get(1).getCreatedAt());
        assertEquals(listing1.getCreatedAt(), descResult.get(2).getCreatedAt());
    }

    @Test
    void getFilteredListingsByParams_ShouldThrowIllegalArgumentExceptionForNonWhitelistedSortField() {
        // Arrange
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("nonExistentField"));
        when(carListingService.getFilteredListings(any(ListingFilterRequest.class), eq(pageable)))
            .thenThrow(new IllegalArgumentException("Sorting by field 'nonExistentField' is not allowed."));

        // Act & Assert
        IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> carListingController.getFilteredListingsByParams(
                null, // brand
                null, // model
                null, // minYear
                null, // maxYear
                null, // location
                null, // locationId
                null, // minPrice
                null, // maxPrice
                null, // minMileage
                null, // maxMileage
                null, // isSold
                null, // isArchived
                pageable
            )
        );
        assertEquals("Sorting by field 'nonExistentField' is not allowed.", ex.getMessage());
    }
}
