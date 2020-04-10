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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.gridscape.nayax.Marshal.SerialPortConnectionError;
import com.gridscape.nayax.log.LogUtility;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

public class SerialCommunicator {

	/**
	 * Provides the application programming interface to connect with serial
	 * port, read data from the serial port, write data to serial port,
	 * disconnect serial port.
	 */

	LogUtility logUtility;
	SerialPort serialPort;
	InputStream inputStream;
	int bufferSize = 256;
	int baudRate = 115200;
	int timeout = 2000;
	OutputStream outputStream;
	static Lock mutex = new ReentrantLock(true);

	/**
	 * Connects serial port using portName
	 * 
	 * @param portName:
	 *            serial communication port identifier
	 * @return error while connecting serial port. If there is successful
	 *         connection it returns AVAILABLE. SerialPortConnectionError
	 *         defined as enum types are AVAILABLE, NO_SUCH_PORT, PORT_IN_USE,
	 *         UNSUPPORTED_COMM_OPERATION, IO_OPREATION, NO_SERIAL_PORTS
	 */
	public SerialPortConnectionError connect(String portName) {
		try {
			if (!listPorts()) {
				throw new Exception();
			}
			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
			if (portIdentifier.isCurrentlyOwned()) {				
				logUtility.showMsg("Error: Port is currently in use");
			} else {
				CommPort commPort = portIdentifier.open(this.getClass().getName(), this.timeout);

				if (commPort instanceof SerialPort) {
					serialPort = (SerialPort) commPort;
					serialPort.enableReceiveTimeout(15);
					serialPort.enableReceiveThreshold(0);
					serialPort.setSerialPortParams(this.baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
							SerialPort.PARITY_NONE);
					//inputStream = serialPort.getInputStream();
					// outputStream = serialPort.getOutputStream();
				} else {					
					logUtility.showMsg("Error: Only serial ports are handled");
				}
			}
			return SerialPortConnectionError.NONE;
		} catch (NoSuchPortException nspe) {			
			return SerialPortConnectionError.NO_SUCH_PORT;
		} catch (PortInUseException piu) {
			return SerialPortConnectionError.PORT_IN_USE;
		} catch (UnsupportedCommOperationException e) {			
			return SerialPortConnectionError.UNSUPPORTED_COMM_OPERATION;
		} catch (IOException e) {			
			return SerialPortConnectionError.IO_OPREATION;
		} catch (Exception e) {			
			return SerialPortConnectionError.NO_SERIAL_PORTS;
		}
	}
	
	/**
	 * Disconnects the serial port.
	 */
	public void disconnect() {
		if (serialPort != null) {
			try {
				if (outputStream != null) {
					outputStream.close();
				}
				if(inputStream != null) {
					inputStream.close();
				}				
			} catch (IOException e) {
				logUtility.showMsg(e.getMessage());
			}
			serialPort.close();
		}
	}

	/**
	 * Writes data to serial port of given size.
	 * 
	 * @param arr:
	 *            data to be written on serial port
	 * @param size:
	 *            size of data(length of byte array)
	 * @return size of data written to serial port
	 * @throws IOException
	 */
	int serialWriter(byte arr[], int size) throws IOException {
		try {
			SerialCommunicator.mutex.lock();
			outputStream = serialPort.getOutputStream();
			outputStream.write(arr, 0, size);
			outputStream.flush();
			outputStream.close();
			logUtility.showMessageOnEventManager(arr, false);		
		} catch (IOException e) {
			logUtility.showMsg(e.getMessage());
		} catch(Exception e) {
			logUtility.showMsg(e.getMessage());
		} catch(Error e) {
			logUtility.showMsg(e.getMessage());
		} finally {
			SerialCommunicator.mutex.unlock();
		}	
		return size;
	}
	
	/**
	 * 
	 * @param arr:
	 *            array of bytes to store received data from serial port.
	 * @param size:
	 *            length of data to be received from serial port.
	 * @return Reads up to size bytes of data from the input stream into an
	 *         array of bytes. An attempt is made to read as many as len bytes,
	 *         but a smaller number may be read. The number of bytes actually
	 *         read is returned as an integer. If no byte is available because
	 *         the stream is at end of file, the value -1 is returned;
	 *         otherwise, at least one byte is read and stored into arr.
	 * @throws IOException
	 */
	int serialRead(byte arr[], int size) throws IOException {		
		int numBytes = 0;
		try {
			SerialCommunicator.mutex.lock();			
			inputStream = serialPort.getInputStream();			
			numBytes = inputStream.read(arr, 0, size);
			inputStream.close();
		} catch (IOException e) {						
			logUtility.showMsg("Read: IOException");
		} catch(Exception e) {
			logUtility.showMsg(e.getMessage());			
		} catch (Error e) {
			logUtility.showMsg(e.getMessage());			
		}
		finally {
			SerialCommunicator.mutex.unlock();
		}
		return numBytes;
	}
	
	/**
	 * lists available ports
	 * 
	 * @return false if no ports are there.
	 */
	public boolean listPorts() {
		int serialPortCount = 0;
		@SuppressWarnings("rawtypes")
		Enumeration ports = CommPortIdentifier.getPortIdentifiers();
		while (ports.hasMoreElements()) {
			CommPortIdentifier port = (CommPortIdentifier) ports.nextElement();
			switch (port.getPortType()) {
			case CommPortIdentifier.PORT_PARALLEL:				
				break;
			case CommPortIdentifier.PORT_SERIAL:
				serialPortCount++;				
				break;		
			default:			
				break;
			}
			logUtility.showMsg(port.getName());
		}
		if (serialPortCount > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	//getter and setters
	public SerialPort getSerialPort() {
		return serialPort;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public int getBaudRate() {
		return baudRate;
	}

	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public LogUtility getLogUtility() {
		return logUtility;
	}

	public void setLogUtility(LogUtility logUtility) {
		this.logUtility = logUtility;
	}

}
