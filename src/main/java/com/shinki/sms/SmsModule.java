package com.shinki.sms;

import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shinki.utils.ByteUtil;

import purejavacomm.SerialPort;


public class SmsModule {
	private Port port=null;
	private static char symbol1 = 13;
	private static char symbolCr = 10;
	private static String strReturn = "", atCommand = "";
	private boolean flag = false;
	public static final int PDUMODE=0x00;
	public static final int TEXTMODE=0x01;
	public static final int DEFMODE=PDUMODE;
	private int textmode=PDUMODE; 
	protected volatile int bundrate=115200;
	protected volatile int m_DataBits = SerialPort.DATABITS_8;
	protected volatile int m_StopBits = SerialPort.STOPBITS_1;
	protected volatile int m_parity = SerialPort.PARITY_NONE;
	private String dev_name=null; 
	private static Logger logger = LoggerFactory.getLogger(SmsModule.class);
	public String device_info=null;
	
	public static final int DEVICE_UNKNOWN=-1;
	public static final int DEVICE_NOSET=0x00;
	public static final int DEVICE_GM35=0x01;
	public static final int DEVICE_SIM800=0x02;
	public static final int DEVICE_SIM808=0x03;
	public static final int DEVICE_DMU=0x04;//专用短信模块
	private int device_type=DEVICE_NOSET;
	
	String now_smsc=null;

	public SmsModule(String comName) {
		this.dev_name = comName;
	}
	public SmsModule(String comName, int bundrate) {
		this.dev_name = comName;
		this.bundrate =bundrate;
	}
	public SmsModule(String comName, int bundrate,int pdu0text1) {
		this.dev_name = comName;
		this.bundrate =bundrate;
		this.textmode = pdu0text1;
	}
	
	public void setParam(String comName, int bundrate,int pdu0text1)
	{
		this.dev_name = comName;
		this.bundrate =bundrate;
		this.textmode = pdu0text1;
	}
	public void setPortParam(String portName, int baudrate, int databit, int stopbit, int parity) {
		dev_name = portName;
		bundrate = baudrate;
		m_DataBits = databit;
		m_StopBits = stopbit;
		m_parity = parity;
	}
	
	public boolean setmode(int pdu0text1)
	{
		this.textmode = pdu0text1;
		if(port==null) return false;
		return setMessageMode(this.textmode);
	}
	public boolean startUp(String sms_center_num) throws InterruptedException
	{
		if(this.dev_name!=null)
		{
			if(port==null)
			{
				port = new Port();
			}
		}
		return init_Port(sms_center_num);
	}
	
	protected boolean init_Port(String sms_center_num) throws InterruptedException
	{
		if(port!=null)
		{
			System.out.println("正在连接" + this.dev_name + "通讯端口...");
			port.close();
			if (port.open(this.dev_name,this.bundrate)) {
				logger.info(this.dev_name +":"+ bundrate + "/通讯端口已经连接!");
				
				String m_info = getATInfo();
				if(m_info==null)
				{
					return DMU_setCSCA(sms_center_num);
				}
				if(setCSMS(sms_center_num))
				{
					if(getDevice_type() !=DEVICE_DMU)
					{
						boolean sms_ready = loadCSMS();
						while(!sms_ready)
						{
							
							Thread.sleep(10*1000);
							sms_ready = loadCSMS();
						}
					}
					if(setMessageMode(this.textmode))
					{
						logger.debug(this.dev_name +"/模式"+ this.textmode + "/通讯端口已配置成功!");
						return true;
					}
					else
					{
						logger.error(this.dev_name + "设置默认模式为："+this.textmode+"失败!!");
						return false;
					}
					
				}
			} else {
				logger.error(this.dev_name + "通讯端口连接失败!");
			}	
		}
		return false;
	}
	
	public void closePort()
	{
		if(port!=null)
		{
			port.close();
			flag=false;
			port=null;
		}
	}
	
