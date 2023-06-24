package dev.rohitverma882.heimdoo

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log

import java.io.File

class Heimdoo {
    companion object {
        private const val TAG = "Heimdoo"
        private const val HEIMDALL_OUT_FILE = "heimdall.out"
        private const val HEIMDALL_ERR_FILE = "heimdall.err"

        init {
            System.loadLibrary("heimdoo")
        }

        @JvmStatic
        external fun init()

        @JvmStatic
        private external fun isHeimdallDevice(fd: Int): Boolean

        @JvmStatic
        private external fun execHeimdall(
            stdout: String,
            stderr: String,
            fd: Int,
            args: Array<String>,
        )

        fun isHeimdallDevice(
            usbDeviceConnection: UsbDeviceConnection,
            usbDevice: UsbDevice?,
        ): Boolean {
            return isHeimdallDevice(
                usbDeviceConnection.fileDescriptor
            )
        }

        fun isHeimdallDevice(usbManager: UsbManager, usbDevice: UsbDevice?): Boolean {
            val usbDeviceConnection = usbManager.openDevice(usbDevice)
            val result = isHeimdallDevice(
                usbDeviceConnection.fileDescriptor
            )
            usbDeviceConnection.close()
            return result
        }

        fun execHeimdall(
            context: Context,
            usbDeviceConnection: UsbDeviceConnection,
            usbDevice: UsbDevice,
            args: Array<String>,
        ): String {
            val outFile = File(context.cacheDir, HEIMDALL_OUT_FILE)
            outFile.delete()
            outFile.parentFile!!.mkdirs()
            outFile.createNewFile()
            val errFile = File(context.cacheDir, HEIMDALL_ERR_FILE)
            errFile.delete()
            errFile.parentFile!!.mkdirs()
            errFile.createNewFile()
            execHeimdall(
                outFile.absolutePath,
                errFile.absolutePath,
                usbDeviceConnection.fileDescriptor,
                args.copyOf()
            )
            return outFile.readText().plus('\n').plus(errFile.readText()).trim()
        }

        fun execHeimdall(
            context: Context,
            usbManager: UsbManager,
            usbDevice: UsbDevice,
            args: Array<String>,
        ): String {
            Log.d(TAG, "execHeimdall() ${args.joinToString(" ")}")
            val usbDeviceConnection = usbManager.openDevice(usbDevice)
            val result = execHeimdall(
                context, usbDeviceConnection, usbDevice, args.copyOf()
            )
            usbDeviceConnection.close()
            return result
        }

        fun execHeimdall(
            context: Context,
            args: Array<String>,
        ): String {
            val outFile = File(context.cacheDir, HEIMDALL_OUT_FILE)
            outFile.delete()
            outFile.parentFile!!.mkdirs()
            outFile.createNewFile()
            val errFile = File(context.cacheDir, HEIMDALL_ERR_FILE)
            errFile.delete()
            errFile.parentFile!!.mkdirs()
            errFile.createNewFile()
            execHeimdall(
                outFile.absolutePath, errFile.absolutePath, -1, args.copyOf()
            )
            return outFile.readText().plus('\n').plus(errFile.readText()).trim()
        }
    }
}