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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.abb.evci.payment.PaymentCommandResultListener;
import com.abb.evci.payment.PaymentEventListener;
import com.abb.evci.payment.PaymentModuleInterface;
import com.abb.evci.payment.PaymentModuleStatusListener;
import com.abb.evci.payment.PaymentEventListener.ConfirmationResult;
import com.abb.evci.payment.PaymentModuleStatusListener.PaymentModuleStatus;
import com.gridscape.nayax.log.LogUtility;

public class PaymentModuleImpl implements PaymentModuleInterface {
	
	PaymentModuleStatusListener paymentModuleStatusListener;	
    ActionExecutor actionExecutor;
    static boolean paymentModuleInitialized = false; 
    TransactionStateManager transactionStateManager;
    public static List<String> commands = new ArrayList<String>();
    private LogUtility logUtility;
	
    
	
	
	public static final String SERIAL_PORT_ID_PARA = "serialPortId";
	public static final String CURRENCY_CODE_PARA = "Currency";
	public static final String LANGUAGE_CODE_PARA = "Language";
	public static final String CARD_SWIPE_TIMEOUT_PARA = "cardSwipeTimeOut";
	public static final String DEBUG_PARA = "debug";
	public static final String DATE_FORMAT = "dateFormat";
	public static final String MAX_PRODUCT_NUMBER = "maxSession";
	public static final String CLEAR_OPEN_SESSION_ON_STARTUP = "clearSessionsOnStartup";
	
	private static final String DEFAULT_DATE_FORMAT = "MMM dd, yyyy hh:mm:ss a";
	
	private static final String[] parameterList = new String[] { SERIAL_PORT_ID_PARA, CURRENCY_CODE_PARA,
			CARD_SWIPE_TIMEOUT_PARA, DEBUG_PARA, LANGUAGE_CODE_PARA, DATE_FORMAT, MAX_PRODUCT_NUMBER,
			CLEAR_OPEN_SESSION_ON_STARTUP };
	private static List<String> parameters = Arrays.asList(parameterList);
	
	private static Map<String, String> configMap = new HashMap<String, String>();
	
	public static SimpleDateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
	private String terminalErrorTime = dateFormat.format(new Date());
	
	public static final String LIBRARY_VERSION = "v1.5";
	
	public static Map<String, String> getConfigMap() {
		return configMap;
	}

	public static void setConfigMap(Map<String, String> configMap) {
		PaymentModuleImpl.configMap = configMap;
	}
	
	public static List<String> getParameters() {
		return parameters;
	}

	public PaymentModuleImpl(){
    	commands.add("confirmPayment");
    	commands.add("abortPayment");
    	
    	configMap.put(SERIAL_PORT_ID_PARA, "");
    	configMap.put(CURRENCY_CODE_PARA, "");
    	configMap.put(LANGUAGE_CODE_PARA, "");
    	configMap.put(CARD_SWIPE_TIMEOUT_PARA, "60");
    	configMap.put(DEBUG_PARA, "false");
    	configMap.put(DATE_FORMAT, DEFAULT_DATE_FORMAT);
    	configMap.put(CLEAR_OPEN_SESSION_ON_STARTUP, "true");
    	configMap.put(MAX_PRODUCT_NUMBER, "10");
    }
    
    
    
    public LogUtility getLogUtility() {
		return logUtility;
	}



	public void setLogUtility(LogUtility logUtility) {
		this.logUtility = logUtility;
	}



	@Override
	public void registerStatusListener(PaymentModuleStatusListener listener) {
		if (listener != null) {
			this.paymentModuleStatusListener = listener;
			ActionExecutor.paymentModuleStatusListener = listener;
		}
	}

	
	public boolean initiate(Map<String, String> map) {			
		if(paymentModuleInitialized || map == null){
			return false;
		}
		
		if(!checkConfiguration(map)){
			return false;
		}		
		configMap = modifyCIMap(map);		
		if(configMap.get(DEBUG_PARA) != null) {
			String val = configMap.get(DEBUG_PARA);
			if(val.equals("true")) {
				logUtility = new LogUtility(true);
			} else {
				logUtility = new LogUtility(false);
			}
		} else {
			logUtility = new LogUtility(false);
		}
		actionExecutor = new ActionExecutor(paymentModuleStatusListener, getTransactionStateManager(), logUtility, commands);
		actionExecutor.initializeTimers(true);
		paymentModuleInitialized = actionExecutor.onInitiate(configMap);
		return paymentModuleInitialized;
	}
			
