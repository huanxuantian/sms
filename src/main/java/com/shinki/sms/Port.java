package com.shinki.sms;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shinki.utils.ByteUtil;

import purejavacomm.CommPortIdentifier;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;
import purejavacomm.UnsupportedCommOperationException;

public class Port {
	private CommPortIdentifier portId;
	private SerialPort serialPort;
	private static OutputStreamWriter out;
	private static InputStreamReader in;
	public static final int BUNDRATE_PORT=9600;
	private static Logger logger = LoggerFactory.getLogger(Port.class.getName());
   	public InputStreamReader getIn(){
    		return in;
    	}
	/**
	 * 打开com口
	 *  @param portName 串口名/路径
	 * @return 成功/失败
	 * */
	public boolean open(String portName){
		//默认波特率BUNDRATE_PORT
		return open(portName,BUNDRATE_PORT);
	}
	public boolean open(String portName,int bundrate){
		try {
			portId = CommPortIdentifier.getPortIdentifier(portName);			
			try {
				serialPort = (SerialPort)portId.open("Serial_Communication",10000);
			} catch (PortInUseException e) {
				e.printStackTrace();
				return false;
			}
			// 下面是得到用于和COM口通讯的输入、输出流。
			try {
				in = new InputStreamReader(serialPort.getInputStream());
				out =new OutputStreamWriter(serialPort.getOutputStream());
			} catch (IOException e) {
				System.out.println("IOException");
				return false;
			}
			// 下面是初始化COM口的传输参数，传入bundrate参数为传输速率：9600,115200等。
			try {
				serialPort.setSerialPortParams(bundrate, SerialPort.DATABITS_8,SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			} catch (UnsupportedCommOperationException e) {
				e.printStackTrace();
				return false;
			}
			
		}  catch (NoSuchPortException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public boolean close(){		
		try {
			if(in!=null)
			{
				in.close();
				in=null;
			}
			if(out!=null)
			{
				out.close();
				out=null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
		if(serialPort!=null)
		{
			serialPort.close();
			serialPort=null;
		}
		return true;
	}
	
	public static void writeln(String s) throws Exception {
	    out.write(s);
	    out.write('\r');
	    out.flush();
	  }
	public static void write(String s) throws Exception {
	    out.write(s);
	    out.flush();
	  }	
	public void write(byte[] bytes) throws Exception {
		this.serialPort.getOutputStream().write(bytes, 0, bytes.length);
		this.serialPort.getOutputStream().flush();
	  }
	public static String read() throws Exception {
	    int n, i;
	    char c;
	    String answer ="";
	    for (i = 0; i <20; i++) {
	      while (in.ready()) {            
	        n = in.read();                
	        if (n != -1) {               
	          c = (char)n;                
	          answer = answer + c;        
	          Thread.sleep(1);            
	        } 
	        else break;                  
	      } 
	      if(answer.indexOf("OK")!=-1){
	    	  break;
	      }
	      Thread.sleep(10);              
	    }
	    return answer;                   
	  }
	
	public String sendATNoCr(String atcommand) throws java.rmi.RemoteException {
	    return sendATNoCr(atcommand,100);
	  }
	public String sendATNoCr(String atcommand,int timeout) throws java.rmi.RemoteException {
	    return sendATRAW(atcommand,timeout,0);
	  }	
	public String sendAT(String atcommand) throws java.rmi.RemoteException {
	    return sendAT(atcommand,100);
	  }
	public String sendAT(String atcommand,int timeout) throws java.rmi.RemoteException {
		return sendATRAW(atcommand,timeout,1);
	}
	private String sendATRAW(String atcommand,int timeout,int withCr1) throws java.rmi.RemoteException {
	    String s="";
	    try {    
	    	if(withCr1>0)
	    	{
	    		writeln(atcommand); 
	    	}
	    	else
	    	{
	    		write(atcommand);
	    	}
			  logger.debug("send cmd:["+atcommand+"]");
			  if(timeout<100)//less to 300 
			  {
				  timeout=300;
			  }
			  {
				  long start_tick = System.currentTimeMillis();
				  while(System.currentTimeMillis()-start_tick<=timeout)
				  {
					  s += read();
					  Thread.sleep(100);
					  if(s.indexOf("Ready OK")==-1&&s.indexOf("OK")!=-1)
					  {
						  break;
					  }
				  }
				  logger.debug("recv ack:["+s+"]");
			  }
	    } 
	    catch (Exception e) {
	    	logger.error("ERROR: send AT command failed; " + "Command: " + atcommand + "; Answer: " + s + "  " + e);
	    } 
	    return s;
	  }
	public String sendATByte(byte[] byte_data,int timeout) throws java.rmi.RemoteException {
	    String s="";
	    try {    
	      this.write(byte_data); 
	      logger.debug("send byte:["+ByteUtil.byte2HexString(byte_data)+"]");
	      if(timeout<300)//less to 300 
	      {
	    	  timeout=300;
	      }
	      
	      {
	    	  long start_tick = System.currentTimeMillis();
	    	  Thread.sleep(100);
	    	  while(System.currentTimeMillis()-start_tick<=timeout)
	    	  {
	    		  s += read();
	    		  Thread.sleep(100);
				  if(s.indexOf("OK")!=-1)
				  {
					  break;
				  }
	    	  }
	    	  logger.debug("recv ack:["+s+"]");
	      }
	    } 
	    catch (Exception e) {
	    	logger.error("ERROR: send AT command failed; " + "bytedata: " + ByteUtil.byte2HexString(byte_data) + "; Answer: " + s + "  " + e);
	    } 
	    return s;
	  }	
}
