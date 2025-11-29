package com.grow.payment.domain;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentEventMessagePublisher {

    private  ApplicationEventPublisher publisher;

    public PaymentEventMessagePublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publishEvent(PaymentEventMessage paymentEventMessage) {
        publisher.publishEvent(paymentEventMessage);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentEvent(PaymentEventMessage event) {
        // 트랜잭션 커밋 후 이벤트 처리
        // 실제 이벤트 처리 로직은 별도의 리스너에서 구현
    }
}
