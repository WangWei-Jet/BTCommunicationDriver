package com.oneway.bt.controller;

import android.util.Log;

public class LogInfoController {

	private static boolean allowNormalLogPrint = false;
	private static boolean allowCommunicationLogPrint = false;

	public static boolean isAllowNormalLogPrint() {
		return allowNormalLogPrint;
	}

	public static void setAllowNormalLogPrint(boolean allowNormalLogPrint) {
		LogInfoController.allowNormalLogPrint = allowNormalLogPrint;
	}

	public static boolean isAllowCommunicationLogPrint() {
		return allowCommunicationLogPrint;
	}

	public static void setAllowCommunicationLogPrint(
			boolean allowCommunicationLogPrint) {
		LogInfoController.allowCommunicationLogPrint = allowCommunicationLogPrint;
	}

	public static void traceNormalInfo(String tag, String msg) {
		if (isAllowNormalLogPrint()) {
			Log.i(tag, msg);
		}
	}

	public static void traceNormalDebug(String tag, String msg) {
		if (isAllowNormalLogPrint()) {
			Log.d(tag, msg);
		}
	}

	public static void traceNormalWarn(String tag, String msg) {
		if (isAllowNormalLogPrint()) {
			Log.w(tag, msg);
		}
	}

	public static void traceNormalError(String tag, String msg) {
		if (isAllowNormalLogPrint()) {
			Log.e(tag, msg);
		}
	}

	public static void traceCommuInfo(String tag, String msg) {
		if (isAllowCommunicationLogPrint()) {
			Log.i(tag, msg);
		}
	}

	public static void traceCommuDebug(String tag, String msg) {
		if (isAllowCommunicationLogPrint()) {
			Log.d(tag, msg);
		}
	}

	public static void traceCommuWarn(String tag, String msg) {
		if (isAllowCommunicationLogPrint()) {
			Log.w(tag, msg);
		}
	}

	public static void traceCommuError(String tag, String msg) {
		if (isAllowCommunicationLogPrint()) {
			Log.e(tag, msg);
		}
	}

}
