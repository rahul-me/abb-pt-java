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


public abstract class Marshal {
	
	/**
	 * defines marshal protocol
	 */
	
	protected boolean keepAliveRunning;
	static byte lastCheckPktId;
	protected byte packetRcv[];
	protected byte currency[] = new byte[3];
	public static String currencyCode;	
	protected byte terminalSerial[] = new byte[17];
	public static String terminalSerialNumber;
	protected int txnIdRcv;
	abstract int sendPacketOnSerial(byte writeCommand[], int writeArraySize,  char vendCancel);
	protected SentCcrMessage lastSentCcrMessage;
	protected CcrMessageEvent ccrMessageEvent;
	
	protected char packCounter;	
	protected byte txnIdArray[];
	protected byte keepAliveInterval;
	protected byte lastPacketIdRcv;
	protected char lastPacketIdSent;
	protected byte lastPacketRetryCount;
	
	protected char priceInitialHexLsb;
	protected char priceFinalHexLsb;
	protected char deviceIdMsb;
	protected char deviceIdLsb;

	protected double cstart;
	protected double cend;
	
	public static long countryCode;
	public static byte language;
	
	protected byte[] country = new byte[3];	
	
	static TerminalError terminalError = TerminalError.NONE;
		
	
	enum SentCcrMessage {
		FIRMWARE_INFO, SEND_DISPLAY, MDB_READER_ENABLE, MDB_READER_DISABLE, MDB_VENDREQ, MDB_VENDSUCCESS, MDB_SESSIONCOMPLETE, MDB_CLOSESESSION, DATATRANSFER, KEEP_ALIVE, DISPLAY_STATUS,
	};

	enum CcrMessageEvent {
		NO_ACTION, RESET, CONFIG, BEGIN_SESSION, READER_ENABLED, READER_DISABLED, VEND_REQUEST, VEND_APPROVED, VEND_DENIED, VEND_CANCELED, END_SESSION, CLOSE_SESSION, STATUS_RECEIVED_SUCCESS, STATUS_RECEIVED_FAIL
	};

	enum MarshalActionEvent {
		NO_MARSHAL_ACTION, SEND_READER_ENABLE, SEND_READER_DISABLE, SEND_VEND_REQUEST, SEND_VEND_SUCCESS, SEND_SESSION_COMPLETE, SEND_CLOSE_SESSION
	}
	enum ReceivedPaymentMessageType {
	    NONE,
	    INITIALIZE,
	    SHUTDOWN,
	    INITIATE_CONFIGURATION,
	    INITIATE_CONFIGURATION_SECOND,
	    START_PAYMENT,
	    ABORT_PAYMENT,
	    CONFIRM_PAYMENT,
	    INITIATE_END_OF_DAY,
	    INITIATE_DIAGNOSTICS_SEQUENCE,
	    INITIATE_SW_UPDATE	    
	}
	
	enum PaymentModuleImplStatus {
    	NOT_AVAILABLE,    	 		     
    	PAYING, 	                 	 		
    	AVAILABLE
    }
	
	enum SerialPortConnectionError{
		NONE,
		NO_SUCH_PORT,
		PORT_IN_USE,
		UNSUPPORTED_COMM_OPERATION,
		IO_OPREATION,
		NO_SERIAL_PORTS		
	}
	
	enum TerminalError {
		NONE,
		NO_COMMUNICATION_WITH_CCR,
		NO_SUCH_PORT,
		PORT_IN_USE,
		INITIALIZATION_OF_COMMUNICATION_WITH_CCR_FAIL,
		NO_SERIAL_PORTS,
		INTERNAL_COMM_ERROR,
		NOT_IN_VALID_PAYMENT_MODULE_STATE,
		INVALID_STARTPAYMENT_PARAMETERS,
		INVALID_PAYMENT_SESSION_ID,
		NO_CARD_SWIPED,
		INVALID_CURRENCY_CODE
	}
		
	public Marshal() {

	}

	Marshal(Marshal orig) {

	}
	
	/**
	 * Retrieves messages sent by credit card reader on serial port. Calls
	 * appropriate method based on type of message received.
	 *
	 * @return type of event interpreted by payment module based on
	 *         identification of type of message retrieved from credit card
	 *         reader.
	 */
	abstract CcrMessageEvent checkReader();
	
	/**
	 * calls method based on type of MarsalActionEvent.
	 * 
	 * @param MarshalActionEvent:
	 *            Type of MarshalActionEvent interpreted by payment module It
	 *            represents user specific action need to operate on credit card
	 *            reader
	 */
	abstract void actionOnCommand(MarshalActionEvent actionEvent);
	
