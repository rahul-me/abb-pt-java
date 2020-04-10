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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.abb.evci.payment.PaymentCommandResultListener;
import com.abb.evci.payment.PaymentEventListener;
import com.abb.evci.payment.PaymentEventListener.ConfirmationResult;
import com.abb.evci.payment.PaymentEventListener.PaymentAuthorizationResult;
import com.abb.evci.payment.PaymentEventListener.PaymentFailureReason;
import com.abb.evci.payment.PaymentModuleStatusListener;
import com.abb.evci.payment.PaymentModuleStatusListener.PaymentModuleStatus;
import com.abb.evci.payment.TransactionInfo;
import com.gridscape.nayax.Marshal.CcrMessageEvent;
import com.gridscape.nayax.Marshal.MarshalActionEvent;
import com.gridscape.nayax.Marshal.PaymentModuleImplStatus;
import com.gridscape.nayax.Marshal.ReceivedPaymentMessageType;
import com.gridscape.nayax.Marshal.SerialPortConnectionError;
import com.gridscape.nayax.Marshal.TerminalError;
import com.gridscape.nayax.log.LogUtility;

public class ActionExecutor {

	/**
	 * Central controller of a system for communicating with credit card reader.
	 * It takes decision for what need to be performed. Executes operation based
	 * on user choices, manages time constraints
	 */

	SerialCommunicator serialCommunicator = new SerialCommunicator();
	NayaxCardReaderAPI nayaxCardReader;
	Utility utility;
	LogUtility logUtility;
	CcrMessageEvent ccrMessageEventFromMarshal = CcrMessageEvent.NO_ACTION;
	CcrMessageEvent lastCcrMessageEventFromMarshal = CcrMessageEvent.NO_ACTION;
	ReceivedPaymentMessageType receivedPaymentMessage = ReceivedPaymentMessageType.NONE;
	ReceivedPaymentMessageType receivedPaymentMessageInThread = ReceivedPaymentMessageType.NONE;
	MarshalActionEvent marshalActionEvent;
	TransactionStateManager transactionStateManager;
	static PaymentModuleStatusListener paymentModuleStatusListener;

	boolean polling_thread_state = true;
	boolean keepAliveErrorTime = false;
	boolean cardSwipedTimerRunning;
	boolean paymentInProgress;
	boolean activeVendDeniedSession;
	boolean startPollingThreadRunning = true;
	boolean communicationStarted = false;
	boolean outoforder = false;
	boolean approvedbutnoendsession = false;
	boolean shutdowninprocess = false;
	boolean resetonprocess = false;
	static boolean invalidbeginsessionreceived = false;
	boolean initiateoperationrunning = false;
	boolean terminalready = false;

	final byte COMA_SEPARATER = 0x2C;
	final int CODE_NOTIFICATION_INTERVAL = 10000; // in milliseconds
	final int KEEPALIVE_CHECK_INTERVAL = 5000; // in milliseconds
	final int KEEPALIVE_CHECK_TIMEOUT = 10000; // in milliseconds
	final int CONFIRM_OR_ABORT_TIMEOUT = 180000; // in milliseconds
	int CARD_SWIPE_TIMEOUT = 60000; // in milliseconds
	final int COMMUNICATION_CHECK = 50000;// in milliseconds
	final int CONFIRMPAYMENT_CHECK_INTERVAL = 5000;
	final int CONFIRM_REQUEST_TIMEOUT = 30000;
	final int APPROVED_BUT_NO_END_SESSION_TIMEOUT = 30000;
	final int CHECK_BEGIN_SESSION_GRACE_TIME = 10000;
	final int CHECK_SETTLEMENT_TIMEOUT = 15000;
	
	final static int abortAmount = 0;

	double cstartconfirmTimeout;
	double cendconfirmTimeout;
	double cstartcardTimeout;
	double cendcardTimeout;
	double cstartkeepalive;
	double cendkeepalive;
	double cstartscode;
	double cendscode;
	double cstartkeepalivecheck;
	double cendkeepalivecheck;
	double cstartcommunication;
	double cendcommunication;
	double cstartconfirmpaymentcheck;
	double cendconfirmpaymentcheck;
	double cstartbeginbutnoendsession;
	double cendbeginbutnoendsession;
	double cstartapprovedbutnoendsession;
	double cendapprovedbutnoendsession;
	double cstartpayingtimeout;
	double cendpayingtimeout;
	
	SerialPortConnectionError serialPortState;
	Thread threadPoller;
	PaymentModuleStatus paymentModuleStatus;
	PaymentModuleImplStatus paymentModuleImplStatus;
	
	
	
	List<String> commands;	
	
	private final String PAYMENT_RESULT_SUCCEEDED = "PAYMENT_SUCCEEDED";
	private final String ABORT_RESULT_SUCCEEDED = "ABORT_SUCCEEDED";
	private final String PAYMENT_RESULT_FAILED = "PAYMENT_FAILED";
	private final String EMPTY_DETAIL = "";
	private final String EMPTY_CODE = "";
	private final String RESULT_TEXT_VEND_DENY = "VEND_DENIED";
	private final String RESULT_TEXT_PAYMENT_ABORTED = "PAYMENT_ABORTED";
	private final String RESULT_TEXT_PAYMENT_TIMEOUT = "CARD_SWIPE_TIMEOUT";
	
	private final PaymentFailureReason PAYMENT_FAILURE_REASON_NULL = null; 
	private final TransactionInfo TRANSACTION_INFO_NULL = null;
	
	private String terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
		
	public static List<Integer> abortCodeList = new ArrayList<Integer>();
	
	
	public ActionExecutor(PaymentModuleStatusListener listener, TransactionStateManager transactionStateManager, LogUtility logUtility,
			List<String> commands) {
		this.logUtility = logUtility;
		this.transactionStateManager = transactionStateManager;
		this.commands = commands;	
		serialCommunicator.setLogUtility(logUtility);
		utility = new Utility();
		logUtility.setUtility(utility);		
		nayaxCardReader = new NayaxCardReaderAPI(serialCommunicator, utility, transactionStateManager, logUtility);
		paymentModuleStatusListener = listener;
		setAbortList();
	}

