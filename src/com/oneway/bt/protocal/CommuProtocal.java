package com.oneway.bt.protocal;

import java.util.LinkedList;

import android.util.Log;

import com.oneway.bt.controller.LogInfoController;
import com.oneway.bt.controller.ServiceBinder;
import com.oneway.bt.controller.Utils;

public class CommuProtocal {

	private ServiceBinder binder;

	private boolean waitForFirst = true;
	private boolean timeoutFlag = false;
	int resTotalLenFromProtocal = 0;
	int currentDataIndex = 0;

	private LinkedList<byte[]> dataCache = new LinkedList<byte[]>();

	public final int TRANS_CMD_CCID_HEAD_LEN = 10;
	private String tag = CommuProtocal.class.getSimpleName();

	public CommuProtocal(ServiceBinder binder) {
		super();
		this.binder = binder;
	}

	public byte[] handleSendMsg(byte[] srcCmd, int cmdLen) {
		byte[] targetCmd = null;
		targetCmd = CCIDProtocol(srcCmd, cmdLen);
		return targetCmd;
	}

	public synchronized boolean handleReceiveMsg(byte[] resData, int dataLen) {
		if (resData == null) {
			LogInfoController.traceNormalError(tag, "receive data null");
			return false;
		}
		if (dataLen > resData.length) {
			LogInfoController.traceNormalError(tag,
					"length larger than real size");
			return false;
		}
		if (timeoutFlag) {
			LogInfoController.traceCommuError(tag, "interaction timeout");
			dataReceiveOver(null);
			return false;
		}
		byte[] tempData = new byte[dataLen];
		System.arraycopy(resData, 0, tempData, 0, dataLen);
		// dataCache.add(tempData);
		// found wait command
		while (dataLen >= 10 && tempData[0] == (byte) 0x80
				&& tempData[7] == (byte) 0x80) {
			LogInfoController.traceCommuDebug(tag,
					"data:" + Utils.bytesToHexString(tempData, 10)
							+ "\nwait found! reset timeout");
			dataLen = dataLen - 10;
			// reset timeout
			binder.resetTimeout();
			if (dataLen > 0) {
				System.arraycopy(tempData, 10, tempData, 0, dataLen);
			} else {
				return true;
			}
		}
		// get real command
		byte[] realData = new byte[dataLen];
		System.arraycopy(tempData, 0, realData, 0, realData.length);
		LogInfoController.traceCommuDebug(tag,
				"res:" + Utils.bytesToHexString(realData, dataLen));
		// 此处增加停止帧判断处理
		if (dataLen == 10 && realData[0] == (byte) 0x81) {
			LogInfoController.traceCommuDebug(tag,
					"\nstop found! interaction over");
			dataCache.add(realData);
			dataReceiveOver(dataCache);
			return true;
		}
		currentDataIndex += dataLen;
		if (waitForFirst) {
			resTotalLenFromProtocal = calDataLen(realData);
			if (resTotalLenFromProtocal < 0) {
				dataReceiveOver(null);
				return false;
			}
		}
		waitForFirst = false;
		dataCache.add(realData);
		int dataRemain = resTotalLenFromProtocal - (currentDataIndex - 10);
		LogInfoController.traceCommuDebug(tag, "responseFinalLen:"
				+ resTotalLenFromProtocal + "   dataLeft:" + dataRemain
				+ "  currentIndex:" + dataLen);
		if (dataRemain < 0) {
			Log.e("CommThread", "Calculate dataRemain error!");
			LogInfoController.traceCommuError(tag, "Calculate dataLeft error!");
			binder.onDataReceiveOver(null);
			return false;
		} else if (dataRemain == 0) {
			LogInfoController.traceCommuDebug(tag, "data receive over");
			dataReceiveOver(dataCache);
			return true;
		}

		return true;
	}

	public boolean resetStatus() {
		dataCache.clear();
		waitForFirst = true;
		timeoutFlag = false;
		resTotalLenFromProtocal = 0;
		currentDataIndex = 0;
		return true;
	}

	public void notifyTimeout() {
		timeoutFlag = true;
	}

	private byte[] CCIDProtocol(byte[] cmd, int cmdLen) {
		byte[] transCmd = fillCmd(cmd, cmdLen);
		byte[] buffer = new byte[cmdLen + 10];
		System.arraycopy(transCmd, 0, buffer, 0, cmdLen + 10);
		return buffer;
	}

	private byte[] fillCmd(byte[] cmd, int cmdDataLenth) {
		// byte[] transCmd = new byte[1024];
		byte[] transCmd = new byte[2048];
		transCmd[0] = 0x6f;
		transCmd[1] = (byte) (cmdDataLenth & (byte) 0xff);
		transCmd[2] = (byte) ((cmdDataLenth >> 8) & (byte) 0xff);
		for (int i = 0; i < 7; i++) {
			if (i == 2) {
				// 增加版本号
				transCmd[3 + i] = 0x10;
			} else {
				transCmd[3 + i] = 0x00;
			}
		}
		System.arraycopy(cmd, 0, transCmd, TRANS_CMD_CCID_HEAD_LEN,
				cmdDataLenth);
		return transCmd;
	}

	private void dataReceiveOver(LinkedList<byte[]> cache) {
		if (cache == null || cache.isEmpty()) {
			binder.onDataReceiveOver(null);
			return;
		}
		byte[] buffer = new byte[2048];
		int index = 0;
		while (!cache.isEmpty()) {
			byte[] tempData = cache.poll();
			if (tempData != null && tempData.length > 0) {
				System.arraycopy(tempData, 0, buffer, index, tempData.length);
				index += tempData.length;
			}
		}
		byte[] res = new byte[index];
		System.arraycopy(buffer, 0, res, 0, index);
		binder.onDataReceiveOver(res);
	}

	private int calDataLen(byte[] response) {
		if (response[0] != (byte) 0x80) {
			Log.e(tag, "Response Data Head is not 0x80!");
			return -1;
		}

		int[] tmp = new int[] { 0x00, 0x00, 0x00, 0x00 };
		for (int i = 0; i < 4; i++) {
			tmp[i] = (response[i + 1] < 0) ? 256 + response[i + 1]
					: response[i + 1];
			tmp[i] = tmp[i] * (1 << 8 * i);
		}
		int ResponseDataLen = tmp[0] + tmp[1] + tmp[2] + tmp[3];
		return ResponseDataLen;

	}

}
