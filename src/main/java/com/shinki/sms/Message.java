package com.shinki.sms;

import java.util.ArrayList;
import java.util.List;

public class Message {
	public static final int MSG_TYPE_OUT =0x00;
	public static final int MSG_TYPE_IN =0x01;
	public static final int MSG_RESENDTIME=3;
	
	public static final int MSG_STATE_READED =0x01;
	public static final int MSG_STATE_UNREAD =0x00;
	public static final int MSG_STATE_ALL =0x04;
	public static final int MSG_STATE_UNKNOWN =-1;
	
	public static final String MSG_NAME_READED ="REC READ";
	public static final String MSG_NAME_UNREAD ="REC UNREAD";
	public static final String MSG_NAME_ALL ="ALL";
	
	private int msgType=MSG_TYPE_OUT;
	private int msgState=MSG_STATE_UNREAD;
	private int msgresend=MSG_RESENDTIME;
	//地址ID
	private String addID;
	//对方号码
	private String phone;
	//短信中心
	private String smsc;
	//消息状态
	private String state;
	//消息内容
	private String message;
	
	private String time;
	
	private long timetick;

	public int getMsgresend() {
		return msgresend;
	}
	public int decMsgewsend()
	{
		if(this.msgresend>0)
		{
			this.msgresend= this.msgresend-1;
		}
		return this.msgresend;
	}
	public void setMsgresend(int msgresend) {
		this.msgresend = msgresend;
	}
	/**
	 * @return timetick
	 */
	public long getTimetick() {
		return timetick;
	}
	/**
	 * @param timetick 要设置的 timetick
	 */
	public void setTimetick(long timetick) {
		this.timetick = timetick;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getSmsc() {
		return smsc;
	}
	public void setSmsc(String smsc) {
		this.smsc = smsc;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public String getAddID() {
		return addID;
	}
	public void setAddID(String addID) {
		this.addID = addID;
	}
	/**
	 * @return msgType
	 */
	public int getMsgType() {
		return msgType;
	}
	/**
	 * @param msgType 要设置的 msgType
	 */
	public void setMsgType(int msgType) {
		this.msgType = msgType;
	}
	/**
	 * @return msgState
	 */
	public int getMsgState() {
		return msgState;
	}
	/**
	 * @param msgState 要设置的 msgState
	 */
	public void setMsgState(int msgState) {
		this.msgState = msgState;
	}
	//短信处理的公共静态函数
	public static boolean phoneVilad(String phone)
	{
		
		if(phone==null||phone.length()==0) return false;
		int plus_offset = phone.indexOf('+');
		if(plus_offset==-1)
		{
			for (int i = 0; i < phone.length(); i++) {
				if (!Character.isDigit(phone.charAt(i))) {
				return false;
				}
			}
		}
		else if(plus_offset==0)
		{
			for (int i = 1; i < phone.length(); i++) {
				if (!Character.isDigit(phone.charAt(i))) {
				return false;
				}
			}			
		}
		else
		{
			return false;
		}
		
		return true;
	}
	/**
	 * 使用SMSModule 的readSMS(String index)的方法时，使用该方法解析MODEM返回的字符串
	 * 根据MODEM返回的字符串，解析成Message对象
	 * 
	 * @param str 待解析的原始应答字符串
	 * @param index 用于标识消息的序号
	 * @return 解析完后的消息对象
	 * */
	public static Message analyseSMS(String str,String index) {
		Message mes = new Message();
		String mesContent;

		String[] s = str.split("\"");

		int len = s.length;
		mes.setAddID(index);
		mesContent = s[len - 1];
		if(mesContent.indexOf("OK")!=-1){
			mesContent = mesContent.substring(0, mesContent.indexOf("OK"));
		}
		mesContent = mesContent.trim();
		mes.setMessage(mesContent);
		mes.setTime(s[len - 2]);
		if (s[1].equals("REC READ")) {
			mes.setState("已读");
		} else {
			mes.setState("未读");
		}

		mes.setPhone(s[3]);

		return mes;
	}
	/**
	 * 使用SMSModule 的readAllSMS(String index)方法时，通过该方法解析MODEM返回来的字符�?
	 * 根据MODEM返回的字符串，解析成Message的集合对
	 * 
	 * @param str 待解析的原始应答字符串
	 * @return 返回解析到的消息列表
	 * */
	public static List<Message> analyseArraySMS(String str) {
		List<Message> mesList = new ArrayList<Message>();
		Message mes;
		String[] messages;
		String temp;
		String[] t;
		if(str.indexOf("CMGL: ")==-1)
			return null;
		str = str.substring(0,str.indexOf("OK")).trim();
		messages = str.split("\r\n");
		if(messages.length<2)
			return null;
		for(int i=0;i<messages.length;i++){
			mes = new Message();
			if(messages[i].length()==0) continue;
			System.out.println("massage_data:"+messages[i]+"msg:"+messages[i]+1);
			messages[i] = messages[i].substring(messages[i].indexOf("CMGL: ")+6);
			t = messages[i].split(",");
			if(t.length>=5){
			mes.setAddID(t[0].trim());
			mes.setMsgType(Message.MSG_TYPE_IN);
			temp = t[1].substring(t[1].indexOf('"')+1,t[1].lastIndexOf('"')).trim();
			if(temp.equals(Message.MSG_NAME_READED)){
				mes.setState("已读");
				mes.setMsgState(Message.MSG_STATE_READED);
			}else if(temp.equals(Message.MSG_NAME_UNREAD)){
				mes.setState("未读");
				mes.setMsgState(Message.MSG_STATE_UNREAD);
			}
			else
			{
				mes.setState("未知（"+temp+")");
				mes.setMsgState(Message.MSG_STATE_UNKNOWN);
			}
			mes.setPhone(t[2].substring(t[2].indexOf('"')+1,t[2].lastIndexOf('"')).trim());
			mes.setTime(t[4].substring(t[4].indexOf('"')+1)+" "+t[5].substring(0,t[5].indexOf('"')));
			if(i>=messages.length) break;
			i++;
			mes.setMessage(analyseStr(messages[i].trim()));
			mesList.add(mes);
			}
		}
		return mesList;
	}

	/**
	 * 使用SMSModule 的readAllSMS(String index)方法时，通过该方法解析MODEM返回来的PDU编码短信格式
	 * 根据MODEM返回的字符串，解析成Message的集合对
	 *  +CMGL: 3,1,"",32
	 * 0891683110304105F0240D91685128612752F60008912122210230230C8BF7786E8BA48BE577ED4FE1
	 * 
	 * @param str 待解析的原始应答字符串
	 * @return 返回解析到的消息列表
	 * */
	public static List<Message> analyseArrayPDUSMS(String str) {
		List<Message> mesList = new ArrayList<Message>();
		Message mes;
		String[] messages;
		String temp;
		String[] t;
		if(str.indexOf("CMGL: ")==-1)
			return null;
		str = str.substring(0,str.indexOf("OK")).trim();
		messages = str.split("\r\n");
		if(messages.length<2)
			return null;
		PduCodec pc = new PduCodec();
		for(int i=0;i<messages.length;i++){
			mes = new Message();
			if(messages[i].length()==0) continue;
			System.out.println("massage_data:"+messages[i]+"msg:"+messages[i]+1);
			messages[i] = messages[i].substring(messages[i].indexOf("CMGL: ")+6);
			t = messages[i].split(",");
			if(t.length>=4){
			mes.setMsgType(Message.MSG_TYPE_IN);
			mes.setAddID(t[0].trim());
			temp = t[1];
			if(temp.equals("1")){
				mes.setState("已读");
				mes.setMsgState(Message.MSG_STATE_READED);
			}else if(temp.equals("0")){
				mes.setState("未读");
				mes.setMsgState(Message.MSG_STATE_UNREAD);
			}
			else
			{
				mes.setState("未知（"+temp+")");
				mes.setMsgState(Message.MSG_STATE_UNKNOWN);
			}

			if(i>=messages.length) break;
			i++;
			pc.Decode(messages[i].trim());
			mes.setPhone(pc.OA.trim());
			mes.setTime(pc.SCTS.trim()+"+"+pc.TimeZone);
			mes.setMessage(pc.UD_Str);
			mesList.add(mes);
			}
		}
		return mesList;
	}

	/**
	 * 将PDU编码的十六进制字符串 如4F60597DFF01转换成unicode "\u4F60\u597D\uFF01"
	 * 
	 * @param str 待转码的原始字符串
	 * @return 转码完成的字符串
	 * */
	public static String analyseStr(String str) {
		StringBuffer sb = new StringBuffer();
		if (!(str.length() % 4 == 0))
			return str;
		for (int i = 0; i < str.length(); i++) {
			if (i == 0 || i % 4 == 0) {
				sb.append("\\u");
			}
			sb.append(str.charAt(i));
		}
		return Unicode2GBK(sb.toString());
	}

	/**
	 * 将unicode编码 "\u4F60\u597D\uFF01" 转换成中文"你好吗“
	 * 
	 * @param dataStr 待转码的字符串
	 * @return 转码完成的字符串
	 * */
	public static String Unicode2GBK(String dataStr) {
		int index = 0;
		StringBuffer buffer = new StringBuffer();
		while (index < dataStr.length()) {
			if (!"\\u".equals(dataStr.substring(index, index + 2))) {
				buffer.append(dataStr.charAt(index));
				index++;
				continue;
			}
			String charStr = "";
			charStr = dataStr.substring(index + 2, index + 6);
			char letter = (char) Integer.parseInt(charStr, 16);
			buffer.append(letter);
			index += 6;
		}
		return buffer.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder();
		sb.append("SMS:[");
		sb.append("tel:"+this.phone+",");
		sb.append("msg:"+this.message+".");
		sb.append("]");
		return sb.toString();
	}

}
