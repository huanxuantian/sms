/**
 * 
 */
package com.shinki.sms;

import java.io.UnsupportedEncodingException;

import com.shinki.utils.ByteUtil;

/**
 * @author shinki
 *
 */
//根据需要在不断完善当中 更新时间：2010-10-20 14:12
//已实现 7bit,16bit编码，7bit,16bit解码
//采用了http://commons.apache.org/codec/ 提供的开发包解析 hex string to byte[]
//PDU串的用户信息长度(TP-UDL)，在各种编码方式下意义有所不同。
//7-bit编码时，指原始短消息的字符个数，而不是编码后的字节数。
//8-bit编码时，就是字节数。UCS2编码时，也是字节数，等于原始短消息的字符数的两倍。

//AT+CMGF=0
//OK
//AT+CMGL=4
//+CMGL: 1,1,,64
//0891683110102105F06405A10110F000080101905103342330050003010202002E00310030003000310030002E0063006F006DFF0C5145503C0039002E0038003562984F1860E0FF01
//+CMGL: 2,1,,152
//0891683110102105F06405A10110F000080101905103842388050003010201622A6B62523000320030003100305E740031003067080030003965E5FF0C60A876845E1062374F59989D4E3A00320030002E003700305143FF0C53D19001201C0043005A00235145503C53615BC67801201D81F300310030003000310030537353EF5145503CFF0C6216767B5F558054901A7F514E0A84254E1A5385007700770077

public class PduCodec {
	// 7bit-160，8bit-140，16bit-70
	private String SCA = "00"; 
	// Service Center Adress [1-12] 服务中心的电话号码
	private String SMSC = "";//"+019000001";为空时可省缺
	// private byte PduType;
	// Protocol Data Unit Type [1] 协议数据单元类型
	private String PduTypeHex = "31";
	// private String MR = "00"; 
	// Message Reference [1] 所有成功的 SMS-SUBMIT
	// 参考数目(0..255)
	public String OA; 
	// Originator Adress [2-12] 发送方 SME 的地址
	// private String DA; 
	// Destination Adress [2-12] 接收方 SME 的地址
	// private String PID = "00"; 
	// Protocol Identifier [1] 参数显示 SMSC以何种方式处理SM
	// (比如FAX,、Voice 等)
	private byte DCS; 
	// Data Coding Scheme [1] 参数表示用户数据(UD)采用什么编码方案
	private String DcsHex = "00"; 
	// Data Coding Scheme [1] 参数表示用户数据(UD)采用什么编码方案
	public String SCTS; 
	// Service Center Time Stamp [7] 参数表示
	// SMSC接收到消息时的时间戳
	// private String VP; 
	// Validity Period [0,1,7] 参数表示消息在 SMSC中不再有效的时长
	// private String UDL; 
	// User Data Length [1] 用户数据段长度
	// private String UD; 
	// User Data [0-140] SM数据
	public String TimeZone;
	private boolean GSM = false; // GSM 压缩
	private byte Alphabet = 0; // 字母表
	
	private String system_smsc = null;
	int textlenght=0;
	
	public String UD_Str = null;
	
 
	// private byte ValidBits[] = { 0, 0x7F, 0x3F, 0x1F, 0x0F, 0x07, 0x03, 0x01 };
 
	// 00 – 默认的字母表，每个字符占用 7 比特位，此时最大可以传送 160 字符
	// 01 – 8bit，此时最大只能传送 140 个字符
	// 10 – USC2（16bit）， 传送双字节字符集
	public static void main(String[] args) {
		PduCodec pc = new PduCodec();
		// String src =
		// "0891683108802505F22405800180F60008013022710413236A60A876847F514E0A5BA2670D4E1A52A153D7740662168BE6535567E58BE276844E8C6B21927467435BC678014E3AFF1A003500380035003600390039FF0C003200305206949F518553EF4EE54F7F75288BE55BC67801529E74064E1A52A1621667E58BE28BE653553002";
		// String src =
		// "07911326040000F0040B911346610089F60000208062917314080CC8F71D14969741F977FD07";
		// System.out.println(pc.Decode(src));
//		短信中心设置错误！
//		0011000A8110299232900008AA0E4E2D658777ED4FE1606F7F167801
		//注意编码和解码的内容是不对称的，详见《PDU格式短信解析.docx》
		pc.SMSC = "+8613010314500";
		System.out.println(pc.Encode("+8615821672256", "请确认该短信"));
		System.out.println(pc.Decode("0891683110304105F0240D91685128612752F60008912122210230230C8BF7786E8BA48BE577ED4FE1"));
	}
 
