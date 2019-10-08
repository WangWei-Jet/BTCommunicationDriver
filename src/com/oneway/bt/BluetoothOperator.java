package com.oneway.bt;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;

import com.oneway.bt.controller.LogInfoController;

public class BluetoothOperator {

	private final UUID uuid = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private final BluetoothAdapter btAdapter = BluetoothAdapter
			.getDefaultAdapter();
	private String tag = BluetoothOperator.class.getSimpleName();

	public BluetoothSocket buildConnection(BluetoothDevice device,
			Object... params) {
		if (params == null || params.length == 0) {
			LogInfoController
					.traceNormalError(tag,
							"buildConnection is not supposed to be invoked without params");
			return null;
		}
		int connectionMode = 0;
		int channelNo = 5;
		connectionMode = (Integer) params[0];
		if (params.length >= 2 && params[1] instanceof Integer) {
			channelNo = (Integer) params[1];
		}
		LogInfoController.traceNormalError(tag, "mode:" + connectionMode
				+ "  channelNo:" + channelNo);
		if (connectionMode == 1) {
			return buildInsecureConnectUUID(device);
		} else if (connectionMode == 2) {
			return buildSecureConnectUUID(device);
		} else if (connectionMode == 3) {
			return buildInsecureConnectChannelNo(device, channelNo);
		} else if (connectionMode == 4) {
			return buildSecureConnectChannelNo(device, channelNo);
		} else {
			return buildInsecureConnectUUID(device);
		}
	}

	public BluetoothSocket buildInsecureConnectUUID(BluetoothDevice device) {
		LogInfoController.traceNormalDebug(tag,
				"invoke buildInsecureConnectUUID");
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1) {
			LogInfoController.traceNormalError(tag,
					"api level not enough,try another connection mode");
			return buildSecureConnectUUID(device);
		}
		if (btAdapter == null) {
			LogInfoController.traceNormalError(tag,
					"no bluetooth adapter found");
			return null;
		}
		btAdapter.cancelDiscovery();

		BluetoothSocket socket;
		try {
			socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
		} catch (IOException e2) {
			LogInfoController.traceNormalWarn(tag, e2.getMessage());
			socket = null;
		}

		if (socket == null) {
			LogInfoController.traceNormalDebug(tag,
					"bluetooth socket launch fail");
			return null;
		}
		LogInfoController.traceNormalDebug(tag,
				"bluetooth socket launch success,next step to connect");

		try {
			btAdapter.cancelDiscovery();
			/**
			 * This method will block until a connection is made or the
			 * connection fails. If this method returns without an exception
			 * then this socket is now connected.
			 */
			LogInfoController
					.traceNormalInfo(tag,
							"try to launch insecure connection through the well-known UUID");
			Thread.sleep(200);
			socket.connect();
		} catch (Exception e) {
			LogInfoController.traceNormalDebug(tag, "connection build fail");
			LogInfoController.traceNormalWarn(tag, e.getMessage());
			try {
				socket.close();
				socket = null;
			} catch (IOException e1) {
				LogInfoController.traceNormalDebug(tag,
						"failed trying closing the unconnected socket");
				LogInfoController.traceNormalWarn(tag, e1.getMessage());
			}
		}

