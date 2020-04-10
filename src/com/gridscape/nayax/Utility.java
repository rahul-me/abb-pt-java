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
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utility {
	
	/**
	 * Provides system required utilities.
	 * 
	 */
	
	long initTime, currentTime;
	
	/**
	 * Provides system times.
	 * 
	 * @param init:
	 *            boolean type true to get initTime(the time when system has
	 *            been initialized) boolean type false to get time difference
	 *            between current time and time when system has been
	 *            initialized.
	 * @return time in milliseconds
	 */
	double elapsedTime(boolean init) {
	    double etime1, etime2;
	    if (init) {
	    	initTime = System.currentTimeMillis( );	    	
	        return initTime;
	    }
	    currentTime = System.currentTimeMillis( );	    
	    etime1 = currentTime;
	    etime2 = initTime;	    
	    return etime1 - etime2;
	}
	int getNewSessionId(){
		long epoch = System.currentTimeMillis();
		int sessionId = (int) (epoch / 1000);
	    return sessionId;
	}
	
	/**
	 * Generates Cyclic Redundancy Code for given array of bytes
	 * 
	 * @param pData:
	 *            Data for which CRC requires
	 * @param len:
	 *            size of data
	 * @param seed:
	 *            seed value to check CRC
	 * @return CRC code
	 */
	short  calculateCrc(byte[] pData, int len, short seed) {
	    int i;
	    short crc = seed;

	    for (i = 0; i < len; i++)
	        crc = Utils_CRC_CCITT(crc, pData[i]);

	    return ( crc);
	}
	
	/**
	 * Calculates CRC code byte by byte. used by
	 * {@link #calculateCrc(byte[],int, short)}
	 * 
	 * @param crc:
	 *            seed value
	 * @param byte1:
	 *            byte to be used in calculation of CRC code.
	 * @return calculated value for CRC in intermediate stages.
	 */
	short  Utils_CRC_CCITT(short crc, byte byte1) {
		byte i;
		short Temp, ShortC, CurrentCRC = 0;

	    ShortC = (short)(byte1 & 0x00FF);
	    Temp = (short) (((crc >> 8) ^ ShortC) << 8);
	    for (i = 0; i < 8; i++) {
	        if (((CurrentCRC ^ Temp) & 0x8000) == 0x8000)
	            CurrentCRC = (short) ((CurrentCRC << 1) ^ 0x1021); /* 1010.0000 0000.0001 = x^16 + x^15 + x^13 + 1*/
	        else
	            CurrentCRC <<= 1;
	        Temp <<= 1;
	    }

	    return (short) ( (crc << 8) ^ CurrentCRC);
	}
	
	/**
	 * Converts given array of byte to long value
	 * 
	 * @param bytes:
	 *            array of bytes needed to convert in long.
	 * @return : long value of given array of byte
	 */
	public static long byteArrayToLong(byte[] bytes){
		long value = 0;
		for (int i = 0; i < bytes.length; i++)
		{
		   value = (value << 8) + (bytes[i] & 0xff);
		}
		return value;
	}
	
	/**
	 * Converts given Integer value to array of bytes.
	 * 
	 * @param currentSesssionId:
	 *            Integer value needed to convert in array of bytes
	 * @return array of bytes for given Integer value
	 * @throws IOException
	 */
	public static byte[] intToByteArray(Integer currentSesssionId) throws IOException {
		byte[] bytes = ByteBuffer.allocate(4).putInt(currentSesssionId).array();
		return bytes;
	}
	
	/**
	 * Converts given Integer value to array of bytes in order of little endian.
	 * 
	 * @param currentSesssionId:
	 *            Integer value needed to convert in array of bytes
	 * @return array of bytes in little endian order for given Integer value.
	 * @throws IOException
	 */
	public byte[] intToByteArrayLE(Integer integer){
		return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(integer).array();
	}
	
	/**
	 * Converts given array of bytes in String in form of hexadecimal
	 * 
	 * @param arr:
	 *            array of bytes need to be converted
	 * @param direction:
	 *            represents message flow 
	 *            true : Credit card reader -> payment
	 *            module 
	 *            false : payment module -> Credit card reader
	 * @return String data in form of hexadecimal values
	 */
	public String byteArrayToString(byte[] arr,boolean direction){
		String msg="";
		if(direction){			
			int b = arr[0];
			b = b & 0x0C;
			b = b >> 2;
			msg += "Rx :: ("+arr.length+") (Ret-"+b+") ";
		} else
			msg += "Tx :: ("+arr.length+") ";
		for(int i=0; i< arr.length;i++) {
			msg += String.format("%02X ", arr[i]);
		}
		return msg;	
	}
	
	public static double roundUp(BigDecimal value) {
		return Math.round(value.floatValue() * 100.0) / 100.0;
	}
}	
