package com.abb.evci.payment;

public interface TransactionInfo {

	
	/**
	 * The unique identifier associated to this transaction. It is up the discretion of the
	 * module to define this. Typically this number is used to report to the outside world
	 * as a reference to the payment made. In case of an non successful payment the transaction id 
	 * maybe null. 
	 */
	public String getTransactionId();
	

    /**
     * Provide the text of the receipt, UTF-8 encoded
     * If the payment terminal provides a receipt, it should be included in the TransactionInfo
     * If not, a text should be created that provides the main information about the transaction, human readable,
     * including the date, time stamp, currency, the amount, the result (succeeded, not accepted, etc.)
     * 
     * @return UTF-8 encoded receipt text or null if not available or not applicable
     */
    public String getReceiptText();

    /**
     * Provide a human readable text describing the result of the payment or command
     * 
     * @return UTF-8 encoded string describing the result of the action (payment or command) or null if not
     * available or not applicable
     */
    public String getResultText();

}
