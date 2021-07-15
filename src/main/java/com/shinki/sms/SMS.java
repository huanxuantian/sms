package com.shinki.sms;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SMS extends Thread{
	
	public static final long TIMEOUT= 120*1000;//ms
	public static final int MSGQUEUESIZE=20;
	
	private static Logger logger = LoggerFactory.getLogger(SMS.class);
	
	private SmsModule module=null;
	private int msg_mode = SmsModule.PDUMODE;
	private boolean SMS_ready=false;
	private String center_number=null;
	private SmsEventListener listener=null;
	private ArrayBlockingQueue<Message> msgQuete = new ArrayBlockingQueue<Message>(MSGQUEUESIZE);
	public static void main(String[] args) throws Exception{
		String msg;
		if(args.length<2) 
		{
			logger.error("you must give a portname and traget phone number for test!");
			logger.error("<exec> <portname> <phone>");
			return;
		}
		String portname = args[0];
		String phone = args[1];

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
		String date_str =df.format(new Date());
		
		msg = "您好，当前时间："+ date_str +"，如果你收到了，表示短信发送成功。";
		
		//int msg_mode = SmsModule.PDUMODE;
		
		//String center_number ="+8613010314500";
		
		SMS testSms = new SMS(portname,115200);
		testSms.setSmsEventListener(new SmsListener());
		//testSms.changeSMSMode(msg_mode);
		testSms.start();
		//msg = msg+ "来自设备："+testSms.module.getdevice_info();
		
		testSms.SendSMS(phone, msg);
		while(true)
		{
			logger.info("waiting for sms!");
			Thread.sleep(60*1000);
			if(testSms.isQueueEmpty())
			{
				logger.info("test finish!");
				break;
			}
			
		}
	}
	
	public SMS()
	{
		
	}
	
	public SMS(String comName)
	{
		init(comName,115200,null);
	}
	
	public SMS(String comName,String center_number)
	{
		init(comName,115200,center_number);
	}
	
	public SMS(String comName, int bundrate)
	{
		init(comName, bundrate,null);
	}

	public SMS(String comName, int bundrate,int Databit,int Stopbit,int Parity)
	{
		if(init(comName, bundrate,null))
		{
			module.setPortParam(comName, bundrate, Databit, Stopbit, Parity);
		}
	}
	
	public SMS(String comName, int bundrate,String center_number)
	{
		init(comName, bundrate,center_number);
	}
	
	public boolean init(String comName, int bundrate)
	{
		return init(comName,bundrate,null);
	}
	
	public void setSmsEventListener(SmsEventListener smsListener)
	{
		this.listener = smsListener;
	}
	
	protected boolean init(String comName, int bundrate,String center_number)
	{
		if(module==null)
		{
			module = new SmsModule(comName,bundrate,msg_mode);
			this.center_number = center_number;
		}
		else
		{
			module.closePort();
			module.setParam(comName,bundrate,msg_mode);
		}
		return (module==null);
	}
	
	public SmsModule getModule() {
		return module;
	}

	public void setModule(SmsModule module) {
		this.module = module;
	}
	public void closeModule()
	{
		over();
		if(this.module!=null)
		{
			
			this.module.closePort();
			this.module=null;
			SMS_ready = false;
		}
	}
	protected boolean startup(String center_number)
	{
		if(module==null) return false;
		boolean init_ok=false;
		try {
			init_ok = module.startUp(center_number);
			long start_tick=System.currentTimeMillis();
			while(!init_ok)
			{
				Thread.sleep(5*1000);
				if(module!=null)
				{
					init_ok = module.setmode(msg_mode);
					if(init_ok&&module.getDevice_type() !=SmsModule.DEVICE_DMU)
					{
						module.loadCSMS();
					}
				}
				else
				{
					break;
				}
				if(System.currentTimeMillis()-start_tick>TIMEOUT)
				{
					//timeout
					break;
				}

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(init_ok)
		{
			SMS_ready =true;
			if(this.listener!=null)
			{
				Message msg = new Message();
				msg.setMessage("module name:["+ this.module.device_info+"]");
				SmsEvent event = new SmsEvent(this.module,SmsEvent.SMS_READY,false,true,-1,msg);//not ready -> ready
				this.listener.smsEvent(event);
			}
		}
		return init_ok;
	}
	
	public boolean changeSMScenterNum(String center_number) throws InterruptedException
	{
		if(!SMS_ready) return false;
		return module.init_Port(center_number);
	}
	
	public boolean changeSMSMode(int mode)
	{
		if(!SMS_ready) return false;
		if(mode!=SmsModule.TEXTMODE)
		{
			return module.setmode(SmsModule.PDUMODE);
		}
		return module.setmode(SmsModule.TEXTMODE);
	}
	
	public boolean IsSMSReady()
	{
		return SMS_ready;
	}
	public boolean isQueueEmpty()
	{
		return (msgQuete.size()==0);
	}
	
 @Override
	public void run() {
		if (this.module == null) {
			over();
			return;
		}
		long sTag = System.currentTimeMillis();
		long lastchecktime =sTag;
		if(!this.IsSMSReady())
		{
			this.SMS_ready = startup(this.center_number);
		}
		for (;;) {
			if(module==null)
			{
				over(); 
				return;
			}
			if(System.currentTimeMillis() - lastchecktime>30*1000L)
			{
				lastchecktime = System.currentTimeMillis();
				if(!this.IsSMSReady())
				{
					this.SMS_ready = startup(this.center_number);
				}
			}
			if (System.currentTimeMillis() - sTag >= 1000L) {

				sTag = System.currentTimeMillis();
				while(this.IsSMSReady()&&msgQuete.size()>0)
				{
					Message mes =msgQuete.poll();
					if(mes!=null)
					{
						String phone = mes.getPhone();
						String message = mes.getMessage();
						if(phone==null||phone.length()==0) continue;
						if(message==null||message.length()==0) continue;
						try {
							boolean send_ok=module.sendSMS(phone,message);
							Thread.sleep(100);
							if(!send_ok) 
							{
								if(mes.decMsgewsend()>0)
								{
									msgQuete.offer(mes);//reserve
									if(this.listener!=null)
									{
										SmsEvent event = new SmsEvent(this.module,SmsEvent.SMS_NEEDRESEND,false,false,mes.getTimetick(),mes);
										this.listener.smsEvent(event);
									}
								}
								else{
									if(this.listener!=null)
									{
										SmsEvent event = new SmsEvent(this.module,SmsEvent.SMS_NOMORETRY,false,false,mes.getTimetick(),mes);
										this.listener.smsEvent(event);
									}
								}
							}
							else
							{
								if(this.listener!=null)
								{
									SmsEvent event = new SmsEvent(this.module,SmsEvent.SMS_SENDED,false,true,mes.getTimetick(),mes);
									this.listener.smsEvent(event);
								}
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public void over() {

		if(this.module!=null)
		{
			if(this.listener!=null)
			{
				SmsEvent event = new SmsEvent(this.module,SmsEvent.SMS_ERROR,false,false,-1);
				this.listener.smsEvent(event);
			}
			this.module.closePort();
			this.module=null;
			SMS_ready = false;
		}
		interrupt();
	}
	
	public long SendSMS(String phone,String msg)
	{
		Message mes = new Message();
		mes.setPhone(phone);
		mes.setMessage(msg);
		mes.setTimetick(System.currentTimeMillis());
		if(msgQuete.offer(mes))
		{
			return mes.getTimetick();
		}
		return -1;
	}
	
	public class SmsListener implements SmsEventListener{
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
	
}
