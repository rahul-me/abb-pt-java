
package com.abb.evci.payment;

import com.abb.evci.payment.PaymentModuleStatusListener.PaymentModuleStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Interface to the payment module for the payment process.<br/>
 * Note that the configuration and diagnosis of the payment module is also part
 * of this interface.
 * <p>
 *
 * This interface is used for different payment terminals for example CCV, Nayax
 * and WeChat.
 * <p>
 *
 * The following payment models are defined: <br/>
 *
 * <ul>
 * <li>Upfront payment: The customer pays the requested amount immediately, this
 * amount is immediately deducted from the card or account. No additional
 * confirmation is required.
 * <li>Upfront payment with confirmation (vending mode): The requested amount is
 * authorized but this amount is only deducted from the card or account after
 * the merchant sends a confirmation. This gives the merchant the option to
 * first deliver the goods before actual deducting the money. This typically
 * works well in a vending machine where you only want to deduct the payment
 * after the vending machine has actually delivered the good.
 * <li>Pre-authorize: An initial amount is pre-authorized, and the merchant
 * decides in a later stage what the actual amount will be. This actual amount
 * should not be higher than the pre-authorized amount.
 * </ul>
 *
 * Some terminals have the option that the payment can be (partially) refunded
 * afterwards. This is different than pre-authorization, since a complete new
 * transaction can be created and the funds are transfered back from the
 * merchant to the customer. Refund is only allowed in case:
 * <p>
 * <ul>
 * <li>Refund is supported.
 * <li>The session was confirmed and an amount is deducted.
 * <li>No refund has been made before on this session.
 * <li>The refunded amount is not bigger then the initially paid amount
 * </ul>
 *
 * A payment module must be started using the {@link #initiate(List, List)} call. No other calls
 * to the module are allowed before invoking the initiate call. The payment module
 * has the following states:
 * <ul>
 * <li>NOT_AVAILABLE : the payment module is not initiated or is temporary busy with non-payment task
 * <li>READY_FOR_PAYMENT: a new payment can be accepted
 * <li>PAYING: a transaction is going on. No new payments can be accepted
 * <li>OUT_OF_ORDER: there is technical issues. The payment terminal cannot be used.
 * </ul>
 *
 * A (new) payment session is started as soon as a successful {@link #startPayment(String, BigDecimal, PaymentEventListener)} is
 * invoked and a valid payment session id is created. <p>
 * Every transaction must get an unique identifier (positive value), the payment session id,
 * which should only be used for internal references. This is not necessarily the same as 
 * the transaction id which is used as a  reference to the customer on receipts etc. <p> 
 *
 * The transaction id (part of the TransactionInfo) is a unique reference to a (successful) payment session,
 * used as reference to the customer. A module may also create a transaction id for incomplete, non successful sessions.<p>
 *
 * A payment session can end in 2 ways:<ul>
 * <li> Due to an authorizationResult that is not successful. In that case the relevant TransactionInfo can be provided via the
 * {@link PaymentEventListener#authorizationResult(int, com.abb.evci.payment.PaymentEventListener.PaymentAuthorizationResult, String, com.abb.evci.payment.PaymentEventListener.PaymentFailureReason, String, TransactionInfo)}
 * After this the payment session id has become obsolete. 
 * <li> Due to the payment confirm. 
 * As soon as the payment session is finalized (after a confirm or abort) the terminal must invoke the 
 * {@link PaymentEventListener#paymentConfirmation(int, com.abb.evci.payment.PaymentEventListener.ConfirmationResult, TransactionInfo, String)}
 * with detail information. After this the payment session id has become obsolete. It is up to
 * the discretion of the module to decide what info will be provided in case of an abort or failure.
 * </ul>
 * <p>
 *
 * <b> Configuration </b>
 * The module must implement the following configuration items:
 * <ul>
 * <li>SimultaneousSessionsSupported:
 * Indicate if a new payment session can be started before the previous session is finalized
 * <li>Currency: ISO 4217 currency code
 * <li>Language: ISO 639-1 code
 * <li>SoftwareVersion  (read only)
 * <li>HardwareVersion (read only)
 * <li>TerminalId 
 * </ul>
 *
 * <b>Nayax specific items</b>
 * <ul>
 * <li>serialPortId : name of the serial port, must be set during {@link #initiate(List, List)}</li>
 * </ul>
 *
 * <b> Nayax Provisioning </b>
 * The terminal needs to be provisioned at least once before it can be used. Provisioning means
 * it is mapped to a specific customer <br>
 * The payment module needs to implement a provision command with 3 arguments
 * (customerContractReference, currency, deviceReference)
 * An application must invoked at least once
 * {@link #executeCommand(String, List, PaymentCommandResultListener)};
 */

public interface PaymentModuleInterface {

    public enum PaymentStyle {
        UPFRONT, UPFRONT_WITH_CONFIRM, PRE_AUTHORIZE
    }

    /**
     * Start the payment module. This must be called before using any other method. After invoking this method
     * the payment module may allocate system resources including threads and memory buffers.<br/>
     * 
     * @return false if the payment terminal was already initiated, initiating fails or the provided keys/values are
     *         invalid. 
     */
    public boolean initiate(Map<String, String> map);

    /**
     * Stop the payment module. The payment module should release all resources including threads and memory buffers.
     * After invoking this call, no other calls are allowed to the module except {@link #initiate()}
     * 
     * @return false if the payment terminal was not initiated yet or in case shutdown fails
     */
    public boolean shutdown();

    /**
     * Provide the current value of the configuration parameter identified by
     * the key
     *
     * @param key:
     *            the identifier of the configuration parameter
     *
     * @return the value of the requested parameter or null if there is no value
     *         or the key is not supported
     */
    public String getConfiguration(String key);

    /**
     * Change the value of a configuration parameter identified by the key
     * It depends on the payment module and terminal if these parameters
     * are persistent or not (see documentation of vendor payment module).
     * It also is vendor specific if parameters can be changed runtime or must
     * be set once. In case the parameter is of a different dataType (for example Integer)
     * the provided value must be the toString value of that dataType.
     *
     * @param key:
     *            the identifier of the configuration parameter
     * @param value:
     *            the requested value
     *
     * @return false if the parameter could not be set
     */
    public boolean setConfiguration(String key, String value);

    /**
     * Provides a list of supported parameters. These parameters
     * can be retrieved individually using {@link #getConfiguration(String)} or
     * changed using {@link #setConfiguration(String, String)}
     *
     * @return the list of supported parameters
     */
    public List<String> getSupportedParameters();

    /**
     * Provides a list of supported commands. These commands can be invoked
     * using {@link #executeCommand(String, String, PaymentCommandResultListener)}
     *
     * @return the list of supported parameters
     */
    public List<String> getSupportedCommands();

    /**
     * To execute a terminal specific command on the Payment Terminal that
     * provides an asynchronous result <br>
     * The list of commands supported can be
     * retrieved using {@link #getSupportedCommands()}
     *
     * @param command;
     *            the identifier of the command to be executed
     * @param args:
     *            optional arguments to be passed to the command
     * @param listener:
     *            listener interface for the result
     *
     * @return false if the command is not supported or cannot be executed, for example due to the current 
     * state of the module.
     */
    public boolean executeCommand(String command, List<String> args, PaymentCommandResultListener listener);

    /**
     * Register the listener which will be informed about the status of the
     * Payment Terminal. The terminal must call the listener every time the status 
     * changes.<br>
     * Only a single listener is allowed. In case there is already a listener
     * active, this will be replaced. If null is provided no listener will be
     * active. In that case no status events are sent out.
     */
    public void registerStatusListener(PaymentModuleStatusListener listener);

    /**
     * Request to start a payment transaction for the provided amount and
     * currency immediately. Immediately means without any significant delay
     * the payment can be started on the terminal and the user can present his card.
     * <br/>
     *
     * Based on the payment style, the amount will immediately be deducted
     * (UPFRONT) or a pre-authorization request will be issued.
     *
     * @param currency:
     *            ISO 4217 currency code (3 character string).
     * @param amount:
     *            must be greater than zero. The scale must be 2.
     * @param listener:
     *            callback which will receive the events about this specific
     *            transaction.
     *
     * @return an unique identifier (greater than zero) for this payment session or -1 in case the
     *         payment cannot be started e.g. since another payment is active, the currency is not
     *         supported
     */
    public int startPayment(String currency, BigDecimal amount, PaymentEventListener listener);

    /**
     * Request to abort an ongoing payment without any significant delay. <p>
     *
     * Abort can be called at any time during the payment process. In case the
     * payment was pre-authorized and not finalized yet, it should be be
     * cancelled. In case the payment was upfront paid and not confirmed yet, it
     * should be cancelled. <p>
     * 
     * In case the payment is aborted one of the appropriate events 
     * {@link PaymentEventListener#authorizationResult(int, com.abb.evci.payment.PaymentEventListener.PaymentAuthorizationResult, String, com.abb.evci.payment.PaymentEventListener.PaymentFailureReason, String)},
     * {@link PaymentEventListener#paymentConfirmation(int, com.abb.evci.payment.PaymentEventListener.ConfirmationResult, TransactionInfo, String)}
     * must still be sent depending on when the abort was invoked. 
     * 
     * @param paymentSessionId:
     *            identifier for the payment session
     * @return false in case of an invalid paymentSessionId or the terminal cannot stop an already ongoing 
     * transaction due to the nature of the terminal.  
     */
    public boolean abortPayment(int paymentSessionId);

    /**
     * Request to confirm an ongoing payment with the requested amount. (Only
     * applicable in case of pre authorized payment)
     *
     * @param finalAmount:
     *            amount that should be charged to the customer, this must be 0
     *            <= amount <= approvedAmount. The scale must be 2.
     * @return false in case payment session is invalid or not in the correct
     *         state, or finalAmount incorrect
     */
    public boolean confirmPayment(int paymentSessionId, BigDecimal finalAmount);

    /**
     * Request to confirm an ongoing payment. (Only applicable in case of
     * upfront payment with confirmation)
     *
     * @return false in case payment session is invalid or not in the correct
     *         state
     */
    public boolean confirmPayment(int paymentSessionId);

    /**
     * Request to refund a previous payment. (Only applicable in case of the
     * payment module supports refunding)
     *
     * @param paymentSessionId
     *            the session which to refund. This session must already have
     *            been confirmed.
     * @param amount:
     *            amount that should be refunded to the customer. This can not
     *            be bigger than the amount initially paid by the customer. The scale
     *            must be 2.
     *
     * @return false in case payment session is invalid or not in the correct
     *         state, or amount is incorrect
     */
    public boolean refundPayment(int paymentSessionId, BigDecimal amount);

    /**
     * Retrieve the current PaymentModuleStatus, see the
     * PaymentModuleStatusListener interface
     *
     * @return the current PaymentModuleStatus
     */
    public PaymentModuleStatus getPaymentModuleStatus();

    /**
     * Request the PaymentSessions that have not been finalized yet.
     *
     * @return a List of the paymentSessionIds of all open PaymentSession;
     */
    public List<Integer> getOpenPaymentSessions();

    /**
     * The payment style supported by this payment module
     */
    public PaymentStyle getPaymentStyle();

    /**
     * isRefund supported by this payment module
     */
    public boolean isRefundSupported();

    /**
     * Perform a self-test on the payment system. This should check everything with software/hardware/connection
     * of what is feasible. During the self test the system may become NOT_AVAILABLE.
     * In case the selfTest reports SUCCEEDED the application can assume that the payment module/terminal is working
     * properly.
     */
    public boolean performSelfTest(PaymentCommandResultListener result);

    /**
     * Create a report with detailed aspects of the payment system, including settings, state of relevant
     * variables, versions of components etc. The report is intended for a service engineer to validate
     * to see if the payment module/terminal is configured properly and investigate an error.
     */
    public boolean reportDiagnose(PaymentCommandResultListener result);

    public boolean reset();

}
