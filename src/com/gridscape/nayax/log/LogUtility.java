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
package com.gridscape.nayax.log;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.gridscape.nayax.Utility;

public class LogUtility {
	private Utility utility;

	// CCR Commands codes
	private final byte MARSHAL_ACK = 0x00;
	private final byte MARSHAL_RESET = 0x01;
	private final byte MARSHAL_FWINFO = 0x05;
	private final byte MARSHAL_CONFIG = 0x06;
	private final byte MARSHAL_KEEPALIVE = 0x07;
	private final byte MARSHAL_DISPLAYMSG = 0x08;
	private final byte MARSHAL_DISPLAYSTATUS = 0x09;
	private final byte MARSHAL_TRANSFER_DATA = 0x0A;
	private final byte MARSHAL_TRANSFER_STATUS = 0x0B;
	private final byte MDB_COMMAND = (byte) 0x80;
	private final byte MDB_BEGIN_SESSION = 0x03;
	private final byte MDB_SESSION_COMPLETE = 0x04;
	private final byte MDB_END_SESSION = 0x07;
	private final byte MDB_CLOSE_SESSION = (byte) 0x80;
	private final byte MDB_VEND = 0x13;
	private final byte MDB_VEND_REQUEST = 0x00;
	private final byte MDB_VEND_CANCEL = 0x01;
	private final byte MDB_VEND_SUCCESS = 0x02;
	private final byte MDB_VEND_APPROVE = 0x05;
	private final byte MDB_VEND_DENY = 0x06;
	private final byte MDB_READER = 0x14;
	private final byte MDB_READER_ENABLE = 0x01;
	private final byte MDB_READER_DISABLE = 0x00;
	private final byte MDB_SETTLEMENT_SUCCESS = 0x00;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("E HH : mm : ss : SSS");

	private static boolean logUtility;

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	FileInputStream configFile = null;

	public static LogUtility logUtilityInstance;

	public static LogUtility getInstance(boolean enable) {
		if (logUtilityInstance != null) {
			return logUtilityInstance;
		} else {
			logUtilityInstance = new LogUtility(enable);
			return logUtilityInstance;
		}
	}

	public LogUtility(boolean enable) {
		logUtility = enable;
		try {			
			configFile = new FileInputStream("logging.properties");
			LOGGER.setLevel(Level.ALL);
			LogManager.getLogManager().readConfiguration(configFile);
		} catch (SecurityException e) {
			showMsg(e.getMessage());
		} catch (IOException e) {
			showMsg(e.getMessage());
		} catch (Exception e) {
			showMsg(e.getMessage());
		}
	}
	
	public static void setDebug(String value) {
		if(value.equalsIgnoreCase("true")) {
			LogUtility.setLogUtility(true);
		} else {
			LogUtility.setLogUtility(false);
		}
	}

	public static boolean isLogUtility() {
		return logUtility;
	}

	public static void setLogUtility(boolean logUtility) {
		LogUtility.logUtility = logUtility;
	}

	public void setUtility(Utility utility) {
		this.utility = utility;
	}