	/**
	 * It starts payment module. Must called after PaymentModuleStatusListener
	 * has been registered.
	 * 
	 * Initializes configuration parameters. Connects the serial port and starts
	 * thread that communicate with credit card reader.
	 * 
	 * 
	 * @param keys:
	 *            List of configuration parameter.
	 * @param values:
	 *            List of values to be assigned to their respective
	 *            configuration parameter.
	 * @return: It will return true upon successful initialization otherwise
	 *          false.
	 */
	boolean onInitiate(Map<String,String> map) {
		boolean status = true;				
		if (map.containsKey(PaymentModuleImpl.CARD_SWIPE_TIMEOUT_PARA) && map.get(PaymentModuleImpl.CARD_SWIPE_TIMEOUT_PARA) != null) {
			try {
				CARD_SWIPE_TIMEOUT = 1000 * Integer.parseInt(map.get(PaymentModuleImpl.CARD_SWIPE_TIMEOUT_PARA));
			} catch (NumberFormatException nfe) {
				return false;
			}
		}
		try {			
			serialPortState = serialCommunicator.connect(PaymentModuleImpl.getConfigMap().get(PaymentModuleImpl.SERIAL_PORT_ID_PARA));
			if (serialPortState != SerialPortConnectionError.NONE) {				
				logUtility.showMsg("Caught exception in "+serialPortState.toString());
				switch (serialPortState) {
				case NO_SUCH_PORT:
					Marshal.terminalError = TerminalError.NO_SUCH_PORT;
					terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
					break;
				case PORT_IN_USE:
					Marshal.terminalError = TerminalError.PORT_IN_USE;
					terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
					break;
				case UNSUPPORTED_COMM_OPERATION:
					Marshal.terminalError = TerminalError.INITIALIZATION_OF_COMMUNICATION_WITH_CCR_FAIL;
					terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
					break;
				case NO_SERIAL_PORTS:
					Marshal.terminalError = TerminalError.NO_SERIAL_PORTS;
					terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
					break;
				default:
					Marshal.terminalError = TerminalError.INTERNAL_COMM_ERROR;
					terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
					break;
				}
				status = false;
				return status;
			}
			threadPoller = new Thread() {
				public void run() {
					while (startPollingThreadRunning) {
						startPolling();
					}
				}
			};
			threadPoller.start();
		} catch (Exception e) {
			logUtility.showMsg("Caught exception in onInitiate()"+e.getMessage());
			status = false;
		}
		if (status) {			
			if(paymentModuleStatus != PaymentModuleStatus.NOT_AVAILABLE) {
				paymentModuleStatus = PaymentModuleStatus.NOT_AVAILABLE;
			}
			if(paymentModuleStatusListener!=null) {
				if(paymentModuleStatus != PaymentModuleStatus.NOT_AVAILABLE) {
					paymentModuleStatus = PaymentModuleStatus.NOT_AVAILABLE;
						paymentModuleStatusListener.statusChanged(paymentModuleStatus, "On initiate");
				}
			}
			paymentModuleStatus = PaymentModuleStatus.NOT_AVAILABLE;
			
			if(PaymentModuleImpl.getConfigMap().get(PaymentModuleImpl.CLEAR_OPEN_SESSION_ON_STARTUP).equals("true")) {
				handleClearSessionOnStartup();
			}
			
		}
		return status;
	}

	/**
	 * Starts the payment. Saves payment session with required parameters upon
	 * call in transaction manager.
	 * 
	 * @param currency:
	 *            valid currency code.(example: USD)
	 * @param amount:
	 *            valid amount(not null, greater than zero, not negative)
	 * @param listener:
	 *            Instance of PaymentEventListener
	 * @return: returns payment session id in integer. -1 in case of invalid
	 *          values of method arguments.
	 */
	int onStartPayment(String currency, BigDecimal amount, PaymentEventListener listener) {
		if (paymentModuleStatus != PaymentModuleStatus.READY_FOR_PAYMENT) {
			logUtility.showMsg("Terminal is not in ready for payment state");
			Marshal.terminalError = TerminalError.NOT_IN_VALID_PAYMENT_MODULE_STATE; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
			return -1;
		}		
		if (amount.scale() != 2 || currency == null || amount == null || (Utility.roundUp(amount)) <= 0 || amount.floatValue() > 655.35 || listener == null) {
			logUtility.showMsg("Parameters are not acceptable");
			Marshal.terminalError = TerminalError.INVALID_STARTPAYMENT_PARAMETERS; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());			
			return -1;
		}
		if (!PaymentModuleImpl.getCurrencycodelist().contains(currency)){
			logUtility.showMsg("Currency s not valid");
			Marshal.terminalError = TerminalError.INVALID_STARTPAYMENT_PARAMETERS; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
			return -1;
		}
		
		
		
