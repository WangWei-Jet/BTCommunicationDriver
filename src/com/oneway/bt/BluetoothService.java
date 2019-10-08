package com.oneway.bt;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.oneway.bt.controller.LogInfoController;
import com.oneway.bt.controller.ServiceBinder;

public class BluetoothService extends Service {

	private ServiceBinder binder;

	private String tag = BluetoothService.class.getSimpleName();

	@Override
	public IBinder onBind(Intent intent) {
		LogInfoController.traceNormalInfo(tag, "service bind");
		binder = new ServiceBinder();
		return binder;
	}

	@Override
	public void onRebind(Intent intent) {
		LogInfoController.traceNormalInfo(tag, "service onrebind");
		super.onRebind(intent);
	}

	@Override
	public void onCreate() {
		LogInfoController.traceNormalInfo(tag, "service oncreate");
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LogInfoController.traceNormalInfo(tag, "service onstart");
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		LogInfoController.traceNormalInfo(tag, "service ondestroy");
		super.onDestroy();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		LogInfoController.traceNormalInfo(tag, "service onunbind");
		return super.onUnbind(intent);
	}

}
