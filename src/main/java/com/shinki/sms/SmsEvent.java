package com.shinki.sms;

import java.util.EventObject;

public class SmsEvent extends EventObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final int SMS_READY = 1;
	
	public static final int SMS_SENDED = 2;
	
	public static final int SMS_NEEDRESEND = 3;
	
	public static final int SMS_ERROR = 4;

	public static final int SMS_NOMORETRY = 5;
	
	private final int eventType;
	private final boolean newValue;
	private final boolean oldValue;
	private long event_subid;
	private Message msg=null;
	
	/**
	 * Constructs a <CODE>SmsEvent</CODE> with the specified smsmodule,
	 * event type, old and new values. Application programs should not directly
	 * create <CODE>SmsEvent</CODE> objects.
	 * 
	 * @param source 事件的发起者
	 * @param eventType 事件类型
	 * @param oldValue 事件的前一个状态
	 * @param newValue 事件当前的状态
	 * @param event_subid 事件所属的事件子ID
	 * @param Message 消息本体
	 */
	public SmsEvent(SmsModule source, int eventType, boolean oldValue, boolean newValue,long event_subid,Message msg) {
		super(source);
		this.eventType = eventType;
		this.newValue = newValue;
		this.oldValue = oldValue;
		this.event_subid = event_subid;
		this.msg = msg;
	}
	public SmsEvent(SmsModule source, int eventType, boolean oldValue, boolean newValue,long event_subid) {
		super(source);
		this.eventType = eventType;
		this.newValue = newValue;
		this.oldValue = oldValue;
		this.event_subid = event_subid;
	}

	
	
	/**
	 * Returns the type of this event.
	 * 
	 * @return The type of this event.
	 */
	public int getEventType() {
		return this.eventType;
	}

	/**
	 * Returns the new value of the state change that caused the
	 * <CODE>SerialPortEvent</CODE> to be propagated.
	 * 
	 * @return The new value of the state change.
	 */
	public boolean getNewValue() {
		return this.newValue;
	}

	/**
	 * Returns the old value of the state change that caused the
	 * <CODE>SerialPortEvent</CODE> to be propagated.
	 * 
	 * @return The old value of the state change.
	 */
	public boolean getOldValue() {
		return this.oldValue;
	}

	/**
	 * @return event_subid
	 */
	public long getEventSubid() {
		return event_subid;
	}

	/**
	 * @return Message
	 */
	public Message getEventMsg() {
		return msg;
	}

}
