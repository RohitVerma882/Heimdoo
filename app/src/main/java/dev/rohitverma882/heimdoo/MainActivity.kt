package dev.rohitverma882.heimdoo

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import dev.rohitverma882.heimdoo.Constants.ACTION_USB_DEVICE_CONNECTED
import dev.rohitverma882.heimdoo.databinding.ActivityMainBinding
import dev.rohitverma882.heimdoo.databinding.DialogAboutBinding

import rikka.html.text.toHtml

import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val openDevices: MutableMap<String, UsbDeviceConnection> = HashMap()
    private var cleanCachedImgs: Boolean = true;

    private val usbManager: UsbManager by lazy {
        (getSystemService(
            Context.USB_SERVICE
        ) as UsbManager)
    }

    private val resultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            hideLoading()
            if (intent == null) return
            val action = intent.action
            if (action.equals(HeimdooService.ACTION_COMMAND_RESULT)) {
                val result = intent.getStringExtra(HeimdooService.KEY_RESULT)
                Log.d(TAG, "received result from heimdoo service: $result")
                var fix = "\n"
                if (result != null && result.startsWith("\n")) {
                    fix = ""
                }
                binding.resultText.append(fix + result)
            }
        }
    }

    private val toCacheReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            hideLoading()
            if (intent == null) return
            val action = intent.action
            if (action.equals(FileCacheService.ACTION_FINISHED_COPY_TO_CACHE)) {
                val path = intent.getStringExtra(HeimdooService.KEY_RESULT)
                Log.d(TAG, "File stored in cache: $path")
                if (path == null || !File(path).canRead()) {
                    showToast(R.string.feiled_to_get_file_path, Toast.LENGTH_LONG)
                } else {
                    binding.commandEditText.append(path)
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerRequestPermissionLauncher { isGranted: Boolean ->
            if (isGranted) {
                tryLaunchSelectFile()
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                showPermissionRationaleDialog()
            } else {
                showToast(R.string.permission_rationale_msg, Toast.LENGTH_LONG)
                tryLaunchSelectFile()
            }
        }

    private val openDocumentLauncher = registerOpenDocumentLauncher("*/*") { uri: Uri? ->
        processFileSelected(uri)
        cleanCachedImgs = false
    }

    private val getContentLauncher = registerGetContentLauncher("*/*") { uri: Uri? ->
        processFileSelected(uri)
        cleanCachedImgs = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        initViews()

        initUsbReceiver()
        ContextCompat.registerReceiver(
            this,
            resultReceiver,
            IntentFilter(HeimdooService.ACTION_COMMAND_RESULT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            this,
            toCacheReceiver,
            IntentFilter(FileCacheService.ACTION_FINISHED_COPY_TO_CACHE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        tryStartHeimdooService()
    }

    private fun initViews() {
        binding.resultText.movementMethod = ScrollingMovementMethod.getInstance()
        binding.commandEditText.setText("")
        binding.commandEditText.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN) {
                onCommandEnter()
                return@setOnKeyListener true
            } else {
                return@setOnKeyListener false
            }
        }
        binding.commandEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                onCommandEnter()
                return@setOnEditorActionListener true
            } else {
                return@setOnEditorActionListener false
            }
        }
        try {
            binding.commandEditText.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onCommandEnter() {
        var command = binding.commandEditText.text.toString().trim()
        if (command.isNotBlank() && allServiceFinished) {
            val cmdArray = command.split(" ").toTypedArray()
            binding.commandEditText.setText("")
            val prefix = "~ \$ "
            command = "\n\n$prefix$command"
            binding.resultText.appendColored(
                command, 0, prefix.length, Color.GREEN
            )
            performCommand(cmdArray.copyOf())
        }
    }

    private fun performCommand(cmdArray: Array<String>) {
        try {
            Log.d(TAG, "heimdall command: ${cmdArray.joinToString(" ")}")
            Intent(this, HeimdooService::class.java).apply {
                action = HeimdooService.ACTION_PROCESS_COMMAND
                putExtra(HeimdooService.KEY_COMMAND, cmdArray)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    setPackage(packageName)
                }
            }.also {
                startService(it)
                showLoading()
                showToast(R.string.performing_command)
            }
        } catch (e: Exception) {
            Utilities.logException(e)
        }
    }

    private fun tryStartHeimdooService() {
        Intent(this, HeimdooService::class.java).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setPackage(packageName)
            }
        }.also {
            try {
                if (!isServiceRunning(HeimdooService::class.java)) {
                    startService(it)
                    sendBroadcast(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    Handler(Looper.getMainLooper()).postDelayed({
                        startService(it)
                        sendBroadcast(it)
                    }, 500)
                } catch (e2: Exception) {
                    Utilities.logException(e2)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_attach -> {
                if (needsRestrictedReadStoragePermission()) {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    tryLaunchSelectFile()
                }
                true
            }

            R.id.action_share -> {
                try {
                    shareResults()
                } catch (e: Exception) {
                    Utilities.logException(e)
                }
                true
            }

            R.id.action_clear -> {
                binding.resultText.text = ""
                true
            }

            R.id.action_about -> {
                showAboutDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun tryLaunchSelectFile() {
        try {
            openDocumentLauncher.launch(null)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                getContentLauncher.launch(null)
            } catch (e2: Exception) {
                Utilities.logException(e2)
                showToast(R.string.feiled_to_access_files)
            }
        }
    }

    private fun processFileSelected(uri: Uri?) {
        uri?.let { any ->
            val path = Utilities.getPath(this, any)
            if (path == null || !File(path).canRead()) {
                Intent(this, FileCacheService::class.java).apply {
                    action = FileCacheService.ACTION_COPY_TO_CACHE
                    putExtra(FileCacheService.KEY_DEST_DIR, imgCacheDir)
                    putExtra(FileCacheService.KEY_DELETE_DEST_DIR_CONTENT, cleanCachedImgs)
                    putExtra(FileCacheService.KEY_SRC_FILE_URI, any)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        setPackage(packageName)
                    }
                }.also {
                    startService(it)
                    showLoading()
                    showToast(R.string.preparing_file, Toast.LENGTH_LONG)
                }
            } else {
                Log.d(TAG, "File is readable directly")
                val finedPath = path.removePrefix("file:")
                binding.commandEditText.append(finedPath)
            }
        }
        if (uri == null) showToast(R.string.feiled_to_get_file_path, Toast.LENGTH_LONG)
    }

    private fun showPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_rationale_title)
            .setMessage(R.string.permission_rationale_msg)
            .setPositiveButton(
                R.string.ok
            ) { _: DialogInterface, _: Int ->
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }.show()
    }

    private val imgCacheDir: String
        get() {
            val imgDir: File = try {
                File(externalCacheDir, "cached_imgs")
            } catch (e: Exception) {
                e.printStackTrace()
                File(cacheDir, "cached_imgs")
            }
            return imgDir.absolutePath
        }

    private fun showLoading() {
        allServiceFinished = false
        binding.progress.visibility = View.VISIBLE
        // binding.commandEditText.isEnabled = false
    }

    private fun hideLoading() {
        allServiceFinished = true
        binding.progress.visibility = View.GONE
        // binding.commandEditText.isEnabled = true
    }

    override fun onDestroy() {
        unregisterReceiver(usbReceiver)
        unregisterReceiver(resultReceiver)
        unregisterReceiver(toCacheReceiver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        tryStartHeimdooService()
        if (allServiceFinished) {
            hideLoading()
        }
    }

    private fun initUsbReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        intentFilter.addAction(ACTION_USB_DEVICE_CONNECTED)
        intentFilter.addAction(ACTION_USB_PERMISSION)
        ContextCompat.registerReceiver(
            this,
            usbReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                ACTION_USB_DEVICE_CONNECTED -> {
                    Log.d(TAG, "Usb device connected")
                    val usbDevice: UsbDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                UsbManager.EXTRA_DEVICE,
                                UsbDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }
                    usbDevice?.let {
                        if (usbManager.hasPermission(it)) {
                            val usbDeviceConnection = usbManager.openDevice(it)
                            if (usbDeviceConnection == null) {
                                Log.d(TAG, "Device opened returned null connection!")
                            } else if (Heimdoo.isHeimdallDevice(
                                    usbDeviceConnection, it
                                )
                            ) {
                                Log.d(TAG, "isHeimdallDevice=true")
                                try {
                                    openDevices[it.deviceName]?.close()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                openDevices[it.deviceName] = usbDeviceConnection
                            } else {
                                Log.e(TAG, "not recognized as heimdall usb device")
                            }
                        } else {
                            Log.d(TAG, "No permission to use USB device. Requesting permission...")
                            requestUsbPermission(it)
                        }
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.d(TAG, "Usb device detached")
                    val usbDevice: UsbDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                UsbManager.EXTRA_DEVICE,
                                UsbDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }
                    usbDevice?.let {
                        try {
                            openDevices[it.deviceName]?.close()
                            openDevices.remove(it.deviceName)
                        } catch (e: Exception) {
                            Utilities.logException(e)
                        }
                    }
                }

                ACTION_USB_PERMISSION -> synchronized(this) {
                    val usbDevice: UsbDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                UsbManager.EXTRA_DEVICE,
                                UsbDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(TAG, "Usb permission granted")
                        usbDevice?.let {
                            if (Heimdoo.isHeimdallDevice(usbManager, it)) {
                                Log.d(TAG, "devname=" + it.deviceName)
                                try {
                                    val result = Heimdoo.execHeimdall(
                                        this@MainActivity,
                                        usbManager,
                                        it,
                                        arrayOf("heimdall", "detect")
                                    )
                                    Log.d(TAG, "result=$result")
                                } catch (_: Exception) {

                                }
                            } else {
                                Log.e(TAG, "not recognized as heimdall usb device")
                            }
                        }
                    } else {
                        Log.d(TAG, "permission denied for device $usbDevice")
                    }
                }
            }
        }
    }

    private fun requestUsbPermission(usbDevice: UsbDevice?) {
        try {
            val pendingIntent = PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_USB_PERMISSION).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        setPackage(packageName)
                    }
                }, PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(usbDevice, pendingIntent)
        } catch (e: Exception) {
            Utilities.logException(e)
            try {
                val pendingIntent = PendingIntent.getBroadcast(
                    this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
                )
                usbManager.requestPermission(usbDevice, pendingIntent)
            } catch (e2: Exception) {
                Utilities.logException(e2)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: $intent")
        val action = intent.action
        if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            Log.d(TAG, "Received intent usb device attached")
            val usbDevice: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            Intent(ACTION_USB_DEVICE_CONNECTED).apply {
                putExtra(UsbManager.EXTRA_DEVICE, usbDevice)
                setPackage(packageName)
            }.also {
                sendBroadcast(it)
            }
        }
    }

    private fun shareResults() {
        var result = binding.resultText.text.toString().trim()
        if (result.isBlank()) result = "null"
        Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, result)
            type = "text/plain"
        }.also {
            startActivity(
                Intent.createChooser(
                    it, getString(R.string.title_share_transcript_with)
                )
            )
        }
    }

    private fun showAboutDialog() {
        val binding = DialogAboutBinding.inflate(layoutInflater, null, false)
        binding.sourceCode.movementMethod = LinkMovementMethod.getInstance()
        binding.sourceCode.text = getString(
            R.string.about_view_source_code,
            "<b><a href=\"https://github.com/RohitVerma882\">@RohitVerma882</a></b>"
        ).toHtml()
        binding.icon.setImageBitmap(
            AppIconCache.getOrLoadBitmap(
                this,
                applicationInfo,
                android.os.Process.myUid() / 100000,
                resources.getDimensionPixelOffset(R.dimen.default_app_icon_size)
            )
        )
        binding.versionName.text = getString(R.string.app_version)
        MaterialAlertDialogBuilder(this)
            .setView(binding.root)
            .show()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val ACTION_USB_PERMISSION = "dev.rohitverma882.heimdoo.USB_PERMISSION"
        private var allServiceFinished: Boolean = true
    }
}