	private String parseATInfo(String info)
	{
		//WH_GM35
		//SIM800
		//SIM808
		if(strReturn.indexOf("WH_GM35", 0) != -1)
		{
			setDevice_type(DEVICE_GM35);
			device_info = "WH_GM35";
		}
		else if(strReturn.indexOf("SIM800", 0) != -1)
		{
			setDevice_type(DEVICE_SIM800);
			device_info = "SIM800";
		}
		else  if(strReturn.indexOf("SIM808", 0) != -1)
		{
			setDevice_type(DEVICE_SIM808);
			device_info = "SIM800";
		}
		else
		{
			setDevice_type(DEVICE_UNKNOWN);
			device_info = info;// "UNKNOW";
		}
		return device_info;
	}
	
	public String getdevice_info()
	{
		return device_info;
	}
	
	private String getATInfo()
	{
		checkATMode();
		try {
			atCommand = "ATI"+ String.valueOf(symbol1);
			strReturn = port.sendAT(atCommand);
			if(strReturn.indexOf("OK", 0) != -1)
			{
				String meta_info = strReturn.substring(0,strReturn.indexOf("OK", 0));
				logger.debug("AT模块厂家信息："+meta_info);
				return parseATInfo(meta_info);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private void checkATMode()
	{
		
		try {
			atCommand = "AT"+ String.valueOf(symbol1);
			strReturn = port.sendAT(atCommand);
			if (strReturn.indexOf("OK", 0) == -1)//not in ATmode
			{
				byte[] cancel_cmd = {0x1B};
				strReturn = port.sendATByte(cancel_cmd,300);
				EnterATMode();

			}
			atCommand = "AT+CSCS=\"GSM\"";
			strReturn = port.sendAT(atCommand);//返回默认编码
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
	}
	
	public void EnterATMode()
	{
		atCommand = "+++";//EBYTE进入AT模式
		try {
			strReturn = port.sendATNoCr(atCommand);
		} catch (RemoteException e) {
			e.printStackTrace();
		}		
	}
	
	public void ExitATMode()//only for EBYTE
	{
		atCommand = "ATO";//EBYTE退出AT模式
		try {
			strReturn = port.sendAT(atCommand);
		} catch (RemoteException e) {
			e.printStackTrace();
		}		
	}
	
	//专用短信模块只能设置中心号码，发短信，无法查询中心号码
	/*
	 * AT+BAUD=115200设置波特率（暂不用，默认9600）
	 * AT+CSCA=8613010314500 设置短信中心（号码根据实际，样例为上海联通的中心号码）返回AT+CSCA OK
	 * 86xxxxxxxxx:短信内容测试
	 */
	private boolean DMU_setCSCA(String sms_center_num)
	{
		if(sms_center_num==null||sms_center_num.length()==0) return false;
		try {
			if(sms_center_num.indexOf('+')!=-1)//号码去除+
			{
				sms_center_num = sms_center_num.substring(sms_center_num.indexOf('+')+1);
			}
			
			atCommand = "AT+CSCA="+ sms_center_num + String.valueOf(symbol1)+String.valueOf(symbolCr);
			logger.debug("DATA:"+ByteUtil.byte2HexString(atCommand.getBytes()));
			Thread.sleep(1000);
			strReturn = port.sendATNoCr(atCommand,10*1000);
			if (strReturn.indexOf("AT+CSCA OK", 0) != -1)
			{
				this.now_smsc = sms_center_num;
				if(DEVICE_NOSET==this.getDevice_type())
				{
					this.setDevice_type(DEVICE_DMU);
					this.device_info = "DMU";
					flag = true;
					logger.info("设置DMU短信中心成功，当前中心号码："+this.now_smsc);
				}
				return true;
			}
		} catch (RemoteException | InterruptedException e) {
			e.printStackTrace();
		}
		return false;
	}
	 // 86xxxxxxxxx:短信内容测试
	private boolean DMU_sendSMS(String phoneCode, String msg)
	{
		try {
		if(phoneCode.indexOf('+')!=-1)
		{
			phoneCode = phoneCode.substring(phoneCode.indexOf('+')+1);
		}
		atCommand = phoneCode+":"+msg+ String.valueOf(symbol1)+String.valueOf(symbolCr);
		byte[] bytes_send = atCommand.getBytes("GBK");
		strReturn = port.sendATByte(bytes_send,5000);//等待10秒
		if (strReturn.indexOf("Send OK", 0) != -1)
		{
			logger.info("短信发送成功...");
			return true;
		}
		else if (strReturn.indexOf("Send fail", 0) != -1)
		{
			logger.info("短信发送失败...");
		}
		} catch (RemoteException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		logger.info("短信发送超时...");
		 return false;
	}
	
	//AT+CSCS?
	//+CSCS: "PCCP936"
	//
	//OK
	public String getATtextMode()
	{
		String text_coding=null;
		try {
			atCommand = "AT+CSCS?";
			strReturn = port.sendAT(atCommand);
			if (strReturn.indexOf("OK", 0) != -1&&strReturn.indexOf("+CSCS:", 0) != -1)
			{
				text_coding = strReturn.substring(strReturn.indexOf("+CSCS:", 0)+6,strReturn.indexOf("OK", 0) );
				if(text_coding.indexOf('\"')!=-1)
				{
					text_coding = text_coding.substring(text_coding.indexOf('\"')+1);
					if(text_coding.indexOf('\"')!=-1)
					{
						text_coding = text_coding.substring(0,text_coding.indexOf('\"')+1).trim();
						return text_coding;
					}
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}
	//PCCP936 ->GBK
	//UCS2 -> unicode
	//为了在兼容在7位char传输中文，以上中文编码要转换成hex_string格式传输。
	public boolean setATtextMode(String mode)
	{

		try {
			atCommand = "AT+CSCS=\""+mode+"\"";
			strReturn = port.sendAT(atCommand);
			if (strReturn.indexOf("OK", 0) == -1)
			{
				logger.error("短信设置编码失败...");
				return false;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		return true;
	}

	/**
	 * 发送短信
	 * 
	 * @param phoneCode
	 *            目标号码
	 * @param msg
	 *            短信内容
	 * */
	public boolean sendSMS(String phoneCode, String msg) {
		if (flag == false) {
			logger.error("***************************************");
			logger.error("COM通讯端口未正常打开");
			logger.error("***************************************");
			return false;
		}
		if(getDevice_type()==DEVICE_NOSET)
		{
			logger.error("***************************************");
			logger.error("未检测到任何设备");
			logger.error("***************************************");
			return false;
		}
		if(getDevice_type()==DEVICE_DMU)
		{
			logger.debug("检测到DMU设备,使用DMU命令发送短信");
			return DMU_sendSMS(phoneCode,msg);
		}
		//空格
		//char symbol2 = 34;
		//ctrl~z  发出短信指令
		char symbol3 = 26;		
		try {
			
			checkATMode();
			getMsgMode();
			if(this.textmode == TEXTMODE)
			{

				atCommand = "AT+CSCS=\"UCS2\"";//设置后所有的参数字符串编码都要使用该编码传输（电话号码和短信内容）注意初始化是必须设置为默认GSM，否则模块返回的内容有可能为该编码
				strReturn = port.sendAT(atCommand);
				if (strReturn.indexOf("OK", 0) == -1)
				{
					logger.error("短信设置编码失败...");
					return false;
				}
				atCommand = "AT+CSMP=17,167,0,8" + String.valueOf(symbol1);
				//atCommand = "AT"+ String.valueOf(symbol1);
				strReturn = port.sendAT(atCommand);
				System.out.println(strReturn);
				if (strReturn.indexOf("OK", 0) != -1) {
					if(getDevice_type()==DEVICE_GM35)//GM35默认使用GBK编码，即便设置其他编码无法生效
					{
						atCommand = "AT+CMGS=" + "\""+ phoneCode + "\""+ String.valueOf(symbol1);
					}
					else
					{
						atCommand = "AT+CMGS=" + "\""+ UCS2.EncodeUCS2(phoneCode.trim()) + "\""+ String.valueOf(symbol1);
					}
					strReturn = port.sendAT(atCommand);
					if (strReturn.indexOf(">", 0) != -1)//safe wait > for input data
					{
						if(getDevice_type()==DEVICE_GM35)
						{
							byte[] gbk_byte = msg.getBytes("GBK");
							strReturn = port.sendATByte(gbk_byte,100);
						}
						else
						{
							atCommand = UCS2.EncodeUCS2(msg.trim());//+ String.valueOf(symbol1);
							//byte [] data_bytes = msg.getBytes("UTF-16BE");
							//atCommand = StringUtil.byte2HexString(data_bytes);
							strReturn = port.sendAT(atCommand);
							//strReturn = port.sendATByte(data_bytes,300);
							//atCommand = String.valueOf(symbol3);
						}

						byte[] byte_datas = new byte[1];
						byte_datas[0]= (byte)0x1A;
						strReturn = port.sendATByte(byte_datas,15*1000);//15秒
						if (strReturn.indexOf("OK") != -1 && strReturn.indexOf("+CMGS")!= -1) {
							logger.info("短信发送成功...");
							atCommand = "AT+CSCS=\"GSM\"";
							strReturn = port.sendAT(atCommand);
							return true;
						}
					}
					atCommand = "AT+CSCS=\"GSM\"";
					strReturn = port.sendAT(atCommand);//返回默认编码
				}
			}
			else
			{
				atCommand = "AT+CSCS=\"GSM\"";//设置后所有的参数字符串编码都要使用该编码传输（电话号码和短信内容）
				strReturn = port.sendAT(atCommand);
				if (strReturn.indexOf("OK", 0) == -1)
				{
					logger.error("短信设置编码失败...");
					//return false;
				}
				//add get SMSCenter Number by AT+CSCS? and use PduCodec pack info and data
				PduCodec coder = new PduCodec();
				
				String pdudata = coder.Encode(this.now_smsc,phoneCode,msg.trim());
				//计算实际数据区的原始数据大小（除smsc中心以外的内容）
				int context_len = (pdudata.length()-coder.encodeSMSC(this.now_smsc).length())/2;
				
				atCommand = "AT+CMGS=" + context_len + String.valueOf(symbol1);
				strReturn = port.sendAT(atCommand);
				if (strReturn.indexOf(">", 0) != -1)//safe wait > for input data
				{
					atCommand = pdudata;
					strReturn = port.sendATNoCr(atCommand);
					atCommand = String.valueOf(symbol3);
					byte[] byte_datas = new byte[1];
					byte_datas[0]= (byte)symbol3;
					strReturn = port.sendATByte(byte_datas,15000);
					if (strReturn.indexOf("OK") != -1 && strReturn.indexOf("+CMGS")!= -1) {
						atCommand = "AT+CSCS=\"GSM\"";
						strReturn = port.sendAT(atCommand);
						logger.info("短信发送成功...");
						return true;
					}
				}
				atCommand = "AT+CSCS=\"GSM\"";
				strReturn = port.sendAT(atCommand);
				
				
			}
		} catch (Exception ex) {

			ex.printStackTrace();
			logger.error("短信发送失败...");
			return false;
		}
		logger.error("短信发送失败...");
		return false;
	}
	//AT+CSCA?
	// 设置消息模式 0-pdu 1-text(默认1 文本方式 )
	public boolean setMessageMode(int op) {

		try {
			
			checkATMode();			
			atCommand = "AT"+ String.valueOf(symbol1);
			strReturn = port.sendAT(atCommand);
			if (strReturn.indexOf("OK", 0) != -1) {
				atCommand = "ATE0"+ String.valueOf(symbol1);
				strReturn = port.sendAT(atCommand);
				if (strReturn.indexOf("OK", 0) != -1)
				{
					logger.debug("已禁用AT命令回显");
				}
				atCommand = "AT+CSCS=?";
				strReturn = port.sendAT(atCommand);

				atCommand = "AT+CSCS=\"GSM\"";
				strReturn = port.sendAT(atCommand);
				if (strReturn.indexOf("OK", 0) != -1)
				{
					logger.debug("已设置编码模式为默认GSM");
				}
				else
				{
					logger.error("不支持编码设置：[" +strReturn+"]");
					if(op!=PDUMODE)
					{
						op = PDUMODE;//可能模块仅支持PDU模式，因此不能设置成PDU以外的模式
						logger.info("由于可能模块不支持切换编码，因此使用默认的模式："+op);
					}
					
				}
				atCommand = "AT+CMGF=" + String.valueOf(op)+ String.valueOf(symbol1);
				strReturn = port.sendAT(atCommand);
				if (strReturn.indexOf("OK", 0) != -1) {
					flag = true;
					logger.info("设置短信模式成功。当前模式："+op);
					return true;
				}
			}
			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
	//AT+CMGF?
//	+CMGF: 0
//
//	OK
	public int getMsgMode()
	{
		
		try {
			atCommand = "AT+CMGF?"+ String.valueOf(symbol1);
			strReturn = port.sendAT(atCommand);
			if (strReturn.indexOf("OK", 0) != -1&&strReturn.indexOf("+CMGF:",0)!=-1) {
				String miode_string = strReturn.substring(strReturn.indexOf("+CMGF:",0)+6, strReturn.indexOf("OK", 0));
				 int mode = Integer.parseInt(miode_string.trim());
				 if(mode==1)
				 {
					 this.textmode = TEXTMODE;
				 }
				 else
				 {
					 this.textmode = PDUMODE;
				 }
				logger.info("获取短信模式成功。当前模式："+this.textmode);
			}	
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return this.textmode;
	}
	
	//AT+CSMS=<number>
	//AT+CSCA?
	/*
	 * +CSCA: "+8613800210500",145
	 * 
	 * OK
	 */
	public boolean loadCSMS()
	{
		
		try {
			checkATMode();
			atCommand = "AT+CSCA?"+ String.valueOf(symbol1);
			strReturn = port.sendAT(atCommand);
			if (strReturn.indexOf("OK", 0) != -1&&strReturn.indexOf("+CSCA:", 0) != -1)
			{
				String csms_line = strReturn.substring(strReturn.indexOf("+CSCA:")+6,strReturn.indexOf("OK")).trim();
				if(csms_line.indexOf('\"')!=-1&&csms_line.indexOf('+')!=-1)
				{
					csms_line = csms_line.substring(csms_line.indexOf('\"')+1,csms_line.length());
					csms_line = csms_line.substring(0,csms_line.indexOf('\"'));
					this.now_smsc = csms_line.substring(csms_line.indexOf('+'),csms_line.length());
					return true;
				}
				else
				{
					logger.error("data:["+csms_line+"]error!");
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}		
		
		return false;
	}
	public boolean setCSMS(String sms_center_num)
	{
		if(sms_center_num==null||sms_center_num.length()==0) return true;//允许空的中心号码
		try {
			checkATMode();
			atCommand = "AT+CSCA=\""+sms_center_num+"\""+ String.valueOf(symbol1);
			strReturn = port.sendAT(atCommand);
			if (strReturn.indexOf("OK", 0) != -1)
			{
				this.now_smsc = sms_center_num;
				logger.info("设置短信中心成功，当前中心号码："+this.now_smsc);
				return true;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 读取短信
	 * @param index 短信存储的位置
	 * @return Message对象
	 * */
	public Message readSMS(String index) {
		Message mes = null;
		if (flag == false) {
			logger.error("System Message:  COM通讯端口未正常打开");
			return mes;
		}
		try {
			atCommand = "AT+CMGR=" + index;
			strReturn = port.sendAT(atCommand);
			if (strReturn.indexOf("OK") != -1) {
				mes = Message.analyseSMS(strReturn, index);
				logger.info("短信位置:" + mes.getAddID());
				logger.info("短信状态:" + mes.getState());
				logger.info("对方号码:" + mes.getPhone());
				logger.info("短信内容:" + mes.getMessage());
				logger.info("发送时间:" + mes.getTime());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return mes;
	}

	// 短信存储状况
	// SIM 卡可保存25 条短消息，现有短消息11 条
	public String getReportSMS() {
		if (flag == false) {
			logger.error("System Message:  COM通讯端口未正常打开");
			return "";
		}
		try {
			atCommand = "AT+CPMS?";
			strReturn = port.sendAT(atCommand);
			logger.info("CPMS?应答："+strReturn);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return strReturn;
	}

	/**
	 *  删除短信
	 * 
	 * @param index 短信存储的位置
	 * @return 成功/失败
	 * */
	public boolean deleteSMS(String index) {
		if (flag == false) {
			logger.error("System Message:  COM通讯端口未正常打开");
			return false;
		}
		try {
			logger.info("System Operate:  正在删除存储位置 + index	 " + " 的短信.....");
			atCommand = "AT+CMGD=" + index;
			strReturn = port.sendAT(atCommand);
			if (strReturn.indexOf("OK") != -1) {
				logger.info("System Message:  成功删除存储位置 + index	" + "的短信.....");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return true;
	}	

	/**
	 *  获取所有短信
	 * 
	 * @return 返回获取的列表数据
	 * */
	public List<Message> readAllSMS() {
		List<Message> listMes = readSMS(Message.MSG_STATE_ALL);
		if (listMes != null && listMes.size() > 0) {

			System.out.println("***************************************");
			System.out.println("System Message:  读取" + listMes.size() + "条短信");
			System.out.println("***************************************");
			Message mes = null;
			for (int i = 0; i < listMes.size(); i++) {
				mes = (Message) listMes.get(i);
				System.out.println("短信位置:" + mes.getAddID());
				System.out.println("短信状态:" + mes.getState());
				System.out.println("对方号码:" + mes.getPhone());
				System.out.println("短信内容:" + mes.getMessage());
				System.out.println("接收时间:" + mes.getTime());
				if(i+1 < listMes.size())
				{
					System.out.println("***************************************");
				}
			}
			System.out.println("***************************************");
		}
		else
		{
			System.out.println("***************************************");
			System.out.println("System Message: 暂无 短信");
			System.out.println("***************************************");			
		}
		return listMes;
	}
	/**
	 *  获取未读短信
	 * 
	 * @return 返回获取的列表数据
	 * */
	public List<Message> readUnreadSMS() {
		List<Message> listMes = readSMS(Message.MSG_STATE_UNREAD);
		if (listMes != null && listMes.size() > 0) {

			System.out.println("***************************************");
			System.out.println("System Message:  读取" + listMes.size() + "条未读短信");
			System.out.println("***************************************");
			Message mes = null;
			for (int i = 0; i < listMes.size(); i++) {
				mes = (Message) listMes.get(i);
				System.out.println("短信位置:" + mes.getAddID());
				System.out.println("短信状态:" + mes.getState());
				System.out.println("对方号码:" + mes.getPhone());
				System.out.println("短信内容:" + mes.getMessage());
				System.out.println("接收时间:" + mes.getTime());
				if(i+1 < listMes.size())
				{
					System.out.println("***************************************");
				}
			}
			System.out.println("***************************************");
		}
		else
		{
			System.out.println("***************************************");
			System.out.println("System Message: 暂无 未读短信");
			System.out.println("***************************************");			
		}
		return listMes;
	}
	/**
	 *  获取已读短信
	 * 
	 * @return 返回获取的列表数据
	 * */
	public List<Message> readReadedSMS() {
		List<Message> listMes = readSMS(Message.MSG_STATE_READED);
		if (listMes != null && listMes.size() > 0) {

			System.out.println("***************************************");
			System.out.println("System Message:  读取" + listMes.size() + "条未读短信");
			System.out.println("***************************************");
			Message mes = null;
			for (int i = 0; i < listMes.size(); i++) {
				mes = (Message) listMes.get(i);
				System.out.println("短信位置:" + mes.getAddID());
				System.out.println("短信状态:" + mes.getState());
				System.out.println("对方号码:" + mes.getPhone());
				System.out.println("短信内容:" + mes.getMessage());
				System.out.println("接收时间:" + mes.getTime());
				if(i+1 < listMes.size())
				{
					System.out.println("***************************************");
				}
			}
			System.out.println("***************************************");
		}
		else
		{
			System.out.println("***************************************");
			System.out.println("System Message: 暂无 已读短信");
			System.out.println("***************************************");			
		}
		return listMes;
	}
	private List<Message> readSMS(int state_type) {
		if (flag == false) {
			logger.error("System Message:  COM通讯端口未正常打开");
			return null;
		}
		if(state_type<0||state_type>4) return null;
		checkATMode();
		getMsgMode();
		List<Message> listMes = new ArrayList<Message>();
		try {

			if(this.textmode == TEXTMODE)
			{
				atCommand = "AT+CSCS=\"UCS2\"";//设置后所有的参数字符串编码都要使用该编码传输（电话号码和短信内容）注意初始化是必须设置为默认GSM，否则模块返回的内容有可能为该编码
				strReturn = port.sendAT(atCommand);
				if (strReturn.indexOf("OK", 0) == -1)
				{
					logger.error("短信设置编码失败...");
					return null;
				}
				if(state_type==Message.MSG_STATE_READED)
				{
					atCommand = "AT+CMGL=\""+Message.MSG_NAME_READED+"\"";
				}
				else if(state_type==Message.MSG_STATE_UNREAD)
				{
					atCommand = "AT+CMGL=\""+Message.MSG_NAME_UNREAD+"\"";
				}
				else
				{
					atCommand = "AT+CMGL=\""+Message.MSG_NAME_ALL+"\"";
				}
			}
			else
			{
				atCommand = "AT+CSCS=\"GSM\"";//设置后所有的参数字符串编码都要使用该编码传输（电话号码和短信内容）注意初始化是必须设置为默认GSM，否则模块返回的内容有可能为该编码
				strReturn = port.sendAT(atCommand);
				if (strReturn.indexOf("OK", 0) == -1)
				{
					logger.error("短信设置编码失败...");
					return null;
				}
				atCommand = "AT+CMGL="+state_type;
			}
			strReturn = port.sendAT(atCommand);
			if(this.textmode == TEXTMODE)
			{
				listMes = Message.analyseArraySMS(strReturn);
			}
			else
			{
				//add analyseArrayPDUSMS for pdu utit
				listMes = Message.analyseArrayPDUSMS(strReturn);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return listMes;
	}
	
	public void getNotSendSMS()
	{
		if (flag == false) {
			logger.error("System Message:  COM通讯端口未正常打开");
			return;
		}
		//STO UNSENT
		try {
			atCommand = "AT+CMGL=\"STO UNSENT\"";
			strReturn = port.sendAT(atCommand);
			if (strReturn.indexOf("OK") != -1)
			{
				
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}

	public  Port getPort() {
		return port;
	}

	public void setPort(Port port) {
		this.port = port;
	}
	/**
	 * @return device_type
	 */
	public int getDevice_type() {
		return device_type;
	}
	/**
	 * 设置设备类型
	 * @param device_type 要设置的类型
	 */
	public void setDevice_type(int device_type) {
		this.device_type = device_type;
	}
}