	private String decodeSMSC(String data) {// 读取短信中心号码
		String SCA_Len = data.substring(0, 2);
		System.out.println("sca_len=" + SCA_Len);
		String SCA_Type = data.substring(2, 4);
		System.out.println("sca_type=" + SCA_Type);
		int sca_len = Integer.parseInt(SCA_Len, 16);
		int sca_eidx = (sca_len + 1) * 2;
		String sca_str = data.substring(0, sca_eidx);
		StringBuilder sb = new StringBuilder();
		if (SCA_Type.equals("91")) {
			sb.append("+");
			sca_len--;
		}
		for (int i = 0; i < sca_len * 2; i += 2) {
			sb.append(sca_str.charAt(i + 5));
			char sca_tmp = sca_str.charAt(i + 4);
			if ('F' != sca_tmp) {
				sb.append(sca_tmp);
			}
		}
		System.out.println("SMSC#" + sb.toString());
		System.out.println("src:=" + sca_str);
		int data_len = data.length();
		return data.substring(sca_eidx, data_len);
	}
 
	private String decodePduType(String data) {
		int PduType_eidx = 2;
		PduTypeHex = data.substring(0, PduType_eidx);
		System.out.println("PduType:" + PduTypeHex);
		System.out.println("src:=" + PduTypeHex);
		int data_len = data.length();
		return data.substring(PduType_eidx, data_len);
	}
 
	private String decodeSME(String data, String addr) {
		String SME_Len = data.substring(0, 2);
		System.out.println("sme_len=" + SME_Len);
		String SME_Type = data.substring(2, 4);
		System.out.println("sme_type=" + SME_Type);
 
		int sme_len = Integer.parseInt(SME_Len, 16);
		if (0 != sme_len % 2) {
			sme_len++;
		}
		int sme_eidx = sme_len + 4;
		StringBuilder sb = new StringBuilder();
		if (SME_Type.equals("91")) {
			sb.append("+");
		}
		for (int i = 0; i < sme_len; i += 2) {
			sb.append(data.charAt(i + 5));
			char tmp = data.charAt(i + 4);
			if ('F' != tmp) {
				sb.append(tmp);
			}
		}
		OA = sb.toString();
		System.out.println(addr + OA);
 
		String SME_str = data.substring(0, sme_eidx);
		System.out.println("src:=" + SME_str);
 
		int data_len = data.length();
		return data.substring(sme_eidx, data_len);
 
	}
 
	private String decodePID(String data) {
		int pid_eidx = 2;
		String PID_Str = data.substring(0, pid_eidx);
		System.out.println("TP_PID:" + PID_Str);
		System.out.println("src:=" + PID_Str);
		int data_len = data.length();
		return data.substring(pid_eidx, data_len);
	}
 
