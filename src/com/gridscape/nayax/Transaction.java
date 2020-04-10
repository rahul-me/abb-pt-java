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

import java.math.BigDecimal;

import com.abb.evci.payment.PaymentEventListener;
import com.abb.evci.payment.PaymentEventListener.PaymentAuthorizationResult;

public class Transaction {
	
	/**
	 * Stores data required for single payment transaction.
	 */
	
	long trxnId;
	String status;
	BigDecimal amount = null;
	Integer productCode = null;
	BigDecimal finalPrice = null;
	private String trxn_type = null;
	public String getTrxn_type() {
		return trxn_type;
	}



	public void setTrxn_type(String trxn_type) {
		this.trxn_type = trxn_type;
	}

	int paymentSessionId;
	private PaymentEventListener paymentEventListener;
	private TransactionInfoImpl transactionInfo;
	
	private PaymentAuthorizationResult paymentAuthorizationResult = PaymentAuthorizationResult.FAILED;
	
	private int vendsuccessretry = 0;
	
	public Transaction(int paymentSessionId, BigDecimal pre_auth_amount, Integer productCode, PaymentEventListener eventListener){
		this.paymentSessionId = paymentSessionId;
		this.setAmount(pre_auth_amount);
		this.setProductCode(productCode);
		this.setPaymentEventListener(eventListener);
	}
	
	
	
	public int getVendsuccessretry() {
		return vendsuccessretry;
	}



	public void setVendsuccessretry(int vendsuccessretry) {
		this.vendsuccessretry = vendsuccessretry;
	}



	public PaymentAuthorizationResult getPaymentAuthorizationResult() {
		return paymentAuthorizationResult;
	}



	public void setPaymentAuthorizationResult(PaymentAuthorizationResult paymentAuthorizationResult) {
		this.paymentAuthorizationResult = paymentAuthorizationResult;
	}

	public Integer getProductCode() {
		return productCode;
	}
	public void setProductCode(Integer productCode) {
		this.productCode = productCode;
	}
	public long getTrxnId() {
		return trxnId;
	}
	public void setTrxnId(long trxnId) {
		this.trxnId = trxnId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}		
	public BigDecimal getFinalPrice() {
		return finalPrice;
	}
	public void setFinalPrice(BigDecimal finalPrice) {
		this.finalPrice = finalPrice;
	}
	public PaymentEventListener getPaymentEventListener() {
		return paymentEventListener;
	}
	public void setPaymentEventListener(PaymentEventListener paymentEventListener) {
		this.paymentEventListener = paymentEventListener;
	}		
	public TransactionInfoImpl getTransactionInfo() {
		return transactionInfo;
	}

	public void setTransactionInfo(TransactionInfoImpl transactionInfo) {
		this.transactionInfo = transactionInfo;
	}
	
	public String getReceiptDataConfirm(){
		return "\nTransaction Session Id: "+paymentSessionId+"\nFinal Amount: "+finalPrice;
	}
	
	public String getReceiptDataAuthorization(){
		return "\nTransaction Session Id: "+paymentSessionId+"\nPre-Auth Amount: "+amount;
	}
		
	public int getPaymentSessionId() {
		return paymentSessionId;
	}

	public void setPaymentSessionId(int paymentSessionId) {
		this.paymentSessionId = paymentSessionId;
	}

	@Override
	public String toString() {
		return "\n------------------------------------------------------------"
				+ "\nPayment Session Id:: "+this.paymentSessionId+"\nPre-Auth Amount:: "+this.amount.floatValue()/100+"\nProduct Code:: "+this.productCode 
				+ "\nCCR Transaction Id:: "+this.trxnId 
				+ "\n------------------------------------------------------------";
	}
	
}
