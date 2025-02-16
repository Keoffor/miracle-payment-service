package com.kenstudy.miracle_hotel_payment_service.service.custom_impl;

import com.kenstudy.miracle_hotel_payment_service.constant.PaymentConstant;
import com.kenstudy.miracle_hotel_payment_service.dto.BookedRoom;
import com.kenstudy.miracle_hotel_payment_service.dto.PaymentBookedRoomDTO;
import com.kenstudy.miracle_hotel_payment_service.dto.PaymentRequest;
import com.kenstudy.miracle_hotel_payment_service.dto.response.PaymentResponse;
import com.kenstudy.miracle_hotel_payment_service.model.Payment;
import com.kenstudy.miracle_hotel_payment_service.repository.PaymentRepository;
import com.kenstudy.miracle_hotel_payment_service.service.PaymentService;
import com.kenstudy.miracle_hotel_payment_service.util.CustomerUtils;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j

public class PaymentServiceImpl implements PaymentService {
    @Value("${stripe.secret.key}")
    private String secretKey;

    @Autowired
    private PaymentRepository paymentRepository;

    public PaymentServiceImpl() {
    }

    @Transactional
    public PaymentResponse bookedRoomCheckout(PaymentRequest paymentRequest) throws StripeException {
        UUID sessionId = UUID.randomUUID();
        Stripe.apiKey = secretKey;
        SessionCreateParams.LineItem lineItem = null;
        Session session = null;

        //find existing customer from payment DB or create a new customer
        Payment customer = findOrCreateCustomer(paymentRequest);

        //find existing customer from Stripe or create a new customer
        Customer customerStripe = CustomerUtils.findOrCreateCustomer(customer.getGuestEmail(),
                customer.getGuestFullName());
        SessionCreateParams.Builder paramBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomer(customerStripe.getId())
                .setSuccessUrl("http://localhost:8080/booking-success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("http://localhost:8080/failure");
        for (BookedRoom bookedRoom : customer.getBookedRooms()) {
            String currency = bookedRoom.getCurrency();
            if (currency == null || currency.isEmpty()) {
                currency = "usd"; // Set default currency to "usd"
            }
            BigDecimal bigDecimal = new BigDecimal(100);
            paramBuilder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .putMetadata("room_id", bookedRoom.getId())
                                                            .setName(bookedRoom.getRoomType())
                                                            .build()
                                            ).setCurrency(currency)
                                            .setUnitAmountDecimal(bookedRoom.getAmount().multiply(bigDecimal)).build())
                            .build()
            );
        }
        try {
            session = Session.create(paramBuilder.build());
            Map<String, List<String>> addCustomer = customer.getCustomerSessions()
                    != null ? new HashMap<>(customer.getCustomerSessions()) : new HashMap<>();
            String sessionCustomer = session.getCustomer();

            // Retrieve or initialize session list for the customer
            List<String> addSession = addCustomer.getOrDefault(sessionCustomer, new ArrayList<>());

            // Add the new session
            addSession.add(String.valueOf(sessionId));
            addCustomer.put(sessionCustomer, addSession);
            customer.setCustomerSessions(addCustomer);
            customer.setDate(new Date());
            paymentRepository.save(customer);
        } catch (StripeException e) {
            throw new RuntimeException(e.getMessage());
        }
        return PaymentResponse.builder()
                .status(customer.getStatus())
                .sessionId(String.valueOf(sessionId))
                .message("Payment session created")
                .stripeUrl(session.getUrl())
                .build();
    }

    @Override
    public List<PaymentBookedRoomDTO> findBookedRoomsByEmailAndCustomerId(String email, String customerId) {
        List<Object[]> results = paymentRepository.findCustomerBookedRoomsByGuestEmailAndCustomerId(email, customerId);

        if (results.isEmpty()) {
            throw new RuntimeException("No record found");
        }
        return results.stream().map(objects -> new PaymentBookedRoomDTO(
                        (Long) objects[0],
                        (String) objects[1],
                        (String) objects[2],
                        (String) objects[3],
                        (Long) objects[4],
                        (String) objects[5],
                        (BigDecimal) objects[6],
                        (Date) objects[7]  // date
                )).filter(dto -> PaymentConstant.INITIATE_PAYMENT.name().equals(dto.getStatus())).sorted(
                        Comparator.comparing(
                                PaymentBookedRoomDTO::getDate, Comparator.nullsLast(Comparator.naturalOrder()) // Sort by date, nulls last
                        ).thenComparing(PaymentBookedRoomDTO::getRoomType) // If date is null, sort by roomType
                )
                .toList();
    }


    private Payment findOrCreateCustomer(PaymentRequest paymentRequest) {
        return paymentRepository.findByGuestEmail(paymentRequest.getGuestEmail())
                .map(existingCustomer -> {
                    if (StringUtils.equals(existingCustomer.getStatus(), PaymentConstant.INITIATE_PAYMENT.name())) {
                        List<BookedRoom> existingBookedRooms = new ArrayList<>(existingCustomer.getBookedRooms());

                        List<BookedRoom> newBookedRooms = paymentRequest.getBookedRooms().stream()
                                .map(CustomerUtils::mapBookedRoom)
                                .collect(Collectors.toCollection(ArrayList::new));

                        existingBookedRooms.addAll(newBookedRooms);
                        existingCustomer.setBookedRooms(existingBookedRooms);
                        existingCustomer.setDate(new Date());

                    } else if (StringUtils.equals(existingCustomer.getStatus(), PaymentConstant.PAYMENT_COMPLETED.name())) {
                        return paymentRepository.save(CustomerUtils.createNewCustomer(paymentRequest));
                    }

                    if (existingCustomer.getCustomerSessions() == null) {
                        existingCustomer.setCustomerSessions(new HashMap<>());
                    }
                    return paymentRepository.save(existingCustomer);

                })
                .orElseGet(() -> paymentRepository.save(CustomerUtils.createNewCustomer(paymentRequest)));
    }


}
