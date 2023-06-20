package dev.rohitverma882.heimdoo

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.IBinder
import android.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

import java.io.File

import kotlin.system.exitProcess

class HeimdooService : Service() {
    private external fun init()

    private val nativeInitialized by lazy {
        try {
            init()
            true
        } catch (e: Exception) {
            Utilities.logException(e)
            Log.d(TAG, "Could not initialize native part: $e")
            false
        }
    }
    private val scope = MainScope()

    private val heimdooListener = object : HeimdooResultListener {
        override fun onHeimdooResult(line: String?) {
            Log.d(TAG, "heimdall result received: $line")
            scope.launch(Dispatchers.IO) {
                Intent(ACTION_COMMAND_RESULT).apply {
                    putExtra(KEY_RESULT, line)
                    setPackage(packageName)
                }.also {
                    sendBroadcast(it)
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action.equals(ACTION_PROCESS_COMMAND)) {
            scope.launch(Dispatchers.IO) {
                try {
                    Log.d(TAG, "native part initialized: $nativeInitialized")
                    processCommandIntent(intent!!)
                } catch (e: Exception) {
                    Utilities.logException(e)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
        exitProcess(0)
        @Suppress("UNREACHABLE_CODE")
        throw RuntimeException("System.exit returned normally, while it was supposed to halt JVM.")
    }

    private fun processCommandIntent(intent: Intent) {
        Log.d(TAG, "processing heimdall intent: $intent")
        val cmdArray = intent.getStringArrayExtra(KEY_COMMAND)!!
        Log.d(TAG, "heimdall command received: ${cmdArray.joinToString(" ")}")
        val usbManager = (getSystemService(Context.USB_SERVICE) as UsbManager)
        val deviceList = (usbManager.deviceList.values as Collection<UsbDevice>)
        if (deviceList.size == 1) {
            val usbDevice = deviceList.first()
            Log.d(TAG, "processing heimdall only one device: ${usbDevice.deviceName}")
            if (isHeimdallDevice(usbManager, usbDevice)) {
                processCommand(usbDevice, cmdArray.copyOf())
            } else {
                processCommand(null, cmdArray.copyOf())
            }
        } else if (deviceList.size > 1) {
            Log.d(TAG, "processing heimdall more devices: ${deviceList.size}")
            deviceList.forEach { usbDevice ->
                if (isHeimdallDevice(usbManager, usbDevice)) {
                    processCommand(usbDevice, cmdArray.copyOf())
                    return
                }
            }
            processCommand(null, cmdArray.copyOf())
        } else {
            processCommand(null, cmdArray.copyOf())
        }
    }

    private fun processCommand(usbDevice: UsbDevice?, args: Array<String>) {
        scope.launch(Dispatchers.IO) {
            if (usbDevice != null) {
                getSystemService(Context.USB_SERVICE).also { any ->
                    execHeimdall(
                        this@HeimdooService, (any as UsbManager), usbDevice, args.copyOf()
                    ).also {
                        heimdooListener.onHeimdooResult(it)
                    }
                }
            } else {
                execHeimdall(this@HeimdooService, args.copyOf()).also {
                    heimdooListener.onHeimdooResult(it)
                }
            }
        }
    }

    companion object {
        init {
            System.loadLibrary("heimdoo")
        }

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

        private const val TAG = "HeimdooService"
        private const val HEIMDALL_OUT_FILE = "heimdall.out"
        private const val HEIMDALL_ERR_FILE = "heimdall.err"
        const val ACTION_PROCESS_COMMAND = "dev.rohitverma882.heimdoo.action.PROCESS_COMMAND"
        const val ACTION_COMMAND_RESULT = "dev.rohitverma882.heimdoo.action.COMMAND_RESULT"
        const val KEY_COMMAND = "key.command"
        const val KEY_RESULT = "key.result"
    }
}