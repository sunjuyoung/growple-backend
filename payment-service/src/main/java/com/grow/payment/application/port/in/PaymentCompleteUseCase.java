package com.grow.payment.application.port.in;

import com.grow.payment.domain.LedgerEventMessage;
import com.grow.payment.domain.WalletEventMessage;

public interface PaymentCompleteUseCase {
    void completePayment(WalletEventMessage walletEventMessage);
    void completePayment(LedgerEventMessage ledgerEventMessage);
}