	@Override
	public boolean shutdown() {
		if (!paymentModuleInitialized) {
			return false;
		}
		if (actionExecutor.onShutdown()) {
			paymentModuleStatusListener = null;
			actionExecutor = null;
			paymentModuleInitialized = false;			
		}
		return true;
	}
	
	@Override
	public int startPayment(String currency, BigDecimal amount, PaymentEventListener listener) {
		if(!paymentModuleInitialized){
			return -1;
		}
		return actionExecutor.onStartPayment(currency, amount, listener);		
	}

	@Override
	public boolean abortPayment(int paymentSessionId) {
		if(!paymentModuleInitialized){
			return false;
		}
		return actionExecutor.onAbortPayment(paymentSessionId);		
	}

	@Override
	public boolean confirmPayment(int paymentSessionId, BigDecimal finalAmount) {
		if(!paymentModuleInitialized){		
			return false;
		}
		return actionExecutor.onConfirmPayment(paymentSessionId, finalAmount);
	}
	
	@Override
	public List<Integer> getOpenPaymentSessions() {
		if(transactionStateManager != null){
			if(actionExecutor != null) {
				return actionExecutor.onGetOpenPaymentSessions();
			} else {
				return new ArrayList<Integer>();
			}
		} else {
			return new ArrayList<Integer>();
		}
				
	}
	
	@Override
	public PaymentModuleStatus getPaymentModuleStatus() {		
		if(!paymentModuleInitialized){
			return PaymentModuleStatus.NOT_AVAILABLE;			
		}
		return actionExecutor.onGetPaymentModuleStatus(); 		
	}

	@Override
	public boolean reportDiagnose(PaymentCommandResultListener result) {
		if(!paymentModuleInitialized) {
			result.commandFinished("Report_diagnose", ConfirmationResult.SUCCEEDED,
			"CCR Serial Number: "+Marshal.terminalSerialNumber +
			"\nCountry Code: " + Marshal.countryCode +
			"\nCurrency Code: " + Marshal.currencyCode + 
			"\nPayment Module Status: " +PaymentModuleStatus.NOT_AVAILABLE +
			"\nTerminal Error: " + Marshal.terminalError + " on "+terminalErrorTime +
			"\nMax Sessions Supported: " + (NayaxCardReaderAPI.MAX_PRODUCT_NUMBER+1) +
			"\nLibrary Version: "+LIBRARY_VERSION);
			return true;
		}
		actionExecutor.onReportDiagnose(result);
		return true;
	}
	
	@Override
	public PaymentStyle getPaymentStyle() {
		return PaymentStyle.PRE_AUTHORIZE;
	}

	@Override
	public boolean refundPayment(int paymentSessionId, BigDecimal amount) {
		return false;
	}

	@Override
	public boolean isRefundSupported() {	
		return false;
	}

	@Override
	public String getConfiguration(String key) {
		return configMap.get(key);
	}

