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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.List;

import javax.management.RuntimeErrorException;

import com.gridscape.nayax.log.LogUtility;

public class NayaxCardReaderAPI extends Marshal {

	/**
	 * Implementation class for Marshal. Implements marshal protocol to
	 * communicate with credit card reader. Manipulates messages according to
	 * marshal protocol.
	 */

	int transferDataReceive = 0;
	int transferDataByApp = 0;
	int beginSessionRecived = 0;
	int sendVendRequest = 0;

	byte ack[] = { 0x0A, 0x00, 0x00, 0x01, 0x01, 0x7C, 0x00, 0x35, 0x00, 0x00, 0x00, 0x00 };
	byte firmwareInfo[] = { 0x4B, 0x00, 0x00, 0x00, 0x02, 0x7C, 0x00, 0x00, 0x05, 0x00, 0x02, 0x04, 0x02, 0x40, 0x01,
			0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79,
			0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F, (byte) 0x80, (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84,
			(byte) 0x85, (byte) 0x86, (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x8A, (byte) 0x8B, (byte) 0x8C,
			(byte) 0x8D, (byte) 0x8E, (byte) 0x8F, (byte) 0x90, (byte) 0x91, (byte) 0x92, (byte) 0x93, (byte) 0x94,
			(byte) 0x95, (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x9A, (byte) 0x9B, (byte) 0x9C,
			(byte) 0x9D, (byte) 0x9E, (byte) 0x9F, (byte) 0xA0, (byte) 0xA1, (byte) 0xA2, (byte) 0xA3, 0x00, 0x00 };
	byte readerEnable[] = { 0x0B, 0x00, 0x01, 0x01, 0x01, 0x7C, 0x00, 0x00, (byte) 0x80, 0x14, 0x01, 0x00, 0x00 };
	byte readerDisable[] = { 0x0B, 0x00, 0x01, 0x01, 0x01, 0x7C, 0x00, 0x00, (byte) 0x80, 0x14, 0x00, 0x00, 0x00 };
	byte vendReq[] = { 0x0F, 0x00, 0x01, 0x0A, 0x01, 0x7C, 0x00, 0x00, (byte) 0x80, 0x13, 0x00, 0x28, 0x00, 0x00, 0x00,
			0x00, 0x00 };
	byte vendSuccess[] = { 0x0D, 0x00, 0x01, 0x0B, 0x01, 0x7C, 0x00, 0x00, (byte) 0x80, 0x13, 0x02, 0x00, 0x00, 0x00,
			0x00 };
	byte vendSessionComplete[] = { 0x0B, 0x00, 0x01, 0x10, 0x01, 0x7C, 0x00, 0x00, (byte) 0x80, 0x13, 0x04, 0x00,
			0x00 };
	byte vendCloseSession[] = { 0x10, 0x00, 0x01, 0x16, 0x01, 0x7C, 0x00, 0x00, (byte) 0x80, 0x13, (byte) 0x80, 0x00,
			0x28, 0x00, 0x00, 0x00, 0x00, 0x00 };
	byte dataTransfer[] = { 0x15, 0x00, 0x01, 0x0C, 0x01, 0x7C, 0x00, 0x00, 0x0A, 0x01, 0x0A, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
	byte keepAlive[] = { 0x09, 0x00, 0x01, 0x06, 0x01, 0x7C, 0x00, 0x00, 0x07, 0x00, 0x00 };
	char displayMsg[] = { 0x2A, 0x00, 0x01, 0x02, 0x01, 0x7C, 0x00, 0x35, 0x08, 0x50, 0x72, 0x69, 0x63, 0x65, 0x20,
			0x3D, 0x20, 0x74, 0x20, 0x20, 0x61, 0x72, 0x64, 0x20, 0x20, 0x20, 0x20, 0x4D, 0x61, 0x72, 0x73, 0x68, 0x61,
			0x6C, 0x6C, 0x20, 0x43, 0x4F, 0x4D, 0x20, 0x20, 0x00, 0x00, 0x00 };

	byte[] ccrEventAck = { 0x00 };
	byte[] ccrEventCommand = { 0x01, 0x06, 0x0A, (byte) 0x80 };

	final byte MARSHAL_ACK = 0x00;
	final byte MARSHAL_RESET = 0x01;
	final byte MARSHAL_FWINFO = 0x05;
	final byte MARSHAL_CONFIG = 0x06;
	final byte MARSHAL_KEEPALIVE = 0x07;
	final byte MARSHAL_DISPLAYMSG = 0x08;
	final byte MARSHAL_DISPLAYSTATUS = 0x09;
	final byte MARSHAL_TRANSFER_DATA = 0x0A;
	final byte MARSHAL_TRANSFER_STATUS = 0x0B;
	final byte MDB_COMMAND = (byte) 0x80;
	final byte MDB_BEGIN_SESSION = 0x03;
	final byte MDB_SESSION_COMPLETE = 0x04;
	final byte MDB_END_SESSION = 0x07;
	final byte MDB_CLOSE_SESSION = (byte) 0x80;
	byte destAddress = (byte) 0xff;
	final byte MDB_VEND = 0x13;
	final byte MDB_VEND_REQUEST = 0x00;
	final byte MDB_VEND_CANCEL = 0x01;
	final byte MDB_VEND_SUCCESS = 0x02;
	final byte MDB_VEND_APPROVE = 0x05;
	final byte MDB_VEND_DENY = 0x06;

	final short UTILS_SEED_CCITT = 0;
	final byte MDB_READER = 0x14;
	final byte MDB_READER_ENABLE = 0x01;
	final byte MDB_READER_DISABLE = 0x00;
	final byte MDB_SETTLEMENT_SUCCESS = 0x00;
	
	static int MAX_PRODUCT_NUMBER = 9;

	TransactionStateManager transactionStateManager;
	SerialCommunicator serialHandler;
	Utility utility;
	LogUtility logUtility;

	boolean selfTest = false;

	static int productCode = 0;
	
	private boolean clearAllSession = false;

	private int forceAbortCode = 0;
	
	private boolean settleStatus = false;
	
	
	
	public boolean isSettleStatus() {
		return settleStatus;
	}

	public void setSettleStatus(boolean settleStatus) {
		this.settleStatus = settleStatus;
	}

	public boolean isClearAllSession() {
		return clearAllSession;
	}

	public void setClearAllSession(boolean clearAllSession) {
		this.clearAllSession = clearAllSession;
	}

	public int getForceAbortCode() {
		return forceAbortCode;
	}

	public void setForceAbortCode(int forceAbortCode) {
		this.forceAbortCode = forceAbortCode;
	}

	public NayaxCardReaderAPI(SerialCommunicator serialHandler, Utility utility,
			TransactionStateManager transactionState, LogUtility logUtility) {
		this.serialHandler = serialHandler;
		this.utility = utility;
		this.transactionStateManager = transactionState;
		this.logUtility = logUtility;
	}

	/*
	 * Receiver Section
	 * ------------------------------------------------------------------
	 * ------------------------------------------------------------------
	 * ------------------------------------------------------------------
	 */

	@Override
	CcrMessageEvent checkReader() throws RuntimeException, RuntimeErrorException {
		try {
			byte packLen[] = new byte[2];
			byte pack[];
			int ret = 0, ii;
			ccrMessageEvent = CcrMessageEvent.NO_ACTION;
			do {
				pack = new byte[1];
				ii = 0;
				try {
					ret = serialHandler.serialRead(pack, 1);
				} catch (IOException e) {
					
				} catch (Exception e) {
					
				}
				if (ret <= 0) {
					break;
				} else {
					packLen[ii++] = pack[0];
				}

				while (ii < 2) {
					try {
						ret = serialHandler.serialRead(pack, 1);
					} catch (IOException e) {
						
					} catch (Exception e) {
						
					}
					if (ret > 0) {
						packLen[ii++] = pack[0];
					} else {											
						break;
					}
				}
				if (ii == 2) {
					int len = ((packLen[1] << 8) & 0xff00) | (packLen[0] & 0xff);
					if (len == 0 || len > 255) {
						break;
					}
					packetRcv = new byte[len];
					if (packetRcv == null) {
						break;
					}
					ret = 0;
					ii = 0;

					while (ii < len) {
						try {
							ret = serialHandler.serialRead(pack, 1);
						} catch (IOException e) {
							
						} catch (Exception e) {
							
						}
						if (ret > 0) {
							packetRcv[ii++] = pack[0];
						} else {
							break;
						}
					}

					// Section for show message to CCREventManager
					if (ii == len) {
						logUtility.showMessageOnEventManager(packetRcv, true); // Direction
																				// True
																				// :
																				// CCR
																				// ->
																				// Peripheral
					}
					// End Section

					if (ii == len) {
						lastPacketIdRcv = packetRcv[1];
						lastPacketRetryCount = packetRcv[0];
						switch (packetRcv[6]) {
						case MDB_COMMAND:
							if (packetRcv[7] == MDB_BEGIN_SESSION) {
								receivedBeginSession();
							} else if (packetRcv[7] == MDB_VEND_APPROVE) {
								receivedVendApproved();
							} else if (packetRcv[7] == MDB_VEND_DENY) {
								receivedVendDenied();
							} else if (packetRcv[7] == MDB_VEND_CANCEL) {
								receivedVendCancelled();
							} else if (packetRcv[7] == MDB_END_SESSION) {
								receivedEndSession();
							}
							break;
						case MARSHAL_ACK:
							receivedAck();
							break;
						case MARSHAL_RESET:
							resetKeepAliveTimer();
							receivedReset();
							break;
						case MARSHAL_CONFIG:
							receivedConfig();
							break;
						case MARSHAL_DISPLAYSTATUS:
							receivedDisplayStatus();
							break;
						case MARSHAL_TRANSFER_DATA:
							receivedTransferData();
							storeDataRcvd(packetRcv);
							break;
						case MARSHAL_TRANSFER_STATUS:
							receivedTransferStatus();
							break;
						default:
							break;
						}
					} else {
						break;
					}
					break;
				} else {
					break;
				}
			} while (false);
		} catch (Exception e) {
			logUtility.showMsg(e.getMessage());
		} catch (Error e) {
			logUtility.showMsg(e.getMessage());
		}

		return ccrMessageEvent;
	}
	
	@Override
	void receivedBeginSession() {
		ccrMessageEvent = CcrMessageEvent.BEGIN_SESSION;
		if (sendAck() != 0) {
			
		}

	}
	
	@Override
	void receivedVendApproved() {
		if (checkMessageRetryCount() == 0) {
			ccrMessageEvent = CcrMessageEvent.VEND_APPROVED;
		}
		if (sendAck() == 0) {
			
		} else {

		}
	}

	@Override
	void receivedVendDenied() {
		ccrMessageEvent = CcrMessageEvent.VEND_DENIED;
		if (sendAck() == 0) {
			if (sendSessionComplete() != 0) {
			}
		} else {
		}

	}

	@Override
	void receivedVendCancelled() {
		ccrMessageEvent = CcrMessageEvent.VEND_CANCELED;
		if (sendAck() == 0) {
			if (sendSessionComplete() != 0) {
				
			}
		} else {
			
		}

	}

	@Override
	void receivedEndSession() {
		if (checkMessageRetryCount() == 0)
			ccrMessageEvent = CcrMessageEvent.END_SESSION;
		if (sendAck() == 0) {
		} else {
		}
	}
	
	@Override
	void receivedReset() {
		keepAliveRunning = false;
		ccrMessageEvent = CcrMessageEvent.RESET;
		if (sendFirmwareInfo() != 0) {
		}

	}

	@Override
	void receivedConfig() {
		destAddress = packetRcv[3];
		keepAliveInterval = packetRcv[10];
		keepAliveInterval |= packetRcv[11] << 8;
		keepAliveInterval |= packetRcv[12] << 16;
		keepAliveInterval |= packetRcv[13] << 24;
		keepAliveRunning = true;
		ccrMessageEvent = CcrMessageEvent.CONFIG;
		language = packetRcv[14];
		for (int j = 3; j > 0; j--) {
			country[3 - j] = packetRcv[14 + j];
		}
		for (int j = 15; j >= 0; j--) {
			terminalSerial[15 - j] = packetRcv[22 + j];
		}
		for (int j = 3; j > 0; j--) {
			currency[3 - j] = packetRcv[17 + j];
		}
		try {
			terminalSerialNumber = new String(terminalSerial, "UTF-8");
			currencyCode = new String(currency, "UTF-8");			
			countryCode = Utility.byteArrayToLong(country);
		} catch (UnsupportedEncodingException e) {
			logUtility.showMsg(e.getMessage());
		}

	}

	@Override
	void receivedAck() {
		
		int pktrcv = 0;
		byte pktid = packetRcv[1];
		if (pktid < 0) {
			pktrcv = convertByteToEquvivalentInt(pktid);
		} else {
			pktrcv = pktid;
		}
		
		// Above block in which convertByteToEquvivalentInt is for compatible comparison between byte value and char value
		// char is in between 0 t0 255 but to compare char value greater than 127 with byte we need to convert byte value equivalent to it.
		// @-!
		
		if (lastPacketIdSent == pktrcv) {
			// TRACE(TraceDebug,"Ack");
			switch (lastSentCcrMessage) {
			case SEND_DISPLAY:
				break;
			case MDB_READER_ENABLE:
				// cstartcardTimeout = cendcardTimeout;
				ccrMessageEvent = CcrMessageEvent.READER_ENABLED;
				break;
			case MDB_READER_DISABLE:
				ccrMessageEvent = CcrMessageEvent.READER_DISABLED;
				if (selfTest)
					selfTest = false;
				break;
			case DISPLAY_STATUS:
				break;
			case KEEP_ALIVE:
				break;
			case MDB_VENDREQ:
				if (sendTransferData() != 0) {
					logUtility.showMsg("sendTransferData");
				}
				break;
			case MDB_VENDSUCCESS:				
				if (sendSessionComplete() != 0) {
					logUtility.showMsg("sendSessionComplete failed after Ack");
				}
				break;
			case DATATRANSFER:
				break;
			case MDB_SESSIONCOMPLETE:
				break;
			case MDB_CLOSESESSION:
				if(checkMessageRetryCount() == 0) {
					ccrMessageEvent = CcrMessageEvent.CLOSE_SESSION;
				} else {
					logUtility.showMsg("Close session ack retrived");					
				}
				break;
			default:
				logUtility.showMsg("Last_Sent_Message. Need to handle");				
				break;
			}
		}

	}
	
	@Override
	void receivedTransferData() {
		if (sendAck() == 0) {
			/*if (sendTransferData() != 0) {
				logUtility.showMsg("sendTransferData");
			}*/
		}
	}

	@Override
	void receivedDisplayStatus() {
		if (sendAck() != 0) {
			logUtility.showMsg("receivedDisplayStatus");
		}

	}

	@Override
	void receivedTransferStatus() {
		if (sendAck() != 0) {
			logUtility.showMsg("receivedTransferStatus");
		} else {
			onReceivedTransferStatus();
		}

	}

	void onReceivedTransferStatus() {
		if (packetRcv.length == 13) {
			if (checkMessageRetryCount() == 0) {
				if (packetRcv[10] == MDB_SETTLEMENT_SUCCESS) {
					logUtility.showMsg("SETTLEMENT STATUS SUCCESS");
					setSettlement();
				} else {
					logUtility.showMsg("SETTLEMENT STATUS FAIL");
					setSettlement();
				}
			} else {
				logUtility.showMsg("Msg Retry for receive transfer status");
			}
		}

	}
	
	void setSettlement() {
		if(clearAllSession) {
			if(settleStatus) {
				settleStatus = false;
				logUtility.showMsg("Got Status");
			}
		}
	}

	@Override
	void storeDataRcvd(byte[] buf) {
		int Startposition = 7;

		int Currentposition = Startposition;
		for (int tag = 0; tag < 9; tag++) // for possible maximum 9 tags
		{
			if ((buf[Currentposition] != '\0') && (buf[Currentposition] < 0x0A)
					&& (Currentposition != buf.length - 2)) {
				switch (buf[Currentposition]) {
				case 0x01: {
					Currentposition++;
					int length = buf[Currentposition];
					byte TransactionId[] = new byte[length];
					for (int tst = 0; tst < length; tst++) {
						Currentposition++;
						TransactionId[length - tst - 1] = buf[Currentposition];
					}
					Currentposition++;
					long l = Utility.byteArrayToLong(TransactionId);
					transactionStateManager.transactionState.get(transactionStateManager.getCurrentSesssionId())
							.setTrxnId(l);
				}
					break;
				case 0x02: {
					Currentposition++;
					int length = buf[Currentposition];
					
					for (int tst = 0; tst < length; tst++) {
						Currentposition++;
						
					}
					Currentposition++;
				}
					break;
				case 0x03:

				{
					Currentposition++;
					int length = buf[Currentposition];
					
					for (int tst = 0; tst < length; tst++) {
						Currentposition++;
						
					}
					Currentposition++;
				}
					break;
				case 0x04: {
					Currentposition++;
					int length = buf[Currentposition];
				
					for (int tst = 0; tst < length; tst++) {
						Currentposition++;
						
					}
					Currentposition++;
				}
					break;
				case 0x05: {
					Currentposition++;
					int length = buf[Currentposition];
					
					for (int tst = 0; tst < length; tst++) {
						Currentposition++;
					
					}
					Currentposition++;
				}
					break;
				case 0x06: {
					Currentposition++;
					int length = buf[Currentposition];
					
					for (int tst = 0; tst < length; tst++) {
						Currentposition++;
						
					}
					Currentposition++;
				}
					break;
				case 0x07: {
					Currentposition++;
					int length = buf[Currentposition];
					
					for (int tst = 0; tst < length; tst++) {
						Currentposition++;
						
					}
					Currentposition++;
				}
					break;
				case 0x08: {
					Currentposition++;
					int length = buf[Currentposition];
					
					for (int tst = 0; tst < length; tst++) {
						Currentposition++;
						
					}
					Currentposition++;
				}
					break;
				case 0x09: {
					Currentposition++;
					int length = buf[Currentposition];
					
					for (int tst = 0; tst < length; tst++) {
						Currentposition++;
						
					}
					Currentposition++;
				}
					break;
				default:
					break;
				}
			} else {
				break;
			}
		}
	}

	
	
	/*
	 * Send Section
	 * -------------------------------------------------------------------------
	 * -------------------------------------------------------------------------
	 * -------------------------------------------------------------------------
	 */
	
	@Override
	void actionOnCommand(MarshalActionEvent actionEvent) {
		switch (actionEvent) {
		case SEND_READER_ENABLE:
			if (sendReaderEnable() != 0) {
				logUtility.showMsg("send ReaderEnable failed");
			}
			break;
		case SEND_READER_DISABLE:
			if (sendReaderDisable() != 0) {
				logUtility.showMsg("send ReaderDisable failed");
			}
			break;
		case SEND_VEND_REQUEST:
			if (sendVendRequest() != 0) {
				logUtility.showMsg("send VendRequest failed");
			}
			break;
		case SEND_VEND_SUCCESS:
			if (sendVendSuccess() != 0) {
				logUtility.showMsg("send VendSuccess failed");
			}
			break;
		case SEND_SESSION_COMPLETE:
			if (sendSessionComplete() != 0) {
				logUtility.showMsg("send SessionComplete failed");
			}
			break;
		case SEND_CLOSE_SESSION:
			if (sendCloseSession('0') != 0) {
				logUtility.showMsg("send CloseSession failed");
			}
			break;
		
		default:
			logUtility.showMsg("No action to do in Marshal");
			break;
		}
	}
	
	@Override
	int sendReaderEnable() {
		resetKeepAliveTimer();
		if (sendPacketOnSerial(readerEnable, readerEnable.length, 'a') == 0) {
			resetKeepAliveTimer();
			lastSentCcrMessage = SentCcrMessage.MDB_READER_ENABLE;
			return 0;
		} else {
			return -1;
		}
	}

	@Override
	int sendReaderDisable() {
		if (sendPacketOnSerial(readerDisable, readerEnable.length, 'a') == 0) {
			lastSentCcrMessage = SentCcrMessage.MDB_READER_DISABLE;
			return 0;
		} else {
			return -1;
		}
	}

	@Override
	int sendVendRequest() {
		if (sendPacketOnSerial(vendReq, vendReq.length, 'a') == 0) {
			lastSentCcrMessage = SentCcrMessage.MDB_VENDREQ;
			return 0;
		} else {
			return -1;
		}
	}

	@Override
	int sendVendSuccess() {		
		if (sendPacketOnSerial(vendSuccess, vendSuccess.length, 'a') == 0) {
			lastSentCcrMessage = SentCcrMessage.MDB_VENDSUCCESS;
			return 0;
		} else {
			return -1;
		}
	}

	@Override
	int sendSessionComplete() {		
		if (sendPacketOnSerial(vendSessionComplete, vendSessionComplete.length, '1') == 0) {
			lastSentCcrMessage = SentCcrMessage.MDB_SESSIONCOMPLETE;
			return 0;
		} else {
			return -1;
		}
	}

	@Override
	int sendCloseSession(char vendCancel) {
		if (sendPacketOnSerial(vendCloseSession, vendCloseSession.length, vendCancel) == 0) {
			lastSentCcrMessage = SentCcrMessage.MDB_CLOSESESSION;
			return 0;
		} else {
			return -1;
		}
	}

	@Override
	int sendTransferData() {
		if (sendPacketOnSerial(dataTransfer, dataTransfer.length, 'a') == 0) {
			lastSentCcrMessage = SentCcrMessage.DATATRANSFER;
			return 0;
		} else {
			return -1;
		}
	}

	@Override
	int sendKeepAlive() {
		if (sendPacketOnSerial(keepAlive, keepAlive.length, 'a') == 0) {
			lastSentCcrMessage = SentCcrMessage.KEEP_ALIVE;
			return 0;
		} else {
			return -1;
		}
	}

	@Override
	int sendFirmwareInfo() {
		try {
			lastSentCcrMessage = SentCcrMessage.FIRMWARE_INFO;
			sendPacketOnSerial(firmwareInfo, firmwareInfo.length, 'a');
		} catch (Exception e) {
			logUtility.showMsg(e.getMessage());
		}
		return 0;
	}

	@Override
	int sendDisplay() {
		return 0;
	}
	
	@Override
	synchronized int sendPacketOnSerial(byte writeCommand[], int writeArraySize, char vendCancel)
			throws RuntimeException {
		try {
			short crc, crcLsb, crcMsb;
			int ret = 0;
			writeCommand[3] = (byte) packCounter;
			if (writeCommand[8] == MARSHAL_FWINFO) {
				writeCommand[7] = 0x00;
			} else {
				writeCommand[7] = (byte) destAddress;
			}
			if (writeCommand[8] == MDB_COMMAND && writeCommand[9] == MDB_VEND) {
				if (writeCommand[10] == MDB_VEND_REQUEST) {
					Transaction transaction = transactionStateManager.transactionState
							.get(transactionStateManager.currentSesssionId);
					byte[] bytes;
					try {
						bytes = Utility.intToByteArray((int)(transaction.amount.doubleValue()* 100));
						writeCommand[11] = bytes[3];
						writeCommand[12] = bytes[2];
						bytes = Utility.intToByteArray(transaction.productCode);
						writeCommand[13] = bytes[3];
						writeCommand[14] = bytes[2];
					} catch (IOException e1) {
						logUtility.showMsg(e1.getMessage());
					} catch (Exception e) {
						logUtility.showMsg(e.getMessage());
					}

				} else if (writeCommand[10] == MDB_VEND_SUCCESS) {
					Transaction transaction = transactionStateManager.transactionState
							.get(transactionStateManager.currentSesssionId);
					try {
						byte[] bytes = Utility.intToByteArray(transaction.productCode);
						writeCommand[11] = bytes[3];
						writeCommand[12] = bytes[2];
					} catch (IOException e) {
						logUtility.showMsg(e.getMessage());
					} catch (Exception e) {
						logUtility.showMsg(e.getMessage());
					}

				} else if (writeCommand[10] == MDB_CLOSE_SESSION) {

					if (clearAllSession) {
						try {
							byte[] bytes = Utility.intToByteArray(0);
							writeCommand[12] = bytes[3];
							writeCommand[13] = bytes[2];
							
							bytes = Utility.intToByteArray(forceAbortCode);
							writeCommand[14] = bytes[3];
							writeCommand[15] = bytes[2];
						} catch(IOException e) {
							logUtility.showMsg(e.getMessage());
						} catch(Exception e) {
							logUtility.showMsg(e.getMessage());
						}
					} else {
						List<Transaction> list = transactionStateManager.requestedTransactionForConfirm;
						if (!list.isEmpty()) {
							Transaction transaction = transactionStateManager.requestedTransactionForConfirm.get(0);
							int productCode = transaction.getProductCode();
							BigDecimal finalPrice = transaction.getFinalPrice();
							try {
								byte[] bytes = Utility.intToByteArray((int) (finalPrice.doubleValue() * 100));
								writeCommand[12] = bytes[3];
								writeCommand[13] = bytes[2];

								bytes = Utility.intToByteArray(productCode);

								writeCommand[14] = bytes[3];
								writeCommand[15] = bytes[2];
							} catch (IOException e) {
								logUtility.showMsg(e.getMessage());
							} catch (Exception e) {
								logUtility.showMsg(e.getMessage());
							}
						}
					}
				}
			} else if (writeCommand[8] == MARSHAL_TRANSFER_DATA) {
				try {
					byte[] traId = Utility.intToByteArray(transactionStateManager.getCurrentSesssionId());

					int a = 0;
					for (int i = 11; i < 11 + traId.length; i++) {
						writeCommand[i] = traId[a];
						a++;
					}
				} catch (IOException e) {
					logUtility.showMsg(e.getMessage());
				} catch (Exception e) {
					logUtility.showMsg(e.getMessage());
				}				
			}

			crc = utility.calculateCrc(writeCommand, writeArraySize - 2, (short) 0);
			// 9DE8
			crcLsb = (short) (0x00FF & crc);
			crcMsb = (short) (0xFF00 & crc);
			crcMsb = (short) (crcMsb >> 8);

			writeCommand[writeArraySize - 2] = (byte) crcLsb;
			writeCommand[writeArraySize - 1] = (byte) crcMsb;
			

			try {			
				ret = serialHandler.serialWriter(writeCommand, writeArraySize);
			} catch (IOException e) {
				logUtility.showMsg(e.getMessage());
			} catch (Exception e) {
				logUtility.showMsg(e.getMessage());
			}

			resetKeepAliveTimer();
			lastPacketIdSent = packCounter;
			if (packCounter == 255) {
				packCounter = 0;
			} else {
				packCounter++;
			}
			if (ret == writeArraySize) {
				return 0;
			} else {
				return -1;
			}
		} catch (Error e) {
			logUtility.showMsg(e.getMessage());
			return -1;
		}
	}
	
	@Override
	int sendAck() {
		short crc, crcLsb, crcMsb;
		ack[7] = destAddress;
		ack[3] = lastPacketIdRcv;
		ack[2] = (byte) (lastPacketRetryCount & 0xfe); // MASK ACK bit0 from
														// reply for retried
														// message ACK

		crc = utility.calculateCrc(ack, 10, UTILS_SEED_CCITT);

		crcLsb = (short) (0x00FF & crc);
		crcMsb = (short) (0xFF00 & crc);
		crcMsb = (short) (crcMsb >> 8);

		ack[10] = (byte) crcLsb;
		ack[11] = (byte) crcMsb;

		int ret = 0;

		try {
			ret = serialHandler.serialWriter(ack, ack.length);
		} catch (IOException e) {
			logUtility.showMsg(e.getMessage());			
		}

		resetKeepAliveTimer();

		if (ret == ack.length) {
			return 0;
		} else {
			return -1;
		}
	}
	
	/*
	 * custom methods
	 * -------------------------------------------------------------------------
	 * -------------------------------------------------------------------------
	 * -------------------------------------------------------------------------
	 */
	
	@Override
	void resetKeepAliveTimer() {
		cstart = cend;
	}

	/**
	 * provides incremental integer value. Repeats cycle between 1 to 49.
	 * 
	 * @return integer vale for product code.
	 */
	public int getProductCode() {
		if (productCode > MAX_PRODUCT_NUMBER) {
			productCode = 0;
			return productCode++;
		} else {
			return productCode++;
		}
	}

	/**
	 * Convert byte value to integer value that exceed the range 127
	 * 
	 * @param b:
	 *            byte value need to be converted in integer
	 * @return: integer value of given byte b.
	 */
	public int convertByteToEquvivalentInt(byte b) {
		return (127 + (128 + b) + 1);
	}

	/**
	 * Manipulates byte value to get retry count of a message.
	 * 
	 * @return int value of retry count 0 means first try and so on.
	 */
	public int checkMessageRetryCount() {
		int b = packetRcv[0];
		b = b & 0x0C;
		b = b >> 2;
		return 0;
	}
}
