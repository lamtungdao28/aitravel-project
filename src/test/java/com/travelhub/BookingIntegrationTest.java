package com.travelhub;
import com.travelhub.booking.dto.*;
import com.travelhub.booking.entity.BookingStatus;
import com.travelhub.booking.service.BookingService;
import com.travelhub.common.exception.BusinessException;
import com.travelhub.destination.entity.Destination;
import com.travelhub.place.entity.Place;
import com.travelhub.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
@Transactional
class BookingIntegrationTest extends PostgresIntegrationTest {
    @Autowired BookingService service;
    @Autowired EntityManager em;
    @Test void supportsOnlyAllowedTransitionsAndPendingEdits() {
        var r=request(true,LocalDate.now().plusDays(10));
        var b=service.create(r);
        assertThat(b.status()).isEqualTo(BookingStatus.PENDING);
        assertThatThrownBy(() -> service.complete(b.id())).isInstanceOf(BusinessException.class);
        service.update(b.id(),new BookingUpdateRequest(r.bookingDate(),LocalTime.NOON,3,"Updated"));
        assertThat(service.confirm(b.id()).status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThatThrownBy(() -> service.update(b.id(),new BookingUpdateRequest(r.bookingDate(),LocalTime.NOON,3,null)))
                .isInstanceOf(BusinessException.class);
        assertThat(service.complete(b.id()).status()).isEqualTo(BookingStatus.COMPLETED);
        assertThatThrownBy(() -> service.cancel(b.id())).isInstanceOf(BusinessException.class);
    }
    @Test void cancelledBookingsAreFinal() {
        var b=service.create(request(true,LocalDate.now().plusDays(1)));
        assertThat(service.cancel(b.id()).status()).isEqualTo(BookingStatus.CANCELLED);
        assertThatThrownBy(() -> service.confirm(b.id())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.cancel(b.id())).isInstanceOf(BusinessException.class);
    }
    @Test void rejectsPastDatesAndDisabledPlaces() {
        var past=request(true,LocalDate.now().minusDays(2));
        assertThatThrownBy(() -> service.create(past)).isInstanceOf(BusinessException.class);
        var disabled=request(false,LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> service.create(disabled)).isInstanceOf(BusinessException.class);
    }
    private BookingCreateRequest request(boolean enabled,LocalDate date) {
        User u=new User();u.setEmail(java.util.UUID.randomUUID()+"@example.test");u.setPassword("test-only");u.setFullName("Booker");em.persist(u);
        Destination d=new Destination();d.setName("Hanoi");d.setCountry("Vietnam");d.setTimezone("Asia/Ho_Chi_Minh");em.persist(d);
        Place p=new Place();p.setName("Restaurant");p.setDestination(d);p.setBookingEnabled(enabled);em.persist(p);
        return new BookingCreateRequest(u.getId(),p.getId(),date,LocalTime.NOON,2,null);
    }
}
