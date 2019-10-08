package com.oneway.bt.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

import android.bluetooth.BluetoothSocket;
import android.os.Binder;
import android.os.SystemClock;

import com.oneway.bt.protocal.CommuProtocal;

public class ServiceBinder extends Binder {

	private CommuProtocal commuProtocal;

	private int singlePackLength = 106;
	private int checkResGapTime = 10;
	private long dataSendTime = 0;
	private long communicationTimeout = 0;

	private boolean receiverAllowed = true;

	// ������Ӧ����
	private LinkedList<byte[]> responsedataList = new LinkedList<byte[]>();

	private String tag = ServiceBinder.class.getSimpleName();

	public ServiceBinder() {
		if (commuProtocal == null) {
			commuProtocal = new CommuProtocal(this);
		}
	}

	public CommuProtocal getCommuProtocal() {
		return commuProtocal;
	}

	public void setCommuProtocal(CommuProtocal commuProtocal) {
		this.commuProtocal = commuProtocal;
	}

	public int getSinglePackLength() {
		return singlePackLength;
	}

	public void setSinglePackLength(int singlePackLength) {
		this.singlePackLength = singlePackLength;
	}

	public boolean isReceiverAllowed() {
		return receiverAllowed;
	}

	public void setReceiverAllowed(boolean receiverAllowed) {
		this.receiverAllowed = receiverAllowed;
	}

	public int getCheckResGapTime() {
		return checkResGapTime;
	}

	public void setCheckResGapTime(int checkResGapTime) {
		this.checkResGapTime = checkResGapTime;
	}

	private Thread[] findAllThreads() {
		ThreadGroup group = Thread.currentThread().getThreadGroup();
		ThreadGroup topGroup = group;

		/* �����߳���������ȡ���߳��� */
		while (group != null) {
			topGroup = group;
			group = group.getParent();
		}
		/* ������߳����ӱ� */
		int estimatedSize = topGroup.activeCount() * 2;
		Thread[] slackList = new Thread[estimatedSize];

		/* ��ȡ���߳���������߳� */
		int actualSize = topGroup.enumerate(slackList);
		/* copy into a list that is the exact size */
		Thread[] list = new Thread[actualSize];
		System.arraycopy(slackList, 0, list, 0, actualSize);
		return (list);
	}

	/**
	 * ͨѶ
	 * 
	 * @param socket
	 *            ͨ��socket
	 * @param srcCmd
	 *            ����ָ��
	 * @param cmdLen
	 *            ����ָ���
	 * @param response
	 *            ��Ӧָ��
	 * @param timeout
	 *            ͨѶ��ʱ��ms��
	 * @return
	 */
	public synchronized int transmit(BluetoothSocket socket, byte[] srcCmd,
			int cmdLen, byte[] response, long timeout) {
		OutputStream outputStream = null;
		try {
			responsedataList.clear();
			commuProtocal.resetStatus();
			if (srcCmd == null || srcCmd.length == 0 || cmdLen <= 0) {
				LogInfoController
						.traceNormalError(tag,
								"params error while trying to do communication operation");
				return -4;
			}
			communicationTimeout = timeout;
			boolean interactionTimeout = true;
			// ��������
			int index = 0;
			outputStream = socket.getOutputStream();
			dataSendTime = SystemClock.elapsedRealtime();
			byte[] realSrcCmd = commuProtocal.handleSendMsg(srcCmd, cmdLen);
			LogInfoController.traceCommuDebug(
					tag,
					"send:"
							+ Utils.bytesToHexString(realSrcCmd,
									realSrcCmd.length));
			cmdLen = realSrcCmd.length;
			while (cmdLen >= singlePackLength) {
				outputStream.write(realSrcCmd, index, singlePackLength);
				outputStream.flush();
				index += singlePackLength;
				cmdLen -= singlePackLength;
			}
			if (cmdLen > 0) {
				outputStream.write(realSrcCmd, index, cmdLen);
				outputStream.flush();
			}

			LogInfoController.traceNormalDebug(tag,
					"request data send successfully");

			long currentTime = SystemClock.elapsedRealtime();
			while (currentTime - dataSendTime <= communicationTimeout) {
				if (responsedataList.isEmpty()) {
					try {
						Thread.sleep(checkResGapTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					currentTime = SystemClock.elapsedRealtime();
				} else {
					// ��ȷ���أ�δ��ʱ
					interactionTimeout = false;
					// ��ȡ��Ӧ
					try {
						int resIndex = 0;
						while (!responsedataList.isEmpty()) {
							byte[] res = responsedataList.poll();
							System.arraycopy(res, 0, response, resIndex,
									res.length);
							resIndex += res.length;
						}
						return resIndex;
					} catch (IndexOutOfBoundsException e) {
						LogInfoController.traceNormalError(tag, e.getMessage());
						LogInfoController
								.traceNormalError(tag,
										"receieve buffer not enough, response data too large");
						return -2;
					}
				}
			}
			if (interactionTimeout) {
				commuProtocal.notifyTimeout();
				return -3;
			}
		} catch (IOException e) {
			// e.printStackTrace();
			LogInfoController.traceNormalError(tag, e.getMessage());
			LogInfoController.traceNormalError(tag, "data send fail");
		} catch (IndexOutOfBoundsException e) {
			// e.printStackTrace();
			LogInfoController.traceNormalError(tag, e.getMessage());
			LogInfoController.traceNormalError(tag,
					"data send fail:command length too large");
		}
		return -1;
	}

	public void resetTimeout() {
		dataSendTime = SystemClock.elapsedRealtime();
	}

	public void onDeviceConnected(BluetoothSocket socket) {
		boolean threadExists = false;
		Thread[] allThreads = findAllThreads();
		for (Thread tempThread : allThreads) {
			if (tempThread.getName().equals(
					socket.getRemoteDevice().getAddress())) {
				threadExists = true;
				break;
			}
		}
		if (!threadExists) {
			DataMonitorThread monitorThread = new DataMonitorThread(socket);
			monitorThread.start();
		}
	}

	/**
	 * ���ݽ�����ϻص�
	 */
	public void onDataReceiveOver(byte[] resData) {
		if (resData == null || resData.length == 0) {
			return;
		}
		responsedataList.add(resData);
	}

	private class DataMonitorThread extends Thread {

		private BluetoothSocket socket;

		public DataMonitorThread(BluetoothSocket socket) {
			super();
			this.socket = socket;
			setName(socket.getRemoteDevice().getAddress());
		}

		@Override
		public void run() {
			super.run();
			InputStream inputStream = null;
			try {
				inputStream = socket.getInputStream();
				while (receiverAllowed) {
					byte[] tempCache = new byte[1024];
					// blocked
					int cacheLen = inputStream.read(tempCache);
					if (cacheLen > 0) {
						LogInfoController.traceCommuDebug(tag, "data detected");
						// data come
						byte[] realCache = new byte[cacheLen];
						System.arraycopy(tempCache, 0, realCache, 0, cacheLen);
						commuProtocal.handleReceiveMsg(realCache, cacheLen);
					}
				}
			} catch (IOException e) {
				LogInfoController.traceNormalDebug(tag, e.getMessage());
				LogInfoController.traceNormalDebug(tag, socket
						.getRemoteDevice().getName() + " disconnected");
			}
		}
	}
}