	@Override
	public boolean setConfiguration(String key, String value) {
		if(key.equals("") || key.equals(null) || value.equals(null) || value.equals("") || !parameters.contains(key)) {
			return false;
		}
		boolean result = false;
		switch(key){
		case SERIAL_PORT_ID_PARA:
			configMap.put(SERIAL_PORT_ID_PARA, value);
			result = true;
			break;
		case DEBUG_PARA:
			if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
				configMap.put(DEBUG_PARA, value);
				LogUtility.setDebug(value);
				result = true;
			} else {
				result = false;
			}						
			break;
		case CURRENCY_CODE_PARA:
			if(currencyCodeList.contains(value)) {
				configMap.put(CURRENCY_CODE_PARA, value);
				result = true;
			} else {
				result = false;
			}
			break;
		case CARD_SWIPE_TIMEOUT_PARA:
			try{
				Integer.parseInt(value);
				configMap.put(CARD_SWIPE_TIMEOUT_PARA, value);
				result = true;
			} catch(NumberFormatException e) {
				result = false;
			}
			break;
		case LANGUAGE_CODE_PARA:
			configMap.put(LANGUAGE_CODE_PARA, value);
			result = true;
			break;
		case DATE_FORMAT:
			if(value == null || value.equals("")) {
				result = false;
			} else {
				try {
					new SimpleDateFormat(value);
					dateFormat = new SimpleDateFormat(value);
					configMap.put(DATE_FORMAT, value);
					result = true;
				} catch(IllegalArgumentException | NullPointerException npe) {
					result = false;
				}
			}
			break;
		case MAX_PRODUCT_NUMBER:
			if(value == null || value.equals("")) {
				result = false;
			} else {
				try {
					int maxProductNumber = Integer.parseInt(value);
					NayaxCardReaderAPI.MAX_PRODUCT_NUMBER = maxProductNumber;
					ActionExecutor.setAbortList();
					result = true;
				}catch(NumberFormatException e) {
					result = false;
				}
			}
			break;
		case CLEAR_OPEN_SESSION_ON_STARTUP:
			if(value == null || value.equals("") || (!value.equals("true") && !value.equals("false"))) {
				result = false;
			} else {
				if(value.equals("true")) {
					configMap.put(CLEAR_OPEN_SESSION_ON_STARTUP, "true");
				} else {
					configMap.put(CLEAR_OPEN_SESSION_ON_STARTUP, "false");
				}
				result = true;
			}
			break;
		default:
			result = false;
			break;
		}
		return result;		
	}

	@Override
	public List<String> getSupportedParameters() {
			return parameters;
	}

	@Override
	public List<String> getSupportedCommands() {		
		return commands;
	}

	@Override
	public boolean executeCommand(String command, List<String> args, PaymentCommandResultListener listener) {	
		if(!paymentModuleInitialized){
			return false;			
		}
		return actionExecutor.onExecuteCommand(command, args, listener);
		
	}

	@Override
	public boolean confirmPayment(int paymentSessionId) {		
		return false;
	}

	@Override
	public boolean performSelfTest(PaymentCommandResultListener result) {
		if(!paymentModuleInitialized){
			return false;
		}
		return actionExecutor.onPerformSelfTest(result);
	}

	@Override
	public boolean reset() {
		if(!paymentModuleInitialized){
			return false;
		}
		return actionExecutor.onReset();
		
	}
	
	TransactionStateManager getTransactionStateManager(){
		if(transactionStateManager != null){			
			return transactionStateManager;
		} else {			
			transactionStateManager = new TransactionStateManager();
			return transactionStateManager;
		}
	}
	
	private boolean checkConfiguration(Map<String, String> map) {
		Set<String> keys = map.keySet();
		for(String key : keys) {
			if(!parameters.contains(key)) {
				return false;
			}
		}
		
		if(map.containsKey(DEBUG_PARA) && map.get(DEBUG_PARA) != null) {
			if(!(map.get(DEBUG_PARA).equals("true") || map.get(DEBUG_PARA).equals("false"))) {
				return false;
			}
		}
		
		if (!map.containsKey(SERIAL_PORT_ID_PARA) || map.get(SERIAL_PORT_ID_PARA) == null || !map.containsKey(CURRENCY_CODE_PARA)
				|| !currencyCodeList.contains(map.get(CURRENCY_CODE_PARA))) {
			return false;
		}
		
		if(map.containsKey(DATE_FORMAT)) {
			if(map.get(DATE_FORMAT) == null || map.get(DATE_FORMAT).equals("")) {
				return false;
			} else {
				try {
					dateFormat = new SimpleDateFormat(map.get(DATE_FORMAT));
				} catch(IllegalArgumentException | NullPointerException npe) {
					return false;
				}
			}
			
		}
		
		if(map.containsKey(MAX_PRODUCT_NUMBER)) {
			if(map.get(MAX_PRODUCT_NUMBER) == null || map.get(MAX_PRODUCT_NUMBER) == "" || Integer.parseInt(map.get(MAX_PRODUCT_NUMBER)) <= 0) {
				return false;
			} else {
				try {
					int maxProductNumber = Integer.parseInt(map.get(MAX_PRODUCT_NUMBER));
					NayaxCardReaderAPI.MAX_PRODUCT_NUMBER = maxProductNumber-1;
				} catch(NumberFormatException e) {
					return false;
				}
			}
		}
		
		if(map.containsKey(CLEAR_OPEN_SESSION_ON_STARTUP)) {
			String value = map.get(CLEAR_OPEN_SESSION_ON_STARTUP);
			if(value == null || value.equals("") || (!value.equals("true") && !value.equals("false"))) {
				return false;
			} else {
				if(value.equals("true")) {
					configMap.put(CLEAR_OPEN_SESSION_ON_STARTUP, "true");
				} else {
					configMap.put(CLEAR_OPEN_SESSION_ON_STARTUP, "false");
				}			
			}
		}
		
		return true;
	}
	
	private Map<String, String> modifyCIMap(Map<String, String> map) {
		Set<String> keys = map.keySet();
		for(String key : keys) {
			configMap.put(key, map.get(key));
		}
		return configMap;
	}
	
	private static final String[] currencyCodes = new String[] { "AED", "AFN", "ALL", "AMD", "ANG", "AOA", "ARS", "AUD", "AWG",
			"AZN", "BAM", "BBD", "BDT", "BGN", "BHD", "BIF", "BMD", "BND", "BOB", "BOV", "BRL", "BSD", "BTN", "BWP",
			"BYN", "BYR", "BZD", "CAD", "CDF", "CHE", "CHF", "CHW", "CLF", "CLP", "CNY", "COP", "COU", "CRC", "CUC",
			"CUP", "CVE", "CZK", "DJF", "DKK", "DOP", "DZD", "EGP", "ERN", "ETB", "EUR", "FJD", "FKP", "GBP", "GEL",
			"GHS", "GIP", "GMD", "GNF", "GTQ", "GYD", "HKD", "HNL", "HRK", "HTG", "HUF", "IDR", "ILS", "INR", "IQD",
			"IRR", "ISK", "JMD", "JOD", "JPY", "KES", "KGS", "KHR", "KMF", "KPW", "KRW", "KWD", "KYD", "KZT", "LAK",
			"LBP", "LKR", "LRD", "LSL", "LYD", "MAD", "MDL", "MGA", "MKD", "MMK", "MNT", "MOP", "MRO", "MUR", "MVR",
			"MWK", "MXN", "MXV", "MYR", "MZN", "NAD", "NGN", "NIO", "NOK", "NPR", "NZD", "OMR", "PAB", "PEN", "PGK",
			"PHP", "PKR", "PLN", "PYG", "QAR", "RON", "RSD", "RUB", "RWF", "SAR", "SBD", "SCR", "SDG", "SEK", "SGD",
			"SHP", "SLL", "SOS", "SRD", "SSP", "STD", "SYP", "SZL", "THB", "TJS", "TMT", "TND", "TOP", "TRY", "TTD",
			"TWD", "TZS", "UAH", "UGX", "USD", "USN", "USS", "UYI", "UYU", "UZS", "VEF", "VND", "VUV", "WST", "XAF",
			"XAG", "XAU", "XBA", "XBB", "XBC", "XBD", "XCD", "XDR", "XFU", "XOF", "XPD", "XPF", "XPT", "XSU", "XTS",
			"XUA", "XXX", "YER", "ZAR", "ZMW" };
	
	private static final List<String> currencyCodeList = Arrays.asList(currencyCodes);

	public static List<String> getCurrencycodelist() {
		return currencyCodeList;
	}
	
	
	

}

