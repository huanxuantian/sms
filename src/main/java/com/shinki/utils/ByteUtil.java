package com.shinki.utils;

import java.nio.charset.Charset;

/**
 * Created by wenyu on 16-11-21.
 */
public class ByteUtil {
	public static byte[] getBytes(short data) {
		byte[] bytes = new byte[2];
		bytes[0] = (byte) (data & 0xff);
		bytes[1] = (byte) ((data & 0xff00) >> 8);
		return bytes;
	}

	public static byte[] getBytes(char data) {
		byte[] bytes = new byte[2];
		bytes[0] = (byte) (data);
		bytes[1] = (byte) (data >> 8);
		return bytes;
	}

	public static byte[] getBytes(int data) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) (data & 0xff);
		bytes[1] = (byte) ((data & 0xff00) >> 8);
		bytes[2] = (byte) ((data & 0xff0000) >> 16);
		bytes[3] = (byte) ((data & 0xff000000) >> 24);
		return bytes;
	}

	public static byte[] getBytes(long data) {
		byte[] bytes = new byte[8];
		bytes[0] = (byte) (data & 0xff);
		bytes[1] = (byte) ((data >> 8) & 0xff);
		bytes[2] = (byte) ((data >> 16) & 0xff);
		bytes[3] = (byte) ((data >> 24) & 0xff);
		bytes[4] = (byte) ((data >> 32) & 0xff);
		bytes[5] = (byte) ((data >> 40) & 0xff);
		bytes[6] = (byte) ((data >> 48) & 0xff);
		bytes[7] = (byte) ((data >> 56) & 0xff);
		return bytes;
	}

	public static byte[] getBytes(float data) {
		int intBits = Float.floatToIntBits(data);
		return getBytes(intBits);
	}

	public static byte[] getBytes(double data) {
		long intBits = Double.doubleToLongBits(data);
		return getBytes(intBits);
	}
	
	public static byte[] getBytes(String data, String charsetName) {
		if(charsetName==null||charsetName.length()==0)
			return data.getBytes();
		Charset charset = Charset.forName(charsetName);
		return data.getBytes(charset);
	}

	public static byte[] getBytes(String data) {
		return getBytes(data, "GBK");
	}

	public static short getShort(byte[] bytes) {
		return (short) ((0xff & bytes[0]) | (0xff00 & (bytes[1] << 8)));
	}

	public static char getChar(byte[] bytes) {
		return (char) ((0xff & bytes[0]) | (0xff00 & (bytes[1] << 8)));
	}

	public static int getInt(byte[] bytes) {
		return (0xff & bytes[0]) | (0xff00 & (bytes[1] << 8)) | (0xff0000 & (bytes[2] << 16))
				| (0xff000000 & (bytes[3] << 24));
	}
	public static int getIntBig(byte[] bytes) {
		return (0xff & bytes[3]) | (0xff00 & (bytes[2] << 8)) | (0xff0000 & (bytes[1] << 16))
				| (0xff000000 & (bytes[0] << 24));
	}

	public static long getLong(byte[] bytes) {
		return (0xffL & (long) bytes[0]) | (0xff00L & ((long) bytes[1] << 8)) | (0xff0000L & ((long) bytes[2] << 16))
				| (0xff000000L & ((long) bytes[3] << 24)) | (0xff00000000L & ((long) bytes[4] << 32))
				| (0xff0000000000L & ((long) bytes[5] << 40)) | (0xff000000000000L & ((long) bytes[6] << 48))
				| (0xff00000000000000L & ((long) bytes[7] << 56));
	}

	public static float getFloat(byte[] bytes) {
		return Float.intBitsToFloat(getInt(bytes));
	}

	public static float getFloatBig(byte[] bytes) {
		return Float.intBitsToFloat(getIntBig(bytes));
	}

	public static double getDouble(byte[] bytes) {
		long l = getLong(bytes);
		// System.out.println(l);
		return Double.longBitsToDouble(l);
	}

	public static String getString(byte[] bytes, String charsetName) {
		return new String(bytes, Charset.forName(charsetName));
	}

	public static String getString(byte[] bytes) {
		return getString(bytes, "GBK");
	}

	public static byte[] subBytes(byte[] src, int begin, int count) {
		byte[] bs = new byte[count];
		System.arraycopy(src, begin, bs, 0, count>src.length?src.length:count);
		return bs;
	}

	public static byte[] addBytes(byte[] data1, byte[] data2) {
		byte[] data3 = new byte[data1.length + data2.length];
		System.arraycopy(data1, 0, data3, 0, data1.length);
		System.arraycopy(data2, 0, data3, data1.length, data2.length);
		return data3;
	}
	
	public static String byte2HexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
	}
	public static byte[] HexString2Byte(String str)
	{
		return HexString2Byte(str,null);
	}
	public static byte[] HexString2Byte(String str,boolean left_zero){
		return HexString2Byte(str,null,left_zero);
	}
	public static byte[] HexString2Byte(String str ,String charsetName) {
		return HexString2Byte(str,charsetName,false);
	}
	public static byte[] HexString2Byte(String str ,String charsetName,boolean left_zero) {
		int size = (str.length() / 2) + (str.length() % 2);
		byte[] bytes = new byte[size];
		byte[] str_bytes =null;
		if(left_zero&&(str.length() % 2)>0)
		{
			str_bytes= getBytes("0"+str,charsetName);
		}
		else
		{
			str_bytes = getBytes(str,charsetName);
		}
		int j = 0;
		for (int i = 0; j <= size;) {
			if (i + 1 > str_bytes.length)
				break;
			if (str_bytes[i] >= '0' && str_bytes[i] <= '9') {
				bytes[j] = (byte) ((byte) ((str_bytes[i] - '0') & 0x0F) << 4);
			} else if ((str_bytes[i] >= 'a' && str_bytes[i] <= 'f')) {
				bytes[j] = (byte) ((byte) ((str_bytes[i] - 'a' + 10) & 0x0F) << 4);
			} else if (str_bytes[i] >= 'A' && str_bytes[i] <= 'F') {
				bytes[j] = (byte) ((byte) ((str_bytes[i] - 'A' + 10) & 0x0F) << 4);
			} else {
				i++;
				continue;
			}

			if (i + 1 >= str_bytes.length) {
				if (bytes[j] != 0)
					j++;
				break;
			}
			if (str_bytes[i + 1] >= '0' && str_bytes[i + 1] <= '9') {
				bytes[j] = (byte) (bytes[j] + (byte) ((str_bytes[i + 1] - '0') & 0x0F));
			} else if ((str_bytes[i + 1] >= 'a' && str_bytes[i + 1] <= 'f')) {
				bytes[j] = (byte) (bytes[j] + (byte) ((str_bytes[i + 1] - 'a' + 10) & 0x0F));
			} else if (str_bytes[i + 1] >= 'A' && str_bytes[i + 1] <= 'F') {
				bytes[j] = (byte) (bytes[j] + (byte) ((str_bytes[i + 1] - 'A' + 10) & 0x0F));
			} else {
				i++;
				continue;
			}
			j++;
			i += 2;
		}
		if (j != size) {
			byte[] sub_bytes = new byte[j];
			System.arraycopy(bytes, 0, sub_bytes, 0, j);
			return sub_bytes;
		} else {
			return bytes;
		}
	}
	
	public static String byte2HexPrintString(byte[] b) {
		String ret = "";
		if (b == null)
			return ret;
		for (int i = 0; i < b.length; i++) {
			if(b.length>16&&i%16==0)	ret = ret+'\n';
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			ret = ret + hex.toUpperCase();
			
		}
		return ret;
	}
	//transcode{org_0,org1,tran,tran_0,tran_1,tran_tran}
	//
	public static byte[] byteUnEscape(byte[] src,int offset,byte[] transcode)
	{
		if(src==null||src.length<=offset) return src;
		if(transcode==null||transcode.length<4) return src;
		byte[] dest=new byte[offset+2*(src.length-offset)];

		System.arraycopy(src, 0, dest, 0, offset);
		int desc_offset=offset;
		//byte last_byte=(byte)(~(transcode[2]&0xFF));

		for(int i=offset;i<src.length-1;i++)
		{
			if(src[i]==transcode[2]&&src[i+1]==transcode[3])
			{
				dest[desc_offset]=transcode[0];
				desc_offset++;
				i++;
			}
			else if(src[i]==transcode[2]&&src[i+1]==transcode[4])
			{
				dest[desc_offset]=transcode[1];
				desc_offset++;
				i++;
			}
			else if(src[i]==transcode[2]&&src[i+1]==transcode[5])
			{
				dest[desc_offset]=transcode[2];
				desc_offset++;
				i++;
			}
			else{
				dest[desc_offset]=src[i];
			}
		}
		System.arraycopy(dest, 0, dest, 0, desc_offset);

		return dest;
	}

	public static byte[] byteEscape(byte[] src,int offset,byte[] transcode)
	{
		if(src==null||src.length<=offset) return src;
		if(transcode==null||transcode.length<4) return src;
		byte[] dest=new byte[src.length];

		System.arraycopy(src, 0, dest, 0, offset);
		int desc_offset=offset;

		for(int i=offset;i<src.length;i++)
		{
			if(src[i]==transcode[0])
			{
				dest[desc_offset]=transcode[2];
				desc_offset++;
				dest[desc_offset]=transcode[3];
				desc_offset++;
			}
			else if(src[i]==transcode[1])
			{
				dest[desc_offset]=transcode[2];
				desc_offset++;
				dest[desc_offset]=transcode[4];
				desc_offset++;
			}
			else if(src[i]==transcode[2])
			{
				dest[desc_offset]=transcode[2];
				desc_offset++;
				dest[desc_offset]=transcode[5];
				desc_offset++;
			}
			else{
				dest[desc_offset]=src[i];
			}
		}
		System.arraycopy(dest, 0, dest, 0, desc_offset);

		return dest;
	}

}
