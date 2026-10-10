package com.travelhub.destination;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.travelhub.common.exception.BusinessException;
import com.travelhub.common.exception.ResourceNotFoundException;
import com.travelhub.destination.dto.DestinationCreateRequest;
import com.travelhub.destination.dto.DestinationUpdateRequest;
import com.travelhub.destination.entity.Destination;
import com.travelhub.destination.mapper.DestinationMapper;
import com.travelhub.destination.repository.DestinationRepository;
import com.travelhub.destination.service.DestinationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class DestinationServiceTest {
    @Mock DestinationRepository repository;
    private DestinationService service;

    @BeforeEach
    void setUp() {
        service = new DestinationService(repository, new DestinationMapper());
    }

    @Test
    void createNormalizesNamesAndRejectsInvalidTimezone() {
        when(repository.saveAndFlush(any(Destination.class)))
                .thenAnswer(
                        invocation -> {
                            Destination destination = invocation.getArgument(0);
                            destination.setId(1L);
                            return destination;
                        });
        var response =
                service.create(
                        new DestinationCreateRequest(
                                " Hanoi ",
                                " Vietnam ",
                                null,
                                null,
                                null,
                                null,
                                "Asia/Ho_Chi_Minh"));
        assertThat(response.name()).isEqualTo("Hanoi");
        assertThat(response.country()).isEqualTo("Vietnam");
        assertThatThrownBy(
                        () ->
                                service.create(
                                        new DestinationCreateRequest(
                                                "Paris",
                                                "France",
                                                null,
                                                null,
                                                null,
                                                null,
                                                "Invalid/Timezone")))
                .isInstanceOf(BusinessException.class);
        verify(repository, times(1)).saveAndFlush(any());
    }

    @Test
    void missingDestinationReturnsDomainException() {
        assertThatThrownBy(() -> service.get(42L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateChangesOnlyMutableFields() {
        Destination destination = destination();
        when(repository.findById(1L)).thenReturn(Optional.of(destination));
        var response =
                service.update(
                        1L,
                        new DestinationUpdateRequest(
                                "Da Nang",
                                "Vietnam",
                                "Coastal city",
                                null,
                                null,
                                null,
                                "Asia/Ho_Chi_Minh"));
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Da Nang");
        verify(repository).flush();
    }

    @Test
    void deletionRejectsPlaces() {
        when(repository.findById(1L)).thenReturn(Optional.of(destination()));
        when(repository.countPlaces(1L)).thenReturn(1L);
        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("places");
        verify(repository, never()).delete(any(Destination.class));
    }

    @Test
    void deletionRejectsTrips() {
        when(repository.findById(1L)).thenReturn(Optional.of(destination()));
        when(repository.countTrips(1L)).thenReturn(1L);
        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("trips");
        verify(repository, never()).delete(any(Destination.class));
    }

    @Test
    void deletionRemovesUnreferencedDestination() {
        Destination destination = destination();
        when(repository.findById(1L)).thenReturn(Optional.of(destination));
        service.delete(1L);
        verify(repository).delete(destination);
        verify(repository).flush();
    }

    private Destination destination() {
        Destination destination = new Destination();
        destination.setId(1L);
        destination.setName("Hanoi");
        destination.setCountry("Vietnam");
        return destination;
    }
}
