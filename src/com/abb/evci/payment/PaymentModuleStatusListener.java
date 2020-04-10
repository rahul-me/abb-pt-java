package com.abb.evci.payment;

/**
 * Listener interface for Payment Module status changes and responses.
 *
 */
public interface PaymentModuleStatusListener {

    public static enum PaymentModuleStatus {

        NOT_AVAILABLE,       // the payment module is not initiated or is temporary busy with non-payment tasks     
        READY_FOR_PAYMENT,    // a new payment can be accepted             
        PAYING,          // a transaction is going on. No new payments can be accepted
        OUT_OF_ORDER      // there are technical issues. The payment terminal cannot be used.
    }

    /**
     * Callback to inform the application that the status of the PT has changed.
     *
     * @param status
     *            the new status
     * @param detail
     *            Some detail info, can be empty normally, but must be filled with meaningful info in case of
     *            OUT_OF_ORDER
     */
    public void statusChanged(PaymentModuleStatus status, String detail);

}
