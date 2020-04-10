/*************************************************************************
 * 
 * Gridscape Solutions, Inc. - CONFIDENTIAL & PROPRIETARY
 * __________________
 * 
 *  Copyright @ 2016-2021 Gridscape Solutions, Inc.
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Gridscape Solutions, Inc.
 * The intellectual and technical concepts contained
 * herein are proprietary to Gridscape Solutions, Inc.
 * and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Gridscape Solutions.
 * 
 * Author: Jayesh, Rahul.
 */
package com.gridscape.nayax;

import com.abb.evci.payment.TransactionInfo;

public class TransactionInfoImpl implements TransactionInfo {
	
	/**
	 * Implementation class for TransactionInfo Interface
	 */
	
	private String transactionId;
	private String receiptText;
	private String resultText;

	
	
	/**
	 * Assigns transaction id. It will assign payment session id as a transaction id.
	 * 
	 * @param receiptNumber:
	 *            receipt number to be assigned
	 */
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	
	/**
	 * receiptTest will have following information
	 * 	1. Terminal serial number
	 *  2. Currency code
	 *  3. Final amount
	 *  4. CCR's transaction id
	 *  5. Status message: 
	 *  	"Payment successful" on successful of confirm payment operation.
	 *  	"Payment Unsuccessful" if confirm payment not got succeeded.
	 * 
	 * @param receiptText:
	 *            String to be assign to receiptText
	 */
	public void setReceiptText(String receiptText) {
		this.receiptText = receiptText;
	}
	
	/**
	 * 
	 * @param resultText:
	 *            "Payment successful" on successful of confirm payment
	 *            operation. 
	 *            "Payment Unsuccessful" if confirm payment not got
	 *            succeeded.
	 */
	public void setResultText(String resultText) {
		this.resultText = resultText;
	}

	@Override
	public String getTransactionId() {
		return transactionId;
	}

	@Override
	public String getReceiptText() {
		return receiptText;
	}

	@Override
	public String getResultText() {
		return resultText;
	}

}
