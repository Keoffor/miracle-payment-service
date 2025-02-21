package com.kenstudy.miracle_hotel_payment_service.repository;

import com.kenstudy.miracle_hotel_payment_service.dto.PaymentBookedRoomDTO;
import com.kenstudy.miracle_hotel_payment_service.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<List<Payment>> findByGuestEmail(String email);

//    @Query(value = "SELECT DISTINCT p.* FROM payment p " +
//            "JOIN customer_sessions cs ON p.id = cs.payment_id " +
//            "WHERE cs.customer_id = :customerId", nativeQuery = true)
//    List<Payment>findPaymentsByCustomerId(@Param("customerId") String customerId);
//  @Query(value = "SELECT DISTINCT p.* FROM payment p WHERE stripe_number = : number", nativeQuery = true)
    @Query(value = "SELECT DISTINCT p.* FROM payment p " +
            "JOIN customer_sessions cs ON p.id = cs.payment_id " +
            "WHERE p.stripe_number = :number", nativeQuery = true)
    List<Payment> findPaymentByStripeNumber(@Param("number") String number);

    @Query(value = "SELECT DISTINCT p.id, p.guest_full_name,p.guest_email, p.status, br.room_id, br.room_type, " +
            "br.amount,br.date FROM payment p " +
            "JOIN booked_rooms br ON p.id = br.payment_id " +
            "JOIN customer_sessions cs ON p.id = cs.payment_id " +
            "WHERE p.guest_email = :guestEmail " +
            "AND cs.customer_id = :customerId",
            nativeQuery = true)
    Optional<List<Object[]>> findCustomerBookedRoomsByGuestEmailAndCustomerId(
            @Param("guestEmail") String guestEmail,
            @Param("customerId") String customerId);

}