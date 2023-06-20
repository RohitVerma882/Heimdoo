package dev.rohitverma882.heimdoo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log

import dev.rohitverma882.heimdoo.Constants.ACTION_USB_DEVICE_CONNECTED

private const val TAG = "UsbReceiver"

class UsbReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action
        if (action == null || action != UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            Log.d(TAG, "Unexpected action in UsbReceiver: $action")
        } else if (intent.hasExtra(UsbManager.EXTRA_DEVICE)) {
            Log.d(TAG, "Broadcasting USB_CONNECTED")
            val usbDevice: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            Intent(ACTION_USB_DEVICE_CONNECTED).apply {
                putExtra(UsbManager.EXTRA_DEVICE, usbDevice)
                setPackage(context.packageName)
            }.also {
                context.sendBroadcast(it)
            }
        } else {
            Log.e(TAG, "ACTION_USB_DEVICE_ATTACHED contains no UsbDevice!")
        }
    }
}