	private String decodeDCS(String data) {
		int dcs_eidx = 2;
		DcsHex = data.substring(0, dcs_eidx);
		DCS = (byte) Integer.parseInt(DcsHex, 16);
		System.out.println("TP_DCS:" + DcsHex);
		GSM = (DCS & 0x20) == 0x20;
		if (GSM) {
			System.out.println("文本用GSM标准压缩算法压缩");
		} else {
			System.out.println("文本未压缩");
		}
		if (((DCS & 0x08) != 0x08) && ((DCS & 0x04) != 0x04)) {
			System.out.println("Alphabet = 7bit");
			Alphabet = 0;
		} else if (((DCS & 0x08) != 0x08) && ((DCS & 0x04) == 0x04)) {
			System.out.println("Alphabet = 8bit");
			Alphabet = 1;
		} else if (((DCS & 0x08) == 0x08) && ((DCS & 0x04) != 0x04)) {
			System.out.println("Alphabet = 16bit");
			Alphabet = 2;
		} else {
			System.out.println("Alphabet = unknow");
		}
 
		if (((DCS & 0x02) != 0x02) && ((DCS & 0x01) != 0x01)) {
			System.out.println("Class 0 Immediate display");
		} else if (((DCS & 0x02) != 0x02) && ((DCS & 0x01) == 0x01)) {
			System.out.println("Class 1 ME specific");
		} else if (((DCS & 0x02) == 0x02) && ((DCS & 0x01) != 0x01)) {
			System.out.println("Class 2 SIM specific");
		} else if (((DCS & 0x02) == 0x02) && ((DCS & 0x01) == 0x01)) {
			System.out.println("Class 3 TE specific");
		} else {
			System.out.println("Class unknow");
		}
 
		System.out.println("src:=" + DcsHex);
		int data_len = data.length();
		return data.substring(dcs_eidx, data_len);
	}
 
	private String decodeSCTS(String data) {
		int vpf_eidx = 0;
		int vp_eidx = 14;
		String VP_Str = data.substring(vpf_eidx, vp_eidx);
		SCTS = String.format("20%c%c-%c%c-%c%c %c%c:%c%c:%c%c",
				VP_Str.charAt(1), VP_Str.charAt(0), VP_Str.charAt(3),
				VP_Str.charAt(2), VP_Str.charAt(5), VP_Str.charAt(4),
				VP_Str.charAt(7), VP_Str.charAt(6), VP_Str.charAt(9),
				VP_Str.charAt(8), VP_Str.charAt(11), VP_Str.charAt(10));
		System.out.println("TimeStamp:" + SCTS);
		String tmp2 = String.format("%c%c", VP_Str.charAt(13),
				VP_Str.charAt(12));
		System.out.println("Time Zone:" + tmp2);
		TimeZone = tmp2;
		System.out.println("src:=" + VP_Str);
		int data_len = data.length();
		return data.substring(vp_eidx, data_len);
	}
 
