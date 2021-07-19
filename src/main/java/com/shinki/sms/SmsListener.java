package com.shinki.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmsListener implements SmsEventListener{
	private static Logger logger = LoggerFactory.getLogger(SmsListener.class);
	@Override
	public void smsEvent(SmsEvent event)
	{
		Message msg= event.getEventMsg();
		long id = event.getEventSubid();
		if(msg!=null)
		{
			if(id>0)
			{
				logger.info("event for msg:"+msg);
			}
			else{
				logger.info("event msg:"+msg.getMessage());
			}
		}

		switch (event.getEventType()) {
		case SmsEvent.SMS_READY:
			logger.info("sms ready in case");
			break;
		case SmsEvent.SMS_SENDED:
			logger.info("sms send finish");
			break;
		case SmsEvent.SMS_NEEDRESEND:
			logger.info("sms send failed,retry again.");
			break;
		case SmsEvent.SMS_ERROR:
			logger.info("sms exit/error.");
			break;
		case SmsEvent.SMS_NOMORETRY:
			logger.info("sms send failed,nomore again.");
			break;
		}
	}
}
