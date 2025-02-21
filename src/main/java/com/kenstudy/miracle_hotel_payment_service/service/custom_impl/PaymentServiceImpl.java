package com.kenstudy.miracle_hotel_payment_service.service.custom_impl;

import com.kenstudy.miracle_hotel_payment_service.exception.GuestNotFoundException;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
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


        LocalDateTime dateTime = LocalDateTime.now(ZoneId.systemDefault());
        int year = dateTime.getYear();
        String sessionId = "ID-" + year + "-" + new Random().nextInt(1000);


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
                .setCancelUrl("http://localhost:8080/checkout");
        for (BookedRoom bookedRoom : customer.getBookedRooms()) {
            String currency = bookedRoom.getCurrency();
            if (currency == null || currency.isEmpty()) {
                currency = "USD"; // Set default currency to "usd"
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

            String customerId = paymentRequest.getCustomerId();

            // Retrieve or initialize session list for the customer
            List<String> addSession = addCustomer.getOrDefault(customerId, new ArrayList<>());

            // Add the new session
            addSession.add(sessionId);
            addCustomer.put(customerId, addSession);
            customer.setCustomerSessions(addCustomer);
            customer.setStripeNumber(session.getCustomer());
            customer.setDate(new Date());
            paymentRepository.save(customer);
        } catch (StripeException e) {
            throw new RuntimeException(e.getMessage());
        }
        return PaymentResponse.builder()
                .status(customer.getStatus())
                .sessionId(sessionId)
                .customerId(session.getCustomer())
                .message("Payment session created")
                .stripeUrl(session.getUrl())
                .build();
    }

    @Override
    public List<PaymentBookedRoomDTO> findBookingRoomsByEmailAndCustomerId(String email, String customerId) {

        List<Object[]> results = paymentRepository.findCustomerBookedRoomsByGuestEmailAndCustomerId(email, customerId)
                .orElseThrow(() -> new GuestNotFoundException("No record found for email " + email + " and Id " + customerId) );

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
        List<Payment> customerList = paymentRepository.findByGuestEmail(paymentRequest.getGuestEmail())
                .orElse(Collections.emptyList());

        if (!customerList.isEmpty()) {
            List<Payment> updatedCustomers = customerList.stream().map(existingCustomer -> {
                if (PaymentConstant.INITIATE_PAYMENT.name().equals(existingCustomer.getStatus())) {
                    List<BookedRoom> existingBookedRooms = new ArrayList<>(existingCustomer.getBookedRooms());

                    List<BookedRoom> newBookedRooms = paymentRequest.getBookedRooms().stream()
                            .map(CustomerUtils::mapBookedRoom)
                            .collect(Collectors.toCollection(ArrayList::new));

                    existingBookedRooms.addAll(newBookedRooms);
                    existingCustomer.setBookedRooms(existingBookedRooms);
                    existingCustomer.setDate(new Date());

                } else if (PaymentConstant.PAYMENT_COMPLETED.name().equals(existingCustomer.getStatus())) {
                    return paymentRepository.save(CustomerUtils.createNewCustomer(paymentRequest));
                }

                if (existingCustomer.getCustomerSessions() == null) {
                    existingCustomer.setCustomerSessions(new HashMap<>());
                }
                return paymentRepository.save(existingCustomer);

            }).toList();

            // Return the latest updated payment based on the most recent date
            return updatedCustomers.stream()
                    .max(Comparator.comparing(Payment::getDate))
                    .orElse(updatedCustomers.get(0)); // Fallback to first if all dates are null
        }

        // If no existing customer, create a new one
        return paymentRepository.save(CustomerUtils.createNewCustomer(paymentRequest));
    }


}

