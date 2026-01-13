package com.grow.payment.adapter.persistence;

import com.grow.payment.domain.Payment;
import com.grow.payment.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByMemberIdAndStudyIdAndStatus(Long memberId, Long studyId, PaymentStatus status);

    List<Payment> findByMemberIdAndStatus(Long memberId, PaymentStatus status);

    List<Payment> findByStudyIdAndStatus(Long studyId, PaymentStatus status);

    boolean existsByMemberIdAndStudyIdAndStatus(Long memberId, Long studyId, PaymentStatus status);

    Optional<Payment> findByPaymentKey(String paymentKey);
}
