# sms
sms library for java project use purejavacomm with serial modem


example:
---
1. import sms class
```
import com.shinki.sms;
```
2.config and start sms
```
SMS smsport = new SMS(portname,115200);	
smsport.start();
```
3.try to send msg
```
smsport.SendSMS(phone, msgtext);
```
note:
---
now msgtext must short to 140byte base on only one sms message.