	/**
	 * Stores data given in buf
	 * 
	 * @param buf:
	 *            array of bytes, data of which need to be stored.
	 */
	abstract void storeDataRcvd(byte[] buf);
	
	/**
	 * sends reader enable message to credit card reader.
	 * 
	 * @return -1 if message has not been sent successfully, 0 otherwise.
	 */
	abstract int sendReaderEnable();
	
	/**
	 * sends reader disable message to credit card reader.
	 * 
	 * @return -1 if message has not been sent successfully, 0 otherwise.
	 */
	abstract int sendReaderDisable();
	
	/**
	 * sends vend request message to credit card reader.
	 * 
	 * @return -1 if message has not been sent successfully, 0 otherwise.
	 */
	abstract int sendVendRequest();
	
	/**
	 * sends vend success message to credit card reader.
	 * 
	 * @return -1 if message has not been sent successfully, 0 otherwise.
	 */
	abstract int sendVendSuccess();
	
	/**
	 * sends session complete message to credit card reader.
	 * 
	 * @return -1 if message has not been sent successfully, 0 otherwise.
	 */
	abstract int sendSessionComplete();
	
	/**
	 * sends close session message to credit card reader.
	 * 
	 * @return -1 if message has not been sent successfully, 0 otherwise.
	 */
	abstract int sendCloseSession(char vendCancel);
	
	/**
	 * sends transfer data message to credit card reader.
	 * 
	 * @return -1 if message has not been sent successfully, 0 otherwise.
	 */
	abstract int sendTransferData();
	
	/**
	 * sends keep alive message to credit card reader.
	 * 
	 * @return -1 if message has not been sent successfully, 0 otherwise.
	 */
	abstract int sendKeepAlive();
	
	/**
	 * sends firmware information message to credit card reader.
	 * 
	 * @return -1 if message has not been sent successfully, 0 otherwise.
	 */
	abstract int sendFirmwareInfo();
	
	/**
	 * sends display message to credit card reader.
	 * 
	 * @return -1 if message has not been sent successfully, 0 otherwise.
	 */
	abstract int sendDisplay();
	
	/**
	 * sends acknowledge message to credit card reader.
	 * 
	 * @return -1 if message has not been sent successfully, 0 otherwise.
	 */
	abstract int sendAck();
	
	/**
	 * calls method that sends firmware information message to credit card
	 * reader.
	 * called when reset message is received from credit card reader.
	 */
	abstract void receivedReset();
	
	/**
	 * stores terminal specific data. Called when configuration message is
	 * received from credit card reader.
	 */
	abstract void receivedConfig();
	
	/**
	 * take actions upon acknowledgement that is received from credit card
	 * reader for specific message sent by payment module. Called when
	 * acknowledgement is received from credit card reader.
	 */
	abstract void receivedAck();
	
	/**
	 * sends acknowledge in response to begin session message received from
	 * credit card reader.
	 */
	abstract void receivedBeginSession();
	
	/**
	 * sends acknowledge in response to vend approve message received from
	 * credit card reader.
	 */
	abstract void receivedVendApproved();
	
	/**
	 * sends acknowledge and session complete messages in response to vend deny
	 * message received from credit card reader.
	 */
	abstract void receivedVendDenied();
	
	/**
	 * sends acknowledge and session complete messages in response to vend canceled
	 * message received from credit card reader.
	 */
	abstract void receivedVendCancelled();
	
	/**
	 * sends acknowledge in response to end session message received from
	 * credit card reader.
	 */
	abstract void receivedEndSession();
	
	/**
	 * sends acknowledge message and transaction id in transfer data message in response to transfer data
	 * message received from credit card reader.
	 */
	abstract void receivedTransferData();
	
	/**
	 * sends acknowledge in response to display message received from
	 * credit card reader.
	 */
	abstract void receivedDisplayStatus();
	
	/**
	 * sends acknowledge in response to status message received from
	 * credit card reader and calls method that will identify status from the received message.
	 */
	abstract void receivedTransferStatus();
	
	/**
	 * reset keep alive check time
	 */
	abstract void resetKeepAliveTimer();

	

	byte getLastPacketIdRcv() {
		return lastPacketIdRcv;
	}

	void setcStart(double value) {
		cstart = value;
	}

	double getcStart() {
		return cstart;
	}

	void setcEnd(double value) {
		cend = value;
	}

	double getcEnd() {
		return cend;
	}	
}
