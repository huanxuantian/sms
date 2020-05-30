package com.shinki.sms;

import java.util.EventListener;

public interface SmsEventListener extends EventListener {
	public void smsEvent(SmsEvent event);
}