		return socket;

	}

	public BluetoothSocket buildSecureConnectUUID(BluetoothDevice device) {
		LogInfoController
				.traceNormalDebug(tag, "invoke buildSecureConnectUUID");
		if (btAdapter == null) {
			LogInfoController.traceNormalError(tag,
					"no bluetooth adapter found");
			return null;
		}
		btAdapter.cancelDiscovery();

		BluetoothSocket socket;
		try {
			socket = device.createRfcommSocketToServiceRecord(uuid);
		} catch (IOException e2) {
			LogInfoController.traceNormalWarn(tag, e2.getMessage());
			socket = null;
		}

		if (socket == null) {
			LogInfoController.traceNormalDebug(tag,
					"bluetooth socket launch fail");
			return null;
		}
		LogInfoController.traceNormalDebug(tag,
				"bluetooth socket launch success,next step to connect");

		try {
			btAdapter.cancelDiscovery();
			LogInfoController
					.traceNormalInfo(tag,
							"try to launch secure connection through the well-known UUID");
			/**
			 * This method will block until a connection is made or the
			 * connection fails. If this method returns without an exception
			 * then this socket is now connected.
			 */
			Thread.sleep(200);
			socket.connect();
		} catch (Exception e) {
			LogInfoController.traceNormalDebug(tag, "connection build fail");
			LogInfoController.traceNormalWarn(tag, e.getMessage());
			try {
				socket.close();
				socket = null;
			} catch (IOException e1) {
				LogInfoController.traceNormalDebug(tag,
						"failed trying closing the unconnected socket");
				LogInfoController.traceNormalWarn(tag, e1.getMessage());
			}
		}

		return socket;

	}

	public BluetoothSocket buildInsecureConnectChannelNo(
			BluetoothDevice device, int channelNo) {

		LogInfoController.traceNormalDebug(tag,
				"invoke buildInsecureConnectChannelNo");
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1) {
			LogInfoController.traceNormalError(tag, "api level not enough");
			return null;
		}
		if (btAdapter == null) {
			LogInfoController.traceNormalError(tag,
					"no bluetooth adapter found");
			return null;
		}
		btAdapter.cancelDiscovery();

		BluetoothSocket socket;
		try {
			Method m = device.getClass().getMethod(
					"createInsecureRfcommSocket", new Class[] { int.class });
			socket = (BluetoothSocket) m.invoke(device,
					Integer.valueOf(channelNo));
		} catch (Exception e2) {
			LogInfoController.traceNormalWarn(tag, e2.getMessage());
			socket = null;
		}

		if (socket == null) {
			LogInfoController.traceNormalDebug(tag,
					"bluetooth socket launch fail");
			return null;
		}
		LogInfoController.traceNormalDebug(tag,
				"bluetooth socket launch success,next step to connect");

		try {
			btAdapter.cancelDiscovery();
			/**
			 * This method will block until a connection is made or the
			 * connection fails. If this method returns without an exception
			 * then this socket is now connected.
			 */
			LogInfoController
					.traceNormalInfo(tag,
							"try to launch insecure connection through specified channel");
			Thread.sleep(200);
			socket.connect();
		} catch (Exception e) {
			LogInfoController.traceNormalDebug(tag, "connection build fail");
			LogInfoController.traceNormalWarn(tag, e.getMessage());
			try {
				socket.close();
				socket = null;
			} catch (IOException e1) {
				LogInfoController.traceNormalDebug(tag,
						"failed trying closing the unconnected socket");
				LogInfoController.traceNormalWarn(tag, e1.getMessage());
			}
		}

		return socket;

	}

	public BluetoothSocket buildSecureConnectChannelNo(BluetoothDevice device,
			int channelNo) {
		LogInfoController.traceNormalDebug(tag,
				"invoke buildSecureConnectChannelNo");
		if (btAdapter == null) {
			LogInfoController.traceNormalError(tag,
					"no bluetooth adapter found");
			return null;
		}
		btAdapter.cancelDiscovery();

		BluetoothSocket socket;
		try {

			Method m = device.getClass().getMethod("createRfcommSocket",
					new Class[] { int.class });
			socket = (BluetoothSocket) m.invoke(device,
					Integer.valueOf(channelNo));
		} catch (Exception e2) {
			LogInfoController.traceNormalWarn(tag, e2.getMessage());
			socket = null;
		}

		if (socket == null) {
			LogInfoController.traceNormalDebug(tag,
					"bluetooth socket launch fail");
			return null;
		}
		LogInfoController.traceNormalDebug(tag,
				"bluetooth socket launch success,next step to connect");

		try {
			btAdapter.cancelDiscovery();
			LogInfoController
					.traceNormalInfo(tag,
							"try to launch secure connection through specified channel");
			/**
			 * This method will block until a connection is made or the
			 * connection fails. If this method returns without an exception
			 * then this socket is now connected.
			 */
			Thread.sleep(200);
			socket.connect();
		} catch (Exception e) {
			LogInfoController.traceNormalDebug(tag, "connection build fail");
			LogInfoController.traceNormalWarn(tag, e.getMessage());
			try {
				socket.close();
				socket = null;
			} catch (IOException e1) {
				LogInfoController.traceNormalDebug(tag,
						"failed trying closing the unconnected socket");
				LogInfoController.traceNormalWarn(tag, e1.getMessage());
			}
		}

		return socket;

	}

	public boolean realseConnection(BluetoothSocket socket) {
		if (socket == null) {
			LogInfoController.traceNormalDebug(tag,
					"socket is null while trying to realse connection");
			return false;
		}
		if (!socket.isConnected()) {
			return true;
		}
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			LogInfoController.traceNormalError(tag, e.getMessage());
		}
		LogInfoController.traceNormalDebug(tag,
				"connection state:" + socket.isConnected());
		return !socket.isConnected();
	}
}
