package com.abb.evci.payment;

import com.abb.evci.payment.PaymentEventListener.ConfirmationResult;

/**
 * Listener interface for Payment Module status changes and responses.
 */
public interface PaymentCommandResultListener {

    /**
     * Callback to inform the application that the payment terminal has finished executing the command
     *
     * @param command
     * @param result
     * @param info
     */
    public void commandFinished(String command, ConfirmationResult result, String info);

}
