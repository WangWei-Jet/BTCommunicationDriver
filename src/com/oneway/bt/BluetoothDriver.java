package com.oneway.bt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;

import com.oneway.bt.controller.LogInfoController;
import com.oneway.bt.controller.ServiceBinder;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class BluetoothDriver {

	private Context context;
	private List<BluetoothSocket> socketList;
	private BluetoothOperator btOper;

	private ServiceBinder serviceBinder;
	private String tag = BluetoothDriver.class.getSimpleName();

	private BroadcastReceiver btListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equalsIgnoreCase(
					BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				LogInfoController.traceNormalDebug(
						tag,
						device.getName() == null ? device.getAddress() : device
								.getName() + " disconnected");
				// 连接断开，从socket list里面剔除
				if (socketList != null && socketList.size() > 0) {
					for (BluetoothSocket socket : socketList) {
						if (socket.getRemoteDevice().getAddress()
								.equalsIgnoreCase(device.getAddress())) {
							try {
								socket.close();
							} catch (IOException e) {
								e.printStackTrace();
								LogInfoController.traceNormalDebug(tag,
										"socket resource realse fail");
							}
							removeUnconnectedSocket(socketList);
							LogInfoController.traceNormalDebug(tag,
									"socket resource realse success");
							break;
						}
					}
				}
			} else if (intent.getAction().equalsIgnoreCase(
					BluetoothAdapter.ACTION_STATE_CHANGED)) {
				int previousState = intent.getIntExtra(
						BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
				if (previousState == BluetoothAdapter.STATE_ON) {
					LogInfoController.traceNormalDebug(tag,
							"local bt adapter state on status change");
					if (socketList != null && socketList.size() > 0) {
						for (BluetoothSocket socket : socketList) {
							try {
								socket.close();
							} catch (IOException e) {
								e.printStackTrace();
								LogInfoController
										.traceNormalDebug(
												tag,
												socket.getRemoteDevice()
														+ " socket resource realse fail");
							}
						}
						removeUnconnectedSocket(socketList);
						LogInfoController.traceNormalDebug(tag,
								"socket resource realse success");
					}
				}
			}
		}
	};

	private ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			LogInfoController.traceNormalDebug(tag, "service disconnected");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (serviceBinder == null) {
				serviceBinder = (ServiceBinder) service;
			}
			LogInfoController.traceNormalDebug(tag, "service connected");
			if (serviceBinder == null) {
				LogInfoController.traceNormalError(tag,
						"can not interact with service");
			}
		}
	};

	public List<BluetoothSocket> getSocketList() {
		return socketList;
	}

	public boolean init(Context context) {
		this.context = context;
		LogInfoController.traceNormalDebug(tag,
				"package Info:" + this.context.getPackageName());
		if (socketList == null) {
			socketList = new ArrayList<BluetoothSocket>();
		}
		if (serviceBinder == null) {
			Intent intent = new Intent(context, BluetoothService.class);
			context.getApplicationContext().bindService(intent,
					serviceConnection, Context.BIND_AUTO_CREATE);
		}
		// 注册断开连接广播
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.setPriority(1000);
		context.getApplicationContext().registerReceiver(btListener, filter);
		return true;
	}

	public boolean connectDevice(BluetoothDevice device, Object... params) {
		if (btOper == null) {
			btOper = new BluetoothOperator();
		}
		if (getDeviceSocket(device) != null) {
			// socket already exits
			return true;
		}
		BluetoothSocket tempSocket = null;
		if (params == null || params.length == 0) {
			// 未指定连接方式,使用最常用的连接方式
			tempSocket = btOper.buildInsecureConnectUUID(device);
		} else {
			// params exist,connection mode specified
			tempSocket = btOper.buildConnection(device, params);
		}
		if (tempSocket == null) {
			// connect fail
			return false;
		}
		// connect success
		socketList.add(tempSocket);
		if (serviceBinder == null) {
			LogInfoController
					.traceNormalError(tag,
							"no service found,may not be able to communicate with device though connected");
		} else {
			serviceBinder.onDeviceConnected(tempSocket);
		}
		return true;
	}

	public synchronized int transmit(byte[] srcCmd, int cmdLen,
			byte[] response, long timeout, Object... params) {
		if (serviceBinder == null) {
			LogInfoController.traceNormalError(tag, "no service found");
			return -99;
		}
		BluetoothSocket socket = null;
		if (params != null && params.length > 0
				&& params[0] instanceof BluetoothDevice) {
			for (BluetoothSocket temp : socketList) {
				if (temp.getRemoteDevice()
						.getAddress()
						.equalsIgnoreCase(
								((BluetoothDevice) params[0]).getAddress())) {
					socket = temp;
					break;
				}
			}
			if (socket == null) {
				LogInfoController
						.traceNormalError(tag,
								"can not find socket,check whether device is connected.");
				return -98;
			}
		}
		if (socket == null) {
			socket = socketList.get(0);
		}
		return serviceBinder
				.transmit(socket, srcCmd, cmdLen, response, timeout);
	}

	public boolean disconnect(Object... params) {
		boolean flag = disconnectDevice(params);
		// if (socketList == null || socketList.size() == 0) {
		// if (serviceBinder != null) {
		// context.getApplicationContext()
		// .unbindService(serviceConnection);
		// serviceBinder = null;
		// }
		// }
		return flag;
	}

	private boolean disconnectDevice(Object... params) {
		if (btOper == null) {
			btOper = new BluetoothOperator();
		}
		if (socketList == null || socketList.size() == 0) {
			LogInfoController.traceNormalDebug(tag,
					"no connection socket while trying to realse connection");
			return false;
		}
		if (params != null && params.length > 0
				&& params[0] instanceof BluetoothDevice) {
			// disconnect specified device connection
			BluetoothDevice device = (BluetoothDevice) params[0];
			for (BluetoothSocket socket : socketList) {
				if (socket.getRemoteDevice().getAddress()
						.equalsIgnoreCase(device.getAddress())) {
					boolean flag = btOper.realseConnection(socket);
					if (flag) {
						socketList.remove(socket);
					}
					return flag;
				}
			}
			LogInfoController.traceNormalDebug(tag,
					"can not disconnect device that is not connected");
			return false;
		}
		while (socketList.size() > 0) {
			BluetoothSocket socket = socketList.get(0);
			boolean flag = btOper.realseConnection(socket);
			if (flag) {
				socketList.remove(socket);
			} else {
				return false;
			}
		}
		return true;
	}

	public boolean isDeviceConnected(Object... params) {
		if (socketList == null || socketList.size() == 0) {
			LogInfoController
					.traceNormalDebug(tag,
							"no connection socket found while trying to query connection state");
			return false;
		}
		if (params != null && params.length > 0
				&& params[0] instanceof BluetoothDevice) {
			BluetoothDevice device = (BluetoothDevice) params[0];
			for (BluetoothSocket socket : socketList) {
				if (socket.getRemoteDevice().getAddress()
						.equalsIgnoreCase(device.getAddress())) {
					return socket.isConnected();
				}
			}
			return false;
		}
		for (BluetoothSocket socket : socketList) {
			if (!socket.isConnected()) {
				return false;
			}
		}
		return true;
	}

	public Map<BluetoothDevice, BluetoothSocket> getConnectedInfo() {
		if (socketList == null || socketList.size() == 0) {
			LogInfoController
					.traceNormalDebug(tag,
							"no connection socket found while trying to query connected devices");
			return null;
		}
		Map<BluetoothDevice, BluetoothSocket> connectedDevicesInfo = new HashMap<BluetoothDevice, BluetoothSocket>();
		removeUnconnectedSocket(socketList);
		if (socketList.size() > 0) {
			for (BluetoothSocket socket : socketList) {
				if (!socket.isConnected()) {
					continue;
				}
				connectedDevicesInfo.put(socket.getRemoteDevice(), socket);
			}
		}
		if (connectedDevicesInfo.size() > 0) {
			return connectedDevicesInfo;
		}
		return null;
	}

	private BluetoothSocket getDeviceSocket(BluetoothDevice device) {
		if (socketList == null || socketList.size() == 0) {
			return null;
		}
		for (BluetoothSocket tempSocket : socketList) {
			if (tempSocket.getRemoteDevice().getAddress()
					.equalsIgnoreCase(device.getAddress())) {
				return tempSocket;
			}
		}
		return null;
	}

	private synchronized List<BluetoothSocket> removeUnconnectedSocket(
			List<BluetoothSocket> sockets) {
		if (sockets == null || sockets.size() == 0) {
			return sockets;
		}
		Iterator<BluetoothSocket> it = sockets.iterator();
		while (it.hasNext()) {
			BluetoothSocket currentSocket = it.next();
			if (!currentSocket.isConnected()) {
				it.remove();
			}
		}
		return sockets;
	}
}
