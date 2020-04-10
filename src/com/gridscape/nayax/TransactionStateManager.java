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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.abb.evci.payment.PaymentEventListener;

import java.util.Collections;

public class TransactionStateManager {

	/**
	 * class for manipulation of stored open payment sessions.
	 */

	public Map<Integer, Transaction> transactionState = Collections.synchronizedMap(new HashMap<Integer, Transaction>());
	List<Transaction> requestedTransactionForConfirm = Collections.synchronizedList(new ArrayList<Transaction>());

	Integer currentSesssionId;

	boolean settlementOperationRunning = false;

	/**
	 * 
	 * @param sessionId:
	 *            paymentSessionId of requested Transaction.
	 * @return instance of transaction attached with paymentSessionId
	 */
	Transaction getTransactionState(Integer paymentSessionId) {
		return transactionState.get(paymentSessionId);
	}

	/**
	 * to create new transaction with paymentSessionId
	 * 
	 * @param sessionId
	 */
	void setTransaction(Integer paymentSessionId, BigDecimal pre_auth_amount, Integer productCode, PaymentEventListener eventListener) {
		Transaction transaction = new Transaction(paymentSessionId, pre_auth_amount, productCode, eventListener);		
		transactionState.put(paymentSessionId, transaction);
	}

	/**
	 * removes transaction of requested paymentSessionId
	 * 
	 * @param paymentSessionId:
	 *            paymentSessionId of transaction to be removed.
	 */
	void removeTransaction(Integer paymentSessionId) {
		transactionState.remove(paymentSessionId);
	}

	/**
	 * returns paymentSessionId that currently in use.
	 * 
	 * @return
	 */
	public Integer getCurrentSesssionId() {
		return currentSesssionId;
	}

	/**
	 * sets paymentSessionId that is going to use.
	 * 
	 * @param currentSesssionId
	 *            : paymentSessionId
	 */
	public void setCurrentSesssionId(Integer currentSesssionId) {
		this.currentSesssionId = currentSesssionId;
	}
	
	public boolean isInConfirmRequest(Transaction transaction) {
		int id = transaction.getPaymentSessionId();
		boolean result = false;
		for(Transaction tran : requestedTransactionForConfirm) {
			if(tran.getPaymentSessionId() == id) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	
}