		if(invalidbeginsessionreceived) {
			nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_SESSION_COMPLETE);
		}
		long starttime = System.currentTimeMillis();		
		while(invalidbeginsessionreceived){
			if(System.currentTimeMillis()> starttime + 3000){
				invalidbeginsessionreceived = false;				
				break;				
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				logUtility.showMsg("Caught exception in onStartPayment() "+e.getMessage());
			}
		}
		
		
		
		
		receivedPaymentMessage = ReceivedPaymentMessageType.START_PAYMENT;
		paymentModuleStatus = PaymentModuleStatus.PAYING;
		
		if(paymentModuleStatusListener != null) {
			paymentModuleStatusListener.statusChanged(paymentModuleStatus,
					"Start Payment Called");
		} 
		
		while(transactionStateManager.settlementOperationRunning) {
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				logUtility.showMsg("Caught exception in onStartPayment() "+e.getMessage());
			}
		}
		int sessionId = utility.getNewSessionId();
		transactionStateManager.setCurrentSesssionId(sessionId);
		transactionStateManager.setTransaction(sessionId, amount, nayaxCardReader.getProductCode(), listener); // changed int cast 25 Aug		
		nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_READER_ENABLE);
		return sessionId;
	}

	/**
	 * Generates the list of open payment session ids.
	 * 
	 * @return: List of open payment session id.
	 */
	public List<Integer> onGetOpenPaymentSessions() {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (Integer integer : transactionStateManager.transactionState.keySet()) {
			Transaction tran = transactionStateManager.transactionState.get(integer);
			if(tran.getPaymentAuthorizationResult() == PaymentAuthorizationResult.APPROVED) {
				list.add(integer);
			}
		}
		return list;
	}
	
	/**
	 * Confirms the payment of payment session using paymentSessionId.
	 * 
	 * @param paymentSessionId:paymentSessionId
	 *            to be confirmed.
	 * @param finalAmount:
	 *            final amount to be confirmed.
	 * @return: true for valid paymentSessionId(one that is in open payment
	 *          session list) and finalAmount(not null, not negative, less than
	 *          pre-auth amount)
	 */
	boolean onConfirmPayment(int paymentSessionId, BigDecimal finalAmount) {
		if (paymentModuleStatus == PaymentModuleStatus.NOT_AVAILABLE || paymentModuleStatus == PaymentModuleStatus.OUT_OF_ORDER) {
			logUtility.showMsg("IN NOT_AVAILABLE or OUT_OF_ORDER state");
			Marshal.terminalError = TerminalError.NOT_IN_VALID_PAYMENT_MODULE_STATE; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
			return false;
		}
		Transaction transaction = transactionStateManager.transactionState.get(paymentSessionId);		
		double l = Utility.roundUp(finalAmount);		
		if (finalAmount.scale() != 2 || transaction == null || (l < 0) || (finalAmount.doubleValue() > transaction.amount.doubleValue())) {			
			Marshal.terminalError = TerminalError.INVALID_PAYMENT_SESSION_ID; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
			return false;
		} 
		if (transactionStateManager.isInConfirmRequest(transaction)) {
			logUtility.showMsg("IN REQUEST");
			Marshal.terminalError = TerminalError.INVALID_PAYMENT_SESSION_ID; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
			return false;
		}
		transaction.setFinalPrice(BigDecimal.valueOf(l));
		TransactionInfoImpl transactionInfo = new TransactionInfoImpl();
		transactionInfo.setTransactionId(String.valueOf(transaction.getTrxnId()));
		transactionInfo.setReceiptText(generateTransactionDetails(transaction));
		transaction.setTransactionInfo(transactionInfo);
		transaction.setTrxn_type("Confirm");
		transactionStateManager.requestedTransactionForConfirm.add(transaction);
		return true;
	}
	
	/**
	 * Aborts requested payment session using it's unique id. Settles requested
	 * payment session with zero amount.
	 * 
	 * @param paymentSessionId:
	 *            payment session id to be aborted.
	 * @return true if requested payment session id is one among open payment
	 *         session id. false otherwise
	 */
	boolean onAbortPayment(int paymentSessionId) {
		if (paymentModuleStatus == PaymentModuleStatus.NOT_AVAILABLE || paymentModuleStatus == PaymentModuleStatus.OUT_OF_ORDER) {
			logUtility.showMsg("Terminal is in NOT_AVAILABLE or OUT_OF_ORDER state");
			Marshal.terminalError = TerminalError.NOT_IN_VALID_PAYMENT_MODULE_STATE; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
			return false;
		}
		Transaction transaction = transactionStateManager.transactionState.get(paymentSessionId);
		if (transaction == null) {
			logUtility.showMsg("TRANSACTION IS NULL");
			Marshal.terminalError = TerminalError.INVALID_PAYMENT_SESSION_ID; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
			return false;
		}
		
		if(transactionStateManager.isInConfirmRequest(transaction)) {
			logUtility.showMsg("IN REQUEST");
			return false;
		}
		
		if(transactionStateManager.getCurrentSesssionId() != null && paymentSessionId == transactionStateManager.getCurrentSesssionId()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				logUtility.showMsg("Caught exception in onAbortPayment() "+e.getMessage());
			}
			if(performAbortIfItIsCurrentSessionId(paymentSessionId)) {
				logUtility.showMsg("ABORT FOR ONGOING PAYMENT");
				return true;
			}
		}
		
		
		transactionStateManager.transactionState.get(paymentSessionId).setFinalPrice(BigDecimal.valueOf(0));
		TransactionInfoImpl transactionInfo = new TransactionInfoImpl();
		transactionInfo.setTransactionId(String.valueOf(transaction.getTrxnId()));
		transactionInfo.setReceiptText(generateTransactionDetails(transaction));
		transaction.setTransactionInfo(transactionInfo);
		transaction.setTrxn_type("Abort");
		transactionStateManager.requestedTransactionForConfirm.add(transaction);
		return true;
	}
	
	/**
	 * executes requested command using required parameters given in argument.
	 * 
	 * @param command:
	 *            valid command to be execute
	 * @param params:
	 *            valid parameters required by requested command.
	 * @param listener:
	 *            instance of PaymentCommandResultListener.
	 * @return
	 */
	boolean onExecuteCommand(String command, List<String> params, PaymentCommandResultListener listener) {
		boolean status = false;
		if (paymentModuleStatus == PaymentModuleStatus.NOT_AVAILABLE || paymentModuleStatus == PaymentModuleStatus.OUT_OF_ORDER) {
			Marshal.terminalError = TerminalError.NOT_IN_VALID_PAYMENT_MODULE_STATE; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
			return false;
		}
		if (commands.contains(command)) {
			if (commands.indexOf(command) == 0) {
				if (params.isEmpty() || params.size() != 2) {
					return false;
				} else {
					try {
						status = onConfirmPayment(Integer.parseInt(params.get(0)),
								new BigDecimal(Float.parseFloat(params.get(1))));
					} catch (NumberFormatException nfe) {
						return false;
					}
				}
			} else if (commands.indexOf(command) == 1) {
				if (params.isEmpty() || params.size() != 1) {
					return false;
				} else {
					try {
						status = onAbortPayment(Integer.parseInt(params.get(0)));
					} catch (NumberFormatException nfe) {
						return false;
					}
				}
			}
		}
		return status;
	}

	/**
	 * Returns state of a payment module at the time of method called.
	 * 
	 * @return current PaymentModuleStatus of payment module
	 */
	PaymentModuleStatus onGetPaymentModuleStatus() {
		return paymentModuleStatus;
	}

	/**
	 * Notifies about the details of current state of payment module.
	 * 
	 * @param result:
	 *            instance of PaymentCommandResultListener.
	 * @return: true if payment terminal is in operable state otherwise return
	 *          false.
	 */
	boolean onReportDiagnose(PaymentCommandResultListener result) {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			logUtility.showMsg("Caught exception in "+e.getMessage());
		}
		String info = "CCR Serial Number: " + Marshal.terminalSerialNumber + "\nCountry Code: " + Marshal.countryCode
				+ "\nCurrency Code: " + Marshal.currencyCode + "\nPayment Module Status: "
				+ paymentModuleStatus + "\nLast Terminal Error: " + Marshal.terminalError + " on "+terminalErrorTime
				+ "\nMax Sessions Supported: "+(NayaxCardReaderAPI.MAX_PRODUCT_NUMBER+1) 
				+ "\nLibrary Version: "+PaymentModuleImpl.LIBRARY_VERSION;
		if(transactionStateManager != null) {
			info += "\nSettlement Operation State: "+transactionStateManager.settlementOperationRunning;
		}
		result.commandFinished("Report_diagnose", ConfirmationResult.SUCCEEDED,	info);
		return true;
	}

	/**
	 * Resets the system. Reinitializes communication with credit card reader.
	 * 
	 * @return false if payment module is not in state to execute reset
	 */

	boolean onReset() {

		if (shutdowninprocess) {
			return false;
		}
		
		if(!checkConfiguration(PaymentModuleImpl.getConfigMap())) {
			logUtility.showMsg("Configuration Parameters are wrong");
			return false;			
		}
						
		resetonprocess = true;

		if (paymentModuleStatus != PaymentModuleStatus.NOT_AVAILABLE) {
			paymentModuleStatus = PaymentModuleStatus.NOT_AVAILABLE;
			if(paymentModuleStatusListener != null) { 
				paymentModuleStatusListener.statusChanged(paymentModuleStatus,
						"On reset call");
			}
			
		}
		
		closeAllOpenSessions();
		
		clearAllSessions();
		
		performReset();
		
		resetonprocess = false;
		return true;
	}
	
	
		
	
	
	public boolean onPerformSelfTest(PaymentCommandResultListener listener) {
		if(paymentModuleStatus != PaymentModuleStatus.READY_FOR_PAYMENT) {
			return false;
		}
		ConfirmationResult confirmationResult = ConfirmationResult.FAILED;
		boolean selftesttimeout = false;
		if (paymentModuleStatus != PaymentModuleStatus.NOT_AVAILABLE) {
			paymentModuleStatus = PaymentModuleStatus.NOT_AVAILABLE;
			if (paymentModuleStatusListener != null) {
				paymentModuleStatusListener.statusChanged(paymentModuleStatus = PaymentModuleStatus.NOT_AVAILABLE,
						"On perform self test call");
			}
		}
		nayaxCardReader.selfTest = true;
		nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_READER_ENABLE);	
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			logUtility.showMsg("Caught exception in "+e.getMessage());
		}
		nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_READER_DISABLE);
		cardSwipedTimerRunning = false;
		double cstartselftest = utility.elapsedTime(true);
		while(nayaxCardReader.selfTest){
			if(System.currentTimeMillis()-cstartselftest > 5000) {
				nayaxCardReader.selfTest = false;
				selftesttimeout = true;
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				logUtility.showMsg("Caught exception in "+e.getMessage());
			}
		}
		if(!selftesttimeout)confirmationResult = ConfirmationResult.SUCCEEDED;
		if(!PaymentModuleImpl.getCurrencycodelist().contains(PaymentModuleImpl.getConfigMap().get(PaymentModuleImpl.CURRENCY_CODE_PARA))) {
			Marshal.terminalError = TerminalError.INVALID_CURRENCY_CODE;
		}
		listener.commandFinished("Self Test", confirmationResult, "");
		if(!selftesttimeout) {
		paymentModuleStatus = PaymentModuleStatus.READY_FOR_PAYMENT;
		if(paymentModuleStatusListener != null) {
			paymentModuleStatusListener.statusChanged(paymentModuleStatus = PaymentModuleStatus.READY_FOR_PAYMENT, "On perform self test call");
		}
		}
		return true;
	}
	
	/**
	 * Shuts down payment terminal. Stops communication with the credit card
	 * reader. Disconnects the serial port.
	 * 
	 * @return true if payment module is able to execute shutdown. false in case
	 *         of payment has not been initialized.
	 */
	boolean onShutdown() {
		if(resetonprocess) {
			return false;
		}
		if(shutdowninprocess) {
			paymentModuleStatus = PaymentModuleStatus.NOT_AVAILABLE;
			if(paymentModuleStatusListener != null) {
				paymentModuleStatusListener.statusChanged(
						paymentModuleStatus = PaymentModuleStatus.NOT_AVAILABLE, "Shutdown in process");
			}			
			return false;
		}
		shutdowninprocess = true;
		if(paymentModuleStatus != PaymentModuleStatus.NOT_AVAILABLE) {
			paymentModuleStatus = PaymentModuleStatus.NOT_AVAILABLE;
			if(paymentModuleStatusListener != null ) {
				paymentModuleStatusListener.statusChanged(
						paymentModuleStatus = PaymentModuleStatus.NOT_AVAILABLE, "Shutdown in process");
			}
		}
		try {
			closeAllOpenSessions();
			startPollingThreadRunning = false;
			polling_thread_state = false;						
			Thread.sleep(3000);
			logUtility.close();
			nayaxCardReader = null;
			serialCommunicator.disconnect();
			paymentModuleStatusListener = null;
		} catch (Exception e) {
			logUtility.showMsg("Caught exception in "+e.getMessage());
			return false;
		}
		shutdowninprocess = false;
		return true;
	}
	
	/**
	 * Communicates with credit card reader. It does following activities. 1.
	 * Checks type of message retrieved from CCR and calls appropriate method.
	 * 2. Checks for user action need to be performed. 3. Checks for
	 * communication liveness every five seconds. 4. Manages cardswipedtimeout.
	 */
	public void startPolling() {
		try {
			while (polling_thread_state == true) {
				initializeTimers(false);
				ccrMessageEventFromMarshal = nayaxCardReader.checkReader();
				if (ccrMessageEventFromMarshal != lastCcrMessageEventFromMarshal) {
					if (ccrMessageEventFromMarshal == CcrMessageEvent.RESET) {
						logUtility.showMsg("Reset has been called");
					} else if (ccrMessageEventFromMarshal == CcrMessageEvent.CONFIG) {				
						onConfig();
					} else if (ccrMessageEventFromMarshal == CcrMessageEvent.BEGIN_SESSION) {
						onBeginSession();				
					} else if (ccrMessageEventFromMarshal == CcrMessageEvent.READER_ENABLED) {			
						onReaderEnabled();
					} else if (ccrMessageEventFromMarshal == CcrMessageEvent.READER_DISABLED) {
						
					} else if (ccrMessageEventFromMarshal == CcrMessageEvent.VEND_APPROVED) {
						onVendApproved();
					} else if (ccrMessageEventFromMarshal == CcrMessageEvent.VEND_DENIED) {
						onVendDenied();					
					} else if (ccrMessageEventFromMarshal == CcrMessageEvent.VEND_CANCELED) {
					
					} else if (ccrMessageEventFromMarshal == CcrMessageEvent.END_SESSION) {
						onEndSession();						
					} else if (ccrMessageEventFromMarshal == CcrMessageEvent.CLOSE_SESSION) {
	
					} else if (ccrMessageEventFromMarshal == CcrMessageEvent.STATUS_RECEIVED_SUCCESS) {
						ccrMessageEventFromMarshal = CcrMessageEvent.NO_ACTION;
			
					} else if (ccrMessageEventFromMarshal == CcrMessageEvent.STATUS_RECEIVED_FAIL) {
						ccrMessageEventFromMarshal = CcrMessageEvent.NO_ACTION;
		
					}
					lastCcrMessageEventFromMarshal = ccrMessageEventFromMarshal;
				}
				
				

				if (nayaxCardReader.getcEnd() > (nayaxCardReader.getcStart() + 800)) {
					if (nayaxCardReader.keepAliveRunning) {
						if (nayaxCardReader.sendKeepAlive() != 0) {

						}
					} else {
						nayaxCardReader.resetKeepAliveTimer();
					}
				}								
								
				
				if (cardSwipedTimerRunning == true) {
					if (cendcardTimeout > (cstartcardTimeout + CARD_SWIPE_TIMEOUT)) {
						onNoCardSwiped();
						cardSwipedTimerRunning = false;
					}
				}
				if (paymentInProgress == true) {
					if (cendconfirmTimeout > (cstartconfirmTimeout + CONFIRM_OR_ABORT_TIMEOUT)) {
						paymentInProgress = false;
					}
				}

				if (!communicationStarted && polling_thread_state) {
					if ((System.currentTimeMillis() - cstartcommunication) > COMMUNICATION_CHECK) {
						onNoCommunication();
						communicationStarted = true;
					}
				}
				if (cendkeepalivecheck > (cstartkeepalivecheck + KEEPALIVE_CHECK_INTERVAL)) {
					keepAliveChecker();
				}
				if (cendscode > (cstartscode + CODE_NOTIFICATION_INTERVAL)) {
					cstartscode = cendscode;
				}
								
				if(approvedbutnoendsession == true) {
					if(cendapprovedbutnoendsession > (cstartapprovedbutnoendsession + APPROVED_BUT_NO_END_SESSION_TIMEOUT)) {						
						approvedbutnoendsession();
					}
				}
				
				
				

				receivedPaymentMessageInThread = receivedPaymentMessage;
				receivedPaymentMessage = ReceivedPaymentMessageType.NONE;


				if (receivedPaymentMessageInThread == ReceivedPaymentMessageType.START_PAYMENT) {

					receivedPaymentMessageInThread = ReceivedPaymentMessageType.NONE;
				} else if (receivedPaymentMessageInThread == ReceivedPaymentMessageType.ABORT_PAYMENT) {

					receivedPaymentMessageInThread = ReceivedPaymentMessageType.NONE;
					paymentInProgress = false;
				} else if (receivedPaymentMessageInThread == ReceivedPaymentMessageType.CONFIRM_PAYMENT) {

					receivedPaymentMessageInThread = ReceivedPaymentMessageType.NONE;
					paymentInProgress = false;
				} else if (receivedPaymentMessageInThread == ReceivedPaymentMessageType.INITIATE_END_OF_DAY) {

					receivedPaymentMessageInThread = ReceivedPaymentMessageType.NONE;
				} else if (receivedPaymentMessageInThread == ReceivedPaymentMessageType.INITIATE_DIAGNOSTICS_SEQUENCE) {

					receivedPaymentMessageInThread = ReceivedPaymentMessageType.NONE;
				} else if (receivedPaymentMessageInThread == ReceivedPaymentMessageType.INITIATE_SW_UPDATE) {

					receivedPaymentMessageInThread = ReceivedPaymentMessageType.NONE;
				}

				if (cendconfirmpaymentcheck > (cstartconfirmpaymentcheck + CONFIRMPAYMENT_CHECK_INTERVAL)) {
					checkForAnyConfirmPayment();
				}

				try {
					Thread.sleep(15);
				} catch (InterruptedException e) {
					logUtility.showMsg("Caught exception in "+e.getMessage());
				}
			}
		} catch (Error e) {
			logUtility.showMsg("Caught exception in "+e.getMessage());
		}
	}
	
	/*
	 * Upon receive
	 * -------------------------------------------------------------------------
	 * 
	 */
	
	/**
	 * executes upon config message is retrieved form credit card reader. It
	 * makes keepAliveChecker enable to start its execution.
	 */
	void onConfig() {
		communicationStarted = true;
		cstartkeepalivecheck = cendkeepalivecheck;
		cstartconfirmpaymentcheck = cendconfirmpaymentcheck;
	}
	
	/**
	 * Makes terminal to start counting elapsed time for calculating card swipe
	 * timeout after reader enable message has been sent.
	 */
	void onReaderEnabled() {		
		cardSwipedTimerRunning = true;	
		cstartcardTimeout = cendcardTimeout;
	}
	
	/**
	 * sends vend request.
	 */
	void onBeginSession() {
		try {
			if (transactionStateManager.getCurrentSesssionId() != null && transactionStateManager.transactionState
					.get(transactionStateManager.getCurrentSesssionId()) != null) {
				cardSwipedTimerRunning = false;
				paymentInProgress = true;
				cstartconfirmTimeout = cendconfirmTimeout;
				nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_VEND_REQUEST);
				cstartconfirmTimeout = cendconfirmTimeout;				
			} else {
				invalidbeginsessionreceived = true;
				nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_SESSION_COMPLETE);
			}
		} catch (Exception e) {
			logUtility.showMsg("Caught exception in "+e.getMessage());
		}
	}

	/**
	 * sends vend success message to credit card reader.Notifies POS Terminal
	 * about vend approval.
	 * 
	 */
	void onVendApproved() {
		try {
			if (transactionStateManager.getCurrentSesssionId() != null) {				
				approvedbutnoendsession = true;
				cstartapprovedbutnoendsession = cendapprovedbutnoendsession;				
				Transaction transaction = transactionStateManager.transactionState
						.get(transactionStateManager.currentSesssionId);						
				if (transaction != null) {

					transaction.setPaymentAuthorizationResult(PaymentAuthorizationResult.APPROVED);			
					transaction.getPaymentEventListener().authorizationResult(transaction.paymentSessionId,
							PaymentAuthorizationResult.APPROVED, EMPTY_CODE, PAYMENT_FAILURE_REASON_NULL,
							TRANSACTION_INFO_NULL, EMPTY_DETAIL);					
					transaction.setVendsuccessretry(transaction.getVendsuccessretry() + 1);					
					nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_VEND_SUCCESS);					
				}
			}
		} catch (Exception e) {
			logUtility.showMsg("Caught exception in onVendApproved()"+"Caught exception in "+e.getMessage());
		}
	}

	/**
	 * Notifies POS Terminal about vend denial. Removes saved payment session.
	 */
	void onVendDenied() {
		try {
			if (transactionStateManager.getCurrentSesssionId() != null) {

				Transaction transaction = transactionStateManager.transactionState
						.get(transactionStateManager.currentSesssionId);

				if (transaction != null) {				
					transaction.setPaymentAuthorizationResult(PaymentAuthorizationResult.FAILED);
					transaction.getPaymentEventListener().authorizationResult(transactionStateManager.currentSesssionId,
							PaymentAuthorizationResult.FAILED, EMPTY_CODE,
							PaymentFailureReason.NOT_ACCEPTED, generateTransactionInfo(transaction, RESULT_TEXT_VEND_DENY),
							generateTransactionDetails(transaction));
					transactionStateManager.transactionState.remove(transactionStateManager.currentSesssionId);
				}
			}
		} catch (Exception e) {
			logUtility.showMsg("Caught exception in onVendDenied() "+"Caught exception in "+e.getMessage());
		}
	}

	/**
	 * Sends reader disable message to credit card reader.
	 */
	void onEndSession() {		
		try {
			if (transactionStateManager.getCurrentSesssionId() != null) {	
				nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_READER_DISABLE);
				
				approvedbutnoendsession = false;
				
				if (paymentModuleStatus != PaymentModuleStatus.READY_FOR_PAYMENT) {
					paymentModuleStatus = PaymentModuleStatus.READY_FOR_PAYMENT;
					if(paymentModuleStatusListener != null) {
						paymentModuleStatusListener.statusChanged(
								paymentModuleStatus = PaymentModuleStatus.READY_FOR_PAYMENT, "End Session Received");
					}					
				}
				
				paymentModuleImplStatus = PaymentModuleImplStatus.AVAILABLE;
				transactionStateManager.setCurrentSesssionId(null);

			} else {
				nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_READER_DISABLE);
				invalidbeginsessionreceived = false;
				if(paymentModuleStatus != PaymentModuleStatus.READY_FOR_PAYMENT) {
					paymentModuleStatus = PaymentModuleStatus.READY_FOR_PAYMENT;
					if (paymentModuleStatusListener != null) {					
						paymentModuleStatusListener.statusChanged(paymentModuleStatus, "End Session");
					}
				}
				
			}
		} catch (Exception e) {
			logUtility.showMsg("Caught exception in onEndSession() "+"Caught exception in "+e.getMessage());
		}
	}
	


	/*
	 * custom methods
	 * -------------------------------------------------------------------------
	 * 
	 */
	
	/**
	 * Manages time constraints.
	 * 
	 * @param firstTime:
	 *            true when it calls first time.
	 */
	void initializeTimers(boolean firstTime) {
		if (firstTime == true) {
			cstartscode = cstartkeepalivecheck = cstartconfirmTimeout = cstartcardTimeout = cstartkeepalive = cstartcommunication = cstartconfirmpaymentcheck = cstartapprovedbutnoendsession = cstartpayingtimeout = utility
					.elapsedTime(true);
			nayaxCardReader.setcStart(cstartscode);
		} else {
			cendscode = cendkeepalivecheck = cendconfirmTimeout = cendcardTimeout = cendkeepalive = cendcommunication = cendconfirmpaymentcheck = cendapprovedbutnoendsession = cendpayingtimeout =  utility
					.elapsedTime(false);
			nayaxCardReader.setcEnd(cendscode);
		}
	}
	
	/**
	 * Checks communication health. Identifies communication loss. Notifies when
	 * terminal is operable.
	 */
	void keepAliveChecker() {
		if (Marshal.lastCheckPktId == nayaxCardReader.getLastPacketIdRcv()) {
			if (keepAliveErrorTime == true && !outoforder) {
				onTemporaryFailure();
				cstartkeepalive = cendkeepalive;
				keepAliveErrorTime = false;
			}
		} else {
			if (keepAliveErrorTime != true) {
				if (keepAliveErrorTime == false)
					cstartkeepalive = cendkeepalive;
				onTerminalOperational();
				if (outoforder)
					outoforder = false;
				keepAliveErrorTime = true;
			}
			Marshal.lastCheckPktId = nayaxCardReader.getLastPacketIdRcv();
		}
		if (keepAliveErrorTime == false && !outoforder) {
			if (cendkeepalive > (cstartkeepalive + KEEPALIVE_CHECK_TIMEOUT)) {
				onPermanentFailure();
				outoforder = true;
				// keepAliveErrorTime = true;
				nayaxCardReader.keepAliveRunning = true;
				cstartkeepalive = cendkeepalive;
			}
		}
		cstartkeepalivecheck = cendkeepalivecheck;
	}
	/**
	 * called when payment module finds no communication with credit card reader
	 */
	void onTemporaryFailure() {
		terminalready = false;
		if (paymentModuleStatus != PaymentModuleStatus.NOT_AVAILABLE) {
			paymentModuleStatus = PaymentModuleStatus.NOT_AVAILABLE;
			if (paymentModuleStatusListener != null) {
				paymentModuleStatusListener.statusChanged(paymentModuleStatus, "Temp faliure");
			}
		}
		Marshal.terminalError = TerminalError.NO_COMMUNICATION_WITH_CCR;
		terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
	}

	/**
	 * Notifies when credit card reader is in state where payment can be
	 * started. called when payment module finds credit card reader to be
	 * operable
	 */
	void onTerminalOperational() {
		terminalready = true;
		if (initiateoperationrunning) {
			initiateoperationrunning = false;
		} else {
			paymentModuleStatus = PaymentModuleStatus.READY_FOR_PAYMENT;
			if (paymentModuleStatusListener != null) {
				nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_READER_DISABLE);
				paymentModuleStatusListener.statusChanged(paymentModuleStatus, "On Terminal Operation");
			}
		}

	}

	/**
	 * Changes payment module state to out of order when payment module finds no
	 * communication with credit card reader for 3 minutes.
	 */
	void onPermanentFailure() {
		terminalready = false;
		paymentModuleStatus = PaymentModuleStatus.OUT_OF_ORDER;
		if (paymentModuleStatusListener != null) {			
			paymentModuleStatusListener.statusChanged(paymentModuleStatus, "Permanent Failure");			
		}
		Marshal.terminalError = TerminalError.NO_COMMUNICATION_WITH_CCR; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
	}
	
	/**
	 * notifies when payment module finds no communication with credit card
	 * reader after payment module is first initialized.
	 */
	void onNoCommunication() {
		if (paymentModuleStatus != PaymentModuleStatus.OUT_OF_ORDER) {
			paymentModuleStatus = PaymentModuleStatus.OUT_OF_ORDER;			
		}
		if (paymentModuleStatusListener != null) {
			if (paymentModuleStatus != PaymentModuleStatus.OUT_OF_ORDER) {
				paymentModuleStatus = PaymentModuleStatus.OUT_OF_ORDER;
				paymentModuleStatusListener.statusChanged(paymentModuleStatus,
						"No Communication With Reader after system initiate call");
			}
			Marshal.terminalError = TerminalError.NO_COMMUNICATION_WITH_CCR; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
		}
	}
	
	/**
	 * Checks for requested call for confirm payments and if available settles those payment sessions.
	 */
	void checkForAnyConfirmPayment() {
		if (!transactionStateManager.settlementOperationRunning) {
			if (!transactionStateManager.requestedTransactionForConfirm.isEmpty()) {
				if (paymentModuleStatus != PaymentModuleStatus.PAYING) {
					transactionStateManager.settlementOperationRunning = true;
					logUtility.showMsg("SETTLEMET_START");
					onPaymentFinished();
				}
			}
		}
		cstartconfirmpaymentcheck = cendconfirmpaymentcheck;
	}
	/**
	 * sends close session message to credit card reader
	 */
	void onPaymentFinished() {
		
		Transaction tran = transactionStateManager.requestedTransactionForConfirm.get(0);
		
		if(transactionStateManager.getTransactionState(tran.getPaymentSessionId()) == null) {
			
			tran.getPaymentEventListener().paymentConfirmation(tran.getPaymentSessionId(), ConfirmationResult.FAILED,
					null, PAYMENT_RESULT_FAILED);
			
			transactionStateManager.requestedTransactionForConfirm.remove(0);
			transactionStateManager.settlementOperationRunning = false;
			logUtility.showMsg("SETTLEMENT_END");
			return;
		}
		
		nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_CLOSE_SESSION);
		
		TransactionInfoImpl transactionInfo = tran.getTransactionInfo();		
		
		
		if (tran.getTrxn_type() == "Confirm") {
			transactionInfo.setTransactionId(String.valueOf(tran.getTrxnId()));
			transactionInfo.setReceiptText(transactionInfo.getReceiptText() + "\nStatus Message: "
					+ PAYMENT_RESULT_SUCCEEDED + "\nFinal Amount: " + tran.getFinalPrice());
			transactionInfo.setResultText(PAYMENT_RESULT_SUCCEEDED);
			tran.getPaymentEventListener().paymentConfirmation(tran.getPaymentSessionId(), ConfirmationResult.SUCCEEDED,
					transactionInfo, PAYMENT_RESULT_SUCCEEDED);
		} else {
			transactionInfo.setTransactionId(String.valueOf(tran.getTrxnId()));
			transactionInfo.setReceiptText(transactionInfo.getReceiptText() + "\nStatus Message: "
					+ ABORT_RESULT_SUCCEEDED);
			transactionInfo.setResultText(ABORT_RESULT_SUCCEEDED);
			tran.getPaymentEventListener().paymentConfirmation(tran.getPaymentSessionId(), ConfirmationResult.ABORTED,
					transactionInfo, RESULT_TEXT_PAYMENT_ABORTED);
		}

		transactionStateManager.transactionState.remove(tran.getPaymentSessionId());
		transactionStateManager.requestedTransactionForConfirm.remove(0);
		transactionStateManager.settlementOperationRunning = false;
		logUtility.showMsg("SETTLEMENT_END");
	}
	
	/**
	 * sends reader disable command to credit card reader. Called when user has
	 * not swiped a card within one minute.
	 */
	void onNoCardSwiped() {
		if (transactionStateManager.getCurrentSesssionId() != null) {
			nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_READER_DISABLE);
			
			Transaction transaction = transactionStateManager.transactionState
					.get(transactionStateManager.currentSesssionId);
			transaction.getPaymentEventListener().authorizationResult(transaction.paymentSessionId,
					PaymentAuthorizationResult.FAILED, EMPTY_CODE, PaymentFailureReason.TIMEOUT, generateTransactionInfo(transaction, RESULT_TEXT_PAYMENT_TIMEOUT),
					generateTransactionDetails(transaction));
			
			transactionStateManager.transactionState.remove(transactionStateManager.currentSesssionId);			
			transactionStateManager.setCurrentSesssionId(null);
			checkForInvalidBeginSessionReceived();					
		}
	}
		
	/**
	 * sends vend success to confirm vend approval.
	 */
	private void approvedbutnoendsession() {
		if (transactionStateManager.getCurrentSesssionId() != null) {
			Transaction transaction = transactionStateManager.transactionState
					.get(transactionStateManager.currentSesssionId);
			if (transaction != null) {
				if (transaction.getVendsuccessretry() < 3) {					
					transaction.setVendsuccessretry(transaction.getVendsuccessretry() + 1);
					nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_VEND_SUCCESS);
				} else {					
					nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_READER_DISABLE);
					transactionStateManager.transactionState.remove(transactionStateManager.currentSesssionId);					
					transactionStateManager.setCurrentSesssionId(null);
					approvedbutnoendsession = false;
					performReset();
				}
			}
		}
	}		
	
	/**
	 * Re-initiates communication with CCR.
	 */
	private void performReset() {
		polling_thread_state = false;
		keepAliveErrorTime = false;
		communicationStarted = false;		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {			
			logUtility.showMsg("Caught exception in performReset() "+e.getMessage());
		}
		serialCommunicator.disconnect();
		serialPortState = serialCommunicator.connect(PaymentModuleImpl.getConfigMap().get(PaymentModuleImpl.SERIAL_PORT_ID_PARA));
		
		if (serialPortState != SerialPortConnectionError.NONE) {
			logUtility.showMsg("Caught exception in serialPortState "+serialPortState.toString());
			switch (serialPortState) {
			case NO_SUCH_PORT:
				Marshal.terminalError = Marshal.TerminalError.NO_SUCH_PORT; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
				break;
			case PORT_IN_USE:
				Marshal.terminalError = Marshal.TerminalError.PORT_IN_USE; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
				break;
			case UNSUPPORTED_COMM_OPERATION:
				Marshal.terminalError = Marshal.TerminalError.INITIALIZATION_OF_COMMUNICATION_WITH_CCR_FAIL; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
				break;
			case NO_SERIAL_PORTS:
				Marshal.terminalError = Marshal.TerminalError.NO_SERIAL_PORTS; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
				break;
			default:
				Marshal.terminalError = Marshal.TerminalError.INTERNAL_COMM_ERROR; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
				break;
			}		
		}
		if(cardSwipedTimerRunning){
			cardSwipedTimerRunning = false;
		}
		if(paymentInProgress) {
			paymentInProgress = false;
		}		
		if(approvedbutnoendsession) {
			approvedbutnoendsession = false;
		}		
		if (shutdowninprocess) {
			shutdowninprocess = false;
		}
		if (initiateoperationrunning) {
			initiateoperationrunning = false;
		}
		if(terminalready) {
			terminalready = false;
		}
		cstartcommunication = utility.elapsedTime(true);
		polling_thread_state = true;
	}
	
	/**
	 * Aborts the current payment session.
	 * 
	 * @param paymentSessionId
	 *            payment session id to abort
	 * 
	 * @return boolean value true if payment session is aborted successfully.
	 */
	private boolean performAbortIfItIsCurrentSessionId(int paymentSessionId) {
		if (cardSwipedTimerRunning && transactionStateManager.getCurrentSesssionId() != null
				&& paymentSessionId == transactionStateManager.getCurrentSesssionId()) {
			
			nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_READER_DISABLE);
			
			Transaction transaction = transactionStateManager.transactionState
					.get(transactionStateManager.currentSesssionId);
			transaction.getPaymentEventListener().authorizationResult(transaction.paymentSessionId,
					PaymentAuthorizationResult.ABORTED, EMPTY_CODE, PAYMENT_FAILURE_REASON_NULL, generateTransactionInfo(transaction, RESULT_TEXT_PAYMENT_ABORTED),
					generateTransactionDetails(transaction));

			transaction.getPaymentEventListener().paymentConfirmation(paymentSessionId, ConfirmationResult.ABORTED, null,
					RESULT_TEXT_PAYMENT_ABORTED);

			

			transactionStateManager.transactionState.remove(transactionStateManager.currentSesssionId);
			transactionStateManager.setCurrentSesssionId(null);
			cardSwipedTimerRunning = false;
			checkForInvalidBeginSessionReceived();						
			return true;
		} else {
			return false;
		}

	}
	
	/**
	 * Checks if library received any invalid begin session message from the
	 * CCR.
	 */
	private synchronized void checkForInvalidBeginSessionReceived() {
		
		Thread beginCheck = new Thread() {
			public void run() {
				boolean status = false;
				long startTime = System.currentTimeMillis();
				while(System.currentTimeMillis() < startTime + CHECK_BEGIN_SESSION_GRACE_TIME) {
					if(invalidbeginsessionreceived) {				
						status = true;
						break;
					}
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						logUtility.showMsg("Caught exception in checkForInvalidBeginSessionReceived() "+"Caught exception in "+e.getMessage());
					}
				}
				if(!status) {					
					invalidbeginsessionreceived = false;
					paymentModuleStatus = PaymentModuleStatus.READY_FOR_PAYMENT;
					if (paymentModuleStatusListener != null) {
						paymentModuleStatusListener.statusChanged(paymentModuleStatus, "READY_FOR_PAYMENT");
					}
				}
			}
		};
		beginCheck.start();
		
	}
	
	/**
	 * Generates instnace of TransactionInfo having transaction id, receipt text
	 * and result text of appropriate action.
	 * 
	 * @param transaction
	 *            whose information need to be generated
	 * @param resultText
	 *            text message to be displayed in resultText.
	 * @return
	 */
	private TransactionInfo generateTransactionInfo(Transaction transaction, String resultText) {
		TransactionInfoImpl transactionInfo = new TransactionInfoImpl();
		transactionInfo.setTransactionId(String.valueOf(transaction.getPaymentSessionId()));
		transactionInfo.setReceiptText(generateTransactionDetails(transaction));
		transactionInfo.setResultText(resultText);
		return transactionInfo;
	}
	
	/**
	 * Generates details of transaction
	 * 
	 * @param transaction
	 *            instance of transaction whose detail need to be generated.
	 * @return
	 */
	private String generateTransactionDetails(Transaction transaction) {
		String detail = "";
		detail += "Terminal Serial Number: " + Marshal.terminalSerialNumber + "\nCurrency Code: " + PaymentModuleImpl.getConfigMap().get(PaymentModuleImpl.CURRENCY_CODE_PARA)
				+ "\nPayment Session Id: " + transaction.getPaymentSessionId() + "\nPre-Auth Amount: "
				+ (transaction.getAmount().floatValue()) + "\nProduct Code: " + transaction.getProductCode()
				+ "\nTransaction Time: "+PaymentModuleImpl.dateFormat.format(new Date());

		if (transaction.getTrxnId() != 0) {
			detail += "\nCCR Transaction Id: " + transaction.getTrxnId();
		}
		return detail;
	}
	
	/**
	 * Checks configuration parameter key string and its value.
	 * 
	 * @param map
	 *            configuration map
	 * @return
	 */
	private boolean checkConfiguration(Map<String, String> map) {
		Set<String> keys = map.keySet();
		for(String key : keys) {
			if(!PaymentModuleImpl.getParameters().contains(key)) {
				return false;
			}
		}
		
		if (map.containsKey(PaymentModuleImpl.CARD_SWIPE_TIMEOUT_PARA) && map.get(PaymentModuleImpl.CARD_SWIPE_TIMEOUT_PARA) != null) {
			try {
				CARD_SWIPE_TIMEOUT = 1000 * Integer.parseInt(map.get(PaymentModuleImpl.CARD_SWIPE_TIMEOUT_PARA));
			} catch (NumberFormatException nfe) {
				return false;
			}
		}
		
		if (!map.containsKey(PaymentModuleImpl.SERIAL_PORT_ID_PARA) || map.get(PaymentModuleImpl.SERIAL_PORT_ID_PARA) == null || !map.containsKey(PaymentModuleImpl.CURRENCY_CODE_PARA)
				|| !PaymentModuleImpl.getCurrencycodelist().contains(map.get(PaymentModuleImpl.CURRENCY_CODE_PARA))) {
			return false;
		}		
		return true;
	}
	
	/**
	 * Initialize map having codes up to maximum limited.
	 */
	public static void setAbortList(){
		abortCodeList = new ArrayList<Integer>();
		for(int i = 0; i <= NayaxCardReaderAPI.MAX_PRODUCT_NUMBER; i++) {
			abortCodeList.add(i);
		}
	}
	
	/**
	 * Forcefully close all the session.
	 */
	private void clearAllSessions() {		
		try {
			nayaxCardReader.setClearAllSession(true);			
			List<Integer> codes = abortCodeList;
			for (int code : codes) {
				logUtility.showMsg("For "+code);
				nayaxCardReader.setSettleStatus(true);
				nayaxCardReader.setForceAbortCode(code);
				nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_CLOSE_SESSION);
				logUtility.showMsg("abort sent");
				long starttime = System.currentTimeMillis();
				while(nayaxCardReader.isSettleStatus()){
					Thread.sleep(1000);
					if(System.currentTimeMillis() > starttime + CHECK_SETTLEMENT_TIMEOUT) {
						nayaxCardReader.setSettleStatus(false);
						logUtility.showMsg("Timeout for settlement status wait");
						break;
					}
				}
				Thread.sleep(500);
			}			
			nayaxCardReader.setClearAllSession(false);
		} catch(InterruptedException e) {
			logUtility.showMsg("Caught exception in clearAllSessions() "+"Caught exception in "+e.getMessage());
		} catch (Exception e) {
			logUtility.showMsg("Caught exception in "+e.getMessage());
		}
		try {
			Thread.sleep(5000);
		} catch(Exception e) {
			logUtility.showMsg("Caught exception in "+e.getMessage());
		}
	}
	
	/**
	 * Aborts all open sessions.
	 */
	private void closeAllOpenSessions() {
		if (paymentModuleStatus != PaymentModuleStatus.OUT_OF_ORDER
				|| paymentModuleStatus != PaymentModuleStatus.NOT_AVAILABLE) {
			List<Integer> list = onGetOpenPaymentSessions();
			if (!list.isEmpty()) {
				for (Integer paymentSessionId : list) {
					Transaction transaction = transactionStateManager.transactionState.get(paymentSessionId);
					transactionStateManager.transactionState.get(paymentSessionId)
							.setFinalPrice(BigDecimal.valueOf(0));
					TransactionInfoImpl transactionInfo = new TransactionInfoImpl();
					transactionInfo.setTransactionId(String.valueOf(paymentSessionId));
					transactionInfo.setReceiptText("Terminal Serial Number: " + Marshal.terminalSerialNumber
							+ "\nCurrency Code: " + Marshal.currencyCode + "\nPayment Session Id: "
							+ transaction.getPaymentSessionId() + "\nFinal Amount: "
							+ (transaction.getFinalPrice().doubleValue()) + "\nCCR Transaction Id: "
							+ transaction.getTrxnId());
					transaction.setTransactionInfo(transactionInfo);
					transactionStateManager.requestedTransactionForConfirm.add(transaction);
				}
			}
			while (!transactionStateManager.requestedTransactionForConfirm.isEmpty()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					logUtility.showMsg("Caught exception in "+e.getMessage());
				}
			}
			try {
				Thread.sleep(5000);
			} catch(InterruptedException e) {
				logUtility.showMsg("Caught exception in "+e.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void initiateHandShakeAgain() {
		polling_thread_state = false;
		keepAliveErrorTime = false;
		communicationStarted = false;		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {			
			logUtility.showMsg("Caught exception in "+e.getMessage());
		}
		serialCommunicator.disconnect();
		serialPortState = serialCommunicator.connect(PaymentModuleImpl.getConfigMap().get(PaymentModuleImpl.SERIAL_PORT_ID_PARA));
		
		if (serialPortState != SerialPortConnectionError.NONE) {
			logUtility.showMsg(serialPortState.toString());
			switch (serialPortState) {
			case NO_SUCH_PORT:
				Marshal.terminalError = Marshal.TerminalError.NO_SUCH_PORT; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
				break;
			case PORT_IN_USE:
				Marshal.terminalError = Marshal.TerminalError.PORT_IN_USE; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
				break;
			case UNSUPPORTED_COMM_OPERATION:
				Marshal.terminalError = Marshal.TerminalError.INITIALIZATION_OF_COMMUNICATION_WITH_CCR_FAIL; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
				break;
			case NO_SERIAL_PORTS:
				Marshal.terminalError = Marshal.TerminalError.NO_SERIAL_PORTS; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
				break;
			default:
				Marshal.terminalError = Marshal.TerminalError.INTERNAL_COMM_ERROR; terminalErrorTime = PaymentModuleImpl.dateFormat.format(new Date());
				break;
			}		
		}		
		cstartcommunication = utility.elapsedTime(true);
		polling_thread_state = true;
	}
	
	private void handleClearSessionOnStartup() {
		initiateoperationrunning = true;
		while(!terminalready) {
			if(paymentModuleStatus == PaymentModuleStatus.OUT_OF_ORDER) {
				initiateoperationrunning = false;
				break;
			}
			try {
				Thread.sleep(200);
			} catch(InterruptedException e) {
				logUtility.showMsg("Caught exception in "+e.getMessage()+ "On Wait for terminal ready");
			}
		}
		if(terminalready) {
			clearAllSessions();
			Thread t = new Thread() {
				public void run(){
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					 logUtility.showMsg("Caught exception in "+e.getMessage());
					}
					paymentModuleStatus = PaymentModuleStatus.READY_FOR_PAYMENT;
					if (paymentModuleStatusListener != null) {
						nayaxCardReader.actionOnCommand(MarshalActionEvent.SEND_READER_DISABLE);
						paymentModuleStatusListener.statusChanged(paymentModuleStatus, "On Terminal Operation");
					}
				}
			};
			t.start();
			
			//initiateHandShakeAgain();
		}
		initiateoperationrunning = false;
	}
	
}