	public void showMessageOnEventManager(byte[] bytes, boolean msgDirection) { // boolean
		// true
		// means
		// message
		// comes
		// from
		// the
		// CCR
		// to
		// peripheral
		// and
		// false
		// means
		// peripheral
		// to
		// CCR
		if (logUtility) {
			String time = dateFormat.format(new Date());
			if (msgDirection) {
				if (bytes.length < 7) {
					this.showMsg("Invalid Msg\n" + utility.byteArrayToString(bytes, true));
				}
				switch (bytes[6]) {
				case MDB_COMMAND:
					if (bytes[7] == MDB_BEGIN_SESSION) {
						this.showMsg(time + "\nRECEIVE_BEGIN_SESSION", true, false, false, true);
						this.showMsg(utility.byteArrayToString(bytes, true), false, false, false, true);
					} else if (bytes[7] == MDB_VEND_APPROVE) {
						this.showMsg(time + "\nRECEIVE_VEND_APPROVE", true, false, false, true);
						this.showMsg(utility.byteArrayToString(bytes, true), false, false, false, true);
					} else if (bytes[7] == MDB_VEND_DENY) {
						this.showMsg(time + "\nRECEIVE_VEND_DENY", true, false, false, true);
						this.showMsg(utility.byteArrayToString(bytes, true), false, false, false, true);
					} else if (bytes[7] == MDB_VEND_CANCEL) {
						this.showMsg(time + "\nRECEIVE_VEND_CANCEL", true, false, false, true);
						this.showMsg(utility.byteArrayToString(bytes, true), false, false, false, true);
					} else if (bytes[7] == MDB_END_SESSION) {
						this.showMsg(time + "\nRECEIVE_END_SESSION", true, false, false, true);
						this.showMsg(utility.byteArrayToString(bytes, true), false, false, false, true);
					}
					break;
				case MARSHAL_ACK:
					this.showMsg(time + "\nRECEIVED_RESPONSE", false, true, false, false);
					this.showMsg(utility.byteArrayToString(bytes, true), false, true, false, false);
					break;
				case MARSHAL_RESET:
					this.showMsg(time + "\nRECEIVED_RESET", true, false, false, true);
					this.showMsg(utility.byteArrayToString(bytes, true), false, false, false, true);
					break;
				case MARSHAL_CONFIG:
					this.showMsg(time + "\nRECEIVE_CONFIG", true, false, false, true);
					this.showMsg(utility.byteArrayToString(bytes, true), false, false, false, true);
					break;
				case MARSHAL_DISPLAYSTATUS:
					this.showMsg(time + "\nRECEIVE_DISPLAY_STATUS", true, false, false, true);
					this.showMsg(utility.byteArrayToString(bytes, true), false, false, false, true);
					break;
				case MARSHAL_TRANSFER_DATA:
					this.showMsg(time + "\nRECEIVE_TRANSFER_DATA", true, false, false, true);
					this.showMsg(utility.byteArrayToString(bytes, true), false, false, false, true);
					break;
				case MARSHAL_TRANSFER_STATUS:
					if (bytes[10] == MDB_SETTLEMENT_SUCCESS) {
						this.showMsg(time + "\nRECEIVE_STATUS_SUCCESS", true, false, false, true);
						this.showMsg(utility.byteArrayToString(bytes, true), false, false, false, true);
					} else {
						this.showMsg(time + "\nRECEIVE_STATUS_UNSUCCESSFUL", true, false, false, true);
						this.showMsg(utility.byteArrayToString(bytes, true), false, false, false, true);
					}
					break;
				default:
					this.showMsg(time + "\nRECEIVE_UNTRACED_COMMAND", true, false, false, true);
					this.showMsg(utility.byteArrayToString(bytes, true), false, false, false, true);
					break;
				}

			}

			if (!msgDirection) {
				switch (bytes[8]) {
				case MARSHAL_KEEPALIVE:
					this.showMsg(time + "\nSEND_KEEPALIVE", false, true, false, false);
					this.showMsg(utility.byteArrayToString(bytes, false), false, true, false, false);
					break;
				case MARSHAL_FWINFO:
					this.showMsg(time + "\nSEND_FIRMWAREINFO", true, false, false, true);
					this.showMsg(utility.byteArrayToString(bytes, false), false, false, false, true);
					break;
				case MDB_COMMAND:
					if (bytes[8] == MDB_COMMAND && bytes[9] == MDB_READER) {
						if (bytes[10] == MDB_READER_ENABLE) {
							this.showMsg(time + "\nMDB_READER_ENABLE", true, false, false, true);
							this.showMsg(utility.byteArrayToString(bytes, false), false, false, false, true);
						} else if (bytes[10] == MDB_READER_DISABLE) {
							this.showMsg(time + "\nSEND_READER_DISABLE", true, false, false, true);
							this.showMsg(utility.byteArrayToString(bytes, false), false, false, false, true);
						}
					} else if (bytes[8] == MDB_COMMAND && bytes[9] == MDB_VEND) {
						if (bytes[10] == MDB_VEND_REQUEST) {
							this.showMsg(time + "\nSEND_VEND_REQUEST", true, false, false, true);
							this.showMsg(utility.byteArrayToString(bytes, false), false, false, false, true);
						} else if (bytes[10] == MDB_VEND_SUCCESS) {
							this.showMsg(time + "\nSEND_VEND_SUCCESS", true, false, false, true);
							this.showMsg(utility.byteArrayToString(bytes, false), false, false, false, true);
						} else if (bytes[10] == MDB_CLOSE_SESSION) {
							this.showMsg(time + "\nMDB_CLOSE_SESSION", true, false, false, true);
							this.showMsg(utility.byteArrayToString(bytes, false), false, false, false, true);
						} else if (bytes[10] == MDB_VEND_CANCEL) {
							this.showMsg(time + "\nSEND_VEND_CANCEL", true, false, false, true);
							this.showMsg(utility.byteArrayToString(bytes, false), false, false, false, true);
						} else if (bytes[10] == MDB_SESSION_COMPLETE) {
							this.showMsg(time + "\nSEND_SESSION_COMPLETE", true, false, false, true);
							this.showMsg(utility.byteArrayToString(bytes, false), false, false, false, true);
						}
					}
					break;
				case MARSHAL_TRANSFER_DATA:
					this.showMsg(time + "\nSEND_TRANSFER_DATA", true, false, false, true);
					this.showMsg(utility.byteArrayToString(bytes, false), false, false, false, true);
					break;
				case MARSHAL_ACK:
					this.showMsg(time + "\nSEND_ACK", true, false, false, true);
					this.showMsg(utility.byteArrayToString(bytes, false), false, false, false, true);
					break;
				default:
					this.showMsg(time + "\nSEND_UNTRACED_COMMAND", true, false, false, true);
					this.showMsg(utility.byteArrayToString(bytes, false), false, false, false, true);
					break;
				}
			}
		}
	}

	public void showMsg(String msg, boolean b, boolean c, boolean d, boolean e) {
		if (logUtility) {
			LOGGER.info(msg);
			// logPlus.showMsg(string, b, c, d, e);
		}
	}

	public void showMsg(String msg) {
		if (logUtility) {
//			System.out.println(msg);
			LOGGER.info(msg);
		}
	}

	public void close() {

	}

}
