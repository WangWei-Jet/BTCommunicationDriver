package com.oneway.bt.controller;

import android.os.Build;
import android.util.Log;

public class Utils {
	public static String HardwareVer = "UNKNOWN";

	private static boolean fixWave = true;

	public static String bytesToHexString(byte[] src) {
		return bytesToHexString(src, src.length);
	}

	public static String bytesToHexString(byte[] src, int len) {
		String tag = "bytesToHexString(byte[], int";

		if (src == null || src.length <= 0 || len <= 0 || len > src.length) {
			Log.e(tag, "Illegal arguments!");
			return null;
		}

		StringBuilder stringBuilder = new StringBuilder("");
		int tempInt = 0;
		String tempStr = "";

		for (int i = 0; i < len; i++) {
			tempInt = src[i] & 0xFF;
			tempStr = Integer.toHexString(tempInt);

			if (tempStr.length() < 2) {
				stringBuilder.append(0);
			}

			stringBuilder.append(tempStr);
		}

		return stringBuilder.toString();
	}

	public static byte[] stringToBCDByte(String src) {
		return stringToBCDByte(src, src.length());
	}

	public static byte[] stringToBCDByte(String src, int len) {
		String tag = "stringToBCDByte(String, int)";

		if (src == null || src.length() == 00 || len % 2 != 0) {
			Log.e(tag, "Illegal arguments!");
			return null;
		}

		byte[] byteArrayResult = new byte[len / 2];

		for (int i = 0; i < len - 1; i += 2) {
			try {
				byteArrayResult[i / 2] = (byte) Integer.parseInt(
						src.substring(i, i + 2), 16);
			} catch (NumberFormatException e) {
				Log.e(tag,
						"The String argument contains chars which isn't hex String! "
								+ "Check the arguments you've passed!");

				byteArrayResult = null;
				break;
			}

			i += 2;
		}

		return byteArrayResult;
	}

	/**
	 * 将16进制组成的字符串转换成byte数组 例如 hex2Byte("0710BE8716FB"); 将返回一个byte数组
	 * b[0]=0x07;b[1]=0x10;...b[5]=0xFB;
	 * 
	 * @param src
	 *            待转换的16进制字符串
	 * @return
	 */
	public static byte[] str2bytes(String src) {
		if (src == null || src.length() == 0 || src.length() % 2 != 0) {
			return null;
		}
		int nSrcLen = src.length();
		byte byteArrayResult[] = new byte[nSrcLen / 2];
		StringBuffer strBufTemp = new StringBuffer(src);
		String strTemp;
		int i = 0;
		while (i < strBufTemp.length() - 1) {
			strTemp = src.substring(i, i + 2);
			byteArrayResult[i / 2] = (byte) Integer.parseInt(strTemp, 16);
			i += 2;
		}
		return byteArrayResult;
	}

	public static String bytesToString(byte[] src) {
		if (src == null || src.length <= 0) {
			return null;
		}
		StringBuffer sBuffer = new StringBuffer();
		for (int i = 0; i < src.length; i++) {
			sBuffer.append((char) src[i]);
		}
		return sBuffer.toString();
	}

	public static short getShort(byte argB1, byte argB2) {
		return (short) ((0xff & argB1) | (argB2 << 8));
	}

	/**
	 * int转换为网络字节序。
	 * 
	 * @param n
	 * @return
	 */
	public static byte[] intToBytes(int n) {
		byte[] b = new byte[4];
		b[3] = (byte) (n & 0xff);
		b[2] = (byte) (n >> 8 & 0xff);
		b[1] = (byte) (n >> 16 & 0xff);
		b[0] = (byte) (n >> 24 & 0xff);
		return b;
	}

	/**
	 * 网络字节序转换为int。
	 * 
	 * @param b
	 * @return
	 */
	public static int bytesToInt(byte b[]) {
		return b[3] & 0xff | (b[2] & 0xff) << 8 | (b[1] & 0xff) << 16
				| (b[0] & 0xff) << 24;
	}

	public static int bytesToInt(byte[] b, int len) {
		String tag = "bytesToInt(byte[], int)";

		if (b == null || b.length == 0 || len <= 0 || len > 4) {
			Log.e(tag, "Illegal arguments!");
			return -1;
		}

		byte[] validByte = new byte[4];

		for (int i = 0; i < 3; i++) {
			if (len > i) {
				validByte[3 - i] = b[len - 1 - i];
			} else {
				validByte[3 - i] = 0;
			}
		}

		return validByte[3] & 0xff | (validByte[2] & 0xff) << 8
				| (validByte[1] & 0xff) << 16 | (validByte[0] & 0xff) << 24;
	}

	/**
	 * 专为<b>非测试界面</b>制定录音波形文件路径，包括设置界面、查询界面、命令界面。
	 * 
	 * @param level2
	 *            二级目录名。
	 * @param level3
	 *            文件名。
	 * @return 路径。
	 */
	public static String createRecordFilePathNotForTest(String level2,
			String level3) {
		String manufacturer = Build.MANUFACTURER;
		String phoneModel = Build.MODEL;
		String phoneMsg = phoneModel.replace(" ", "%");
		String dovilaModel = Utils.HardwareVer;
		String dovilaMsg = dovilaModel.replace(" ", "%");

		String level1 = manufacturer + "_" + phoneMsg + "/" + dovilaMsg;

		String filePath = level1 + "/" + level2 + "/" + level3;

		return filePath;
	}

	/**
	 * 修复波形
	 * 
	 * @param src
	 *            需要修复的波形源数据
	 * @param start
	 *            修复起始点
	 * @param end
	 *            修复结束点
	 */
	public static void fixWave(short[] src, int start, int end) {
		if (Utils.fixWave) {
			for (int i = start; i < end - 1; i++) {
				src[i] = (short) ((src[i] + src[i + 1]) >> 1);
			}
		}
	}

	public static String balanceFormatWithDecimal(String s) {
		String tag = "balanceFormatWithDecimal(String)";

		if (s == null || s.length() < 3) {
			Log.e(tag, "Illegal arguments!");
			return null;
		}

		String tempStr = s;

		for (int i = 0; i < (s.length() - 3); i++) {
			if (tempStr.startsWith("0")) {
				tempStr = tempStr.substring(1);
			} else {
				break;
			}
		}

		tempStr = tempStr.substring(0, tempStr.length() - 2) + "."
				+ tempStr.substring(tempStr.length() - 2, tempStr.length());

		return tempStr;
	}

	public static String balanceFormatWithoutDecimal(String s) {
		String tag = "balanceFormatWithoutDecimal(String)";

		if (s == null || s.length() == 0) {
			Log.e(tag, "Illegal arguments!");
			return null;
		}

		String tempStr = s;

		for (int i = 0; i < (s.length() - 1); i++) {
			if (tempStr.startsWith("0")) {
				tempStr = tempStr.substring(1);
			} else {
				break;
			}
		}

		return tempStr;
	}
}
