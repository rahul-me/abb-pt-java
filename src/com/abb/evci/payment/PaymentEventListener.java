package com.abb.evci.payment;

/**
 * Listener interface for Payment events
 */
public interface PaymentEventListener {

    /**
     * The result of the authorization. APPROVED means that the authorization is
     * OK and that the goods can be delivered. Depending on the payment mode,
     * some additional action is still required by the merchant to actually get
     * the funds. The other results can be used to provide feedback to the user
     * why the payment failed
     */
    public static enum PaymentAuthorizationResult {
        /** Payment has been approved for the given amount */
        APPROVED,
        /** Payment has failed */
        FAILED,
        /** Payment has been aborted by application */
        ABORTED
    }

    public static enum PaymentFailureReason {
        TECHNICAL_FAILURE,  // e.g. no communication with provider, terminal not working properly
        NOT_ACCEPTED, // not supported or not accepted (e.g. blocked or expired)
        TIMEOUT,      // for instance no card presented, user took to long to enter code
        AMOUNT_NOT_APPROVED // for example above limit on card
    }

    public enum ConfirmationResult {
        SUCCEEDED, ABORTED, FAILED
    }

    /**
     * Callback to inform the application of the payment authorization result.
     *
     * After a start of an payment request the results is provided.
     *
     * @param paymentSessionId
     *            the identifier of the payment session
     * @param authorizationResult
     *            the result
     * @param code
     *            a specific (terminal specific) code, maybe empty
     * @param reason
     *            the reason the payment failed (in case of FAILED) or null (in case of ABORT/APPROVED)
     * @param info
     *            transaction details (in case of failure or abort)
     * @param detail
     *            a human readable text in case of a failure, maybe empty or
     *            null in other cases
     */
    public void authorizationResult(int paymentSessionId, PaymentAuthorizationResult authorizationResult, String code,
            PaymentFailureReason reason, TransactionInfo info, String detail);

    /**
     * Callback to inform the application that the payment terminal has
     * confirmed processing the payment.
     *
     * After the payment has finished executing, this callback will pass the
     * result of the payment and optionally the receipt of the payment to the
     * application.
     *
     * @param paymentSessionId
     *            the identifier of the payment session
     * @param result
     *            the outcome of the payment.
     * @param info
     *            transaction details
     * @param detail
     *            a human readable text in case of a failure, maybe empty or
     *            null in other cases
     */
    public void paymentConfirmation(int paymentSessionId, ConfirmationResult result, TransactionInfo info,
            String detail);

    /**
     * Callback to inform the application that the payment terminal has
     * confirmed processing the refund.
     *
     * After the refund has finished executing, this callback will pass the
     * result of the refund and optionally the receipt of the refund to the
     * application.
     *
     * @param paymentSessionId
     *            the identifier of the payment session
     * @param result
     *            the outcome of the refund.
     * @param info
     *            transaction details
     * @param detail
     *            a human readable text in case of a failure, maybe empty or
     *            null in other cases
     */
    public void refundConfirmation(int paymentSessionId, ConfirmationResult result, TransactionInfo info,
            String detail);

}