	private String decodeUD(String data) {
		int udl_eidx = 2;
		String UDL_Str = data.substring(0, udl_eidx);
		int char_len = Integer.parseInt(UDL_Str, 16);
		int byte_len = char_len;
		System.out.println("udl_len=" + char_len);
		String UD_HexStr = data.substring(udl_eidx, udl_eidx + char_len * 2);
		
		if (0 == Alphabet) {
			StringBuilder sb = new StringBuilder();
			byte[] ud_array1 = new byte[char_len];
			try {
				ud_array1 = ByteUtil.HexString2Byte(UD_HexStr);
				int j = char_len * 7;
				byte_len = j / 8;
				if (0 != j % 8) {
					byte_len++;
				}
				int a0 = 0, a1, a2;
				for (int i = 0; i < byte_len; i++) {
					j = i % 7;
					a1 = ud_array1[i] & 0xFF;
					a2 = ((a1 << j) & 0x7F) | a0;
					sb.append((char) a2);
					if (6 != j) {
						a0 = ((a1 >> (7 - j)) & 0x7F);
					} else {
						a2 = ((a1 >> 1) & 0x7F);
						sb.append((char) a2);
						a0 = 0;
					}
				}
			} catch (/*DecoderException*/ Exception e) {
				e.printStackTrace();
			}
			UD_Str = sb.toString();
		} else if (1 == Alphabet) {
			UD_Str = "还未处理的8位解码";
		} else if (2 == Alphabet) {
			byte[] ud_array = new byte[char_len];
			try {
				ud_array = ByteUtil.HexString2Byte(UD_HexStr);
				UD_Str = new String(ud_array, "UTF-16");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		System.out.println("ud_str:=" + UD_Str);
		int data_len = data.length();
		System.out.println("src:=" + data.substring(udl_eidx, data_len));
		String str = "time:" + SCTS + " from:" + OA + " content:" + UD_Str;
		return str;
	}
 
	public String encodeSMSC(String sCA) {// 设置短信中心号码
	// Len[1] + Type[0-1] + Addr[0-10]
	// Type 含符号"+"国际0x91, 国内0x81
	// Len = Type + Addr 字节和
		if ((null != sCA) && (0 < sCA.length())) {
			String SCA_Type;
			String SCA_Addr0;
			StringBuilder SCA_Addr1 = new StringBuilder();
			if (sCA.startsWith("+")) {
				SCA_Type = "91";
				SCA_Addr0 = sCA.substring(1);
			} else {
				SCA_Type = "81";
				SCA_Addr0 = sCA;
			}
			if (0 != SCA_Addr0.length() % 2) {
				SCA_Addr0 += "F";
			}
			String SCA_Len = String.format("%02X", 1 + SCA_Addr0.length() / 2);
			for (int i = 0; i < SCA_Addr0.length(); i += 2) {
				SCA_Addr1.append(SCA_Addr0.charAt(i + 1));
				SCA_Addr1.append(SCA_Addr0.charAt(i));
			}
			SCA = SCA_Len + SCA_Type + SCA_Addr1.toString();
			return SCA;
		} else {
			System.out.println("短信中心设置错误！");
			SCA ="00";//省缺值不是所有都能设置为空，建议填入正确的号码
			return SCA;
		}
	}
 
	public String encodePduType(String Pdu) {
		return Pdu;
	}
 
	public String encodeMR(String mR) {
		return mR;
	}
 
	public String encodePID(String pID) {
		return pID;
	}
 
	private String encodeDA(String sME) {
		// Len[1] + Type[0-1] + Addr[0-10]
		// Len：地址长度。指 8613851724908 的字符个数长度。这与 SCA中的定义不一样！
		// Type 含符号"+"国际0x91, 国内0x81
		// Len = Type + Addr 字节和
		String SME_Len = "00";
		String SME_Type = "00";
		String SME_Addr0;
		StringBuilder SME_Addr1 = new StringBuilder();
		if ((null != sME) && (0 < sME.length())) {
			if (sME.startsWith("+")) {
				SME_Type = "91";
				SME_Addr0 = sME.substring(1);
			} else if (sME.startsWith("106")) {// 小灵通
				SME_Type = "A1";
				SME_Addr0 = sME;
			} else {
				SME_Type = "81";
				SME_Addr0 = sME;
			}
			SME_Len = String.format("%02X", SME_Addr0.length());
			if (0 != SME_Addr0.length() % 2) {
				SME_Addr0 += "F";
			}
			for (int i = 0; i < SME_Addr0.length(); i += 2) {
				SME_Addr1.append(SME_Addr0.charAt(i + 1));
				SME_Addr1.append(SME_Addr0.charAt(i));
			}
		} else {
			System.out.println("设置SME地址错误！请输入正确的SME地址");
		}
		return SME_Len + SME_Type + SME_Addr1.toString();
	}
 
	public String encodeDCS(String dCS) {
		return dCS;
	}
 
	public String encodeVP(String vP) {
		return vP;
	}
 
	public String encodeUD(String uD) {
		int sLen = uD.length();
		String tmp1 = String.format("%02X", sLen);
		String tmp2 = null;
		byte[] src = uD.getBytes();
		if (uD.length() == uD.getBytes().length) {
			int j = sLen * 7;
			int dLen = j / 8;
			if (0 != j % 8) {
				dLen++;
			}
			byte[] dst = new byte[dLen];
			int sIdx = 0, dIdx = 0, BitsLeft = 0, BitsFill;
			while (dIdx < dLen) {
				if (0 < BitsLeft) {
					BitsFill = (src[sIdx] << (8 - BitsLeft)) & 0xFF;// 要前移的 bits
					dst[dIdx - 1] = (byte) (dst[dIdx - 1] | BitsFill);// 前移数据到前一个字节
					dst[dIdx] = (byte) (src[sIdx] >>> BitsLeft);// 前移数据后的字节
					if (6 == BitsLeft) {
						dst[dIdx] |= (byte) (src[++sIdx] << 1);
						BitsLeft = 0;
					} else {
						BitsLeft++;
					}
					dIdx++;
					sIdx++;
				} else {
					dst[dIdx++] = (byte) (src[sIdx++] & 0x7F);
					BitsLeft = 1;
				}
			}
			dIdx = 0;
			while (dIdx < dLen) {
				tmp2 = String.format("%02X", dst[dIdx++]);// 已经充满的字节
				tmp1 += tmp2;
			}
		} else {
			int dLen = sLen * 2;
			byte[] dst = new byte[dLen];
			try {
				tmp1 = String.format("%02X", dLen);
				byte[] tmp = uD.getBytes("UTF-16");
				System.arraycopy(tmp, 2, dst, 0, dLen);
				tmp1 += ByteUtil.byte2HexString(dst);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return tmp1;
	}
 
	public String Decode(String data) {
		System.out.println(data);
		String tmp = decodeSMSC(data);
		tmp = decodePduType(tmp);
		tmp = decodeSME(tmp, "Sender:");
		tmp = decodePID(tmp);
		tmp = decodeDCS(tmp);
		tmp = decodeSCTS(tmp);
		tmp = decodeUD(tmp);
		return tmp;
	}
	public int getTextLenght()
	{
		return textlenght;
	}
	public String Encode(String smsc,String Tel, String Data) {
		String tmp = encodeSMSC(smsc);
		int head_offset = tmp.length();
		tmp += encodePduType("11");//17中文，16进制x11
		tmp += encodeMR("00");//发送方号码，发出短信时可填空内容的长度0，16进制x00
		tmp += encodeDA(Tel);
		tmp += encodePID("00");
		if (Data.length() == Data.getBytes().length) {
			tmp += encodeDCS("00");// 00:7bit编码
		} else {
			tmp += encodeDCS("08");// 00:16bit编码
		}
		
		tmp += encodeVP("AA");// AA 有效期四天
		tmp += encodeUD(Data);
		textlenght= tmp.length()-head_offset;
		return tmp;
	}
	public String Encode(String Tel, String Data) {
		String tmp = encodeSMSC(SMSC);
		int head_offset = tmp.length();
		tmp += encodePduType("11");
		tmp += encodeMR("00");
		tmp += encodeDA(Tel);
		tmp += encodePID("00");
		if (Data.length() == Data.getBytes().length) {
			tmp += encodeDCS("00");// 00:7bit编码
		} else {
			tmp += encodeDCS("08");// 00:16bit编码
		}
		tmp += encodeVP("AA");// AA 有效期四天
		tmp += encodeUD(Data);
		textlenght= tmp.length()-head_offset;
		return tmp;
	}
	public void setSystemSmsc(String smsc)
	{
		this.system_smsc = smsc;
	}
	public String EncodeD(String Tel, String Data) {
		if(system_smsc==null) return null;
			
		String tmp = encodeSMSC(system_smsc);
		int head_offset = tmp.length();
		tmp += encodePduType("11");
		tmp += encodeMR("00");
		tmp += encodeDA(Tel);
		tmp += encodePID("00");
		if (Data.length() == Data.getBytes().length) {
			tmp += encodeDCS("00");// 00:7bit编码
		} else {
			tmp += encodeDCS("08");// 00:16bit编码
		}
		tmp += encodeVP("AA");// AA 有效期四天
		tmp += encodeUD(Data);
		textlenght= tmp.length()-head_offset;
		return tmp;
	}
	public String EncodeWithoutSmsc(String Tel, String Data)
	{
		String tmp ="";
		int head_offset = tmp.length();
		tmp += encodePduType("11");
		tmp += encodeMR("00");
		tmp += encodeDA(Tel);
		tmp += encodePID("00");
		if (Data.length() == Data.getBytes().length) {
			tmp += encodeDCS("00");// 00:7bit编码
		} else {
			tmp += encodeDCS("08");// 00:16bit编码
		}
		tmp += encodeVP("AA");// AA 有效期四天
		tmp += encodeUD(Data);
		textlenght= tmp.length()-head_offset;
		return tmp;		
	}
}
