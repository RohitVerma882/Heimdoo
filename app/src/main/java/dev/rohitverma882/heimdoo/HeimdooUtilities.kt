package dev.rohitverma882.heimdoo

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView

import java.io.File
import java.util.concurrent.Executor

fun Context.showToast(textRes: Int, duration: Int = Toast.LENGTH_SHORT) {
    if (Looper.myLooper() != Looper.getMainLooper()) {
        mainExecutorCompat.execute { showToast(textRes, duration) }
        return
    }
    Toast.makeText(this, textRes, duration).show()
}

fun Context.showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    if (Looper.myLooper() != Looper.getMainLooper()) {
        mainExecutorCompat.execute { showToast(text, duration) }
        return
    }
    Toast.makeText(this, text, duration).show()
}

fun Context.needsRestrictedReadStoragePermission(): Boolean {
    return (Build.VERSION.SDK_INT < 33) && (ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) != 0)
}

fun Context.needsCompleteReadStoragePermission(): Boolean {
    if (Build.VERSION.SDK_INT >= 30) {
        return !Environment.isExternalStorageManager()
    }
    return needsRestrictedReadStoragePermission()
}

fun AppCompatActivity.registerRequestPermissionLauncher(isGranted: (Boolean) -> Unit): ActivityResultLauncher<String> {
    return registerForActivityResult(ActivityResultContracts.RequestPermission(), isGranted)
}

fun AppCompatActivity.registerOpenDocumentLauncher(
    type: String,
    uri: (Uri?) -> Unit,
): ActivityResultLauncher<Unit?> {
    return registerForActivityResult(OpenDocumentFixed(type), uri)
}

fun AppCompatActivity.registerGetContentLauncher(
    type: String,
    uri: (Uri?) -> Unit,
): ActivityResultLauncher<Unit?> {
    return registerForActivityResult(GetContentFixed(type), uri)
}

fun Context.canReadUri(uri: Uri): Boolean {
    val path = Utilities.getPath(this, uri)
    return path != null && File(path).canRead()
}

fun View.showSnackbar(
    view: View,
    msg: String,
    length: Int,
    actionMsg: CharSequence?,
    action: (View) -> Unit,
) {
    val snackbar = Snackbar.make(view, msg, length)
    if (actionMsg != null) {
        snackbar.setAction(actionMsg) {
            action(this)
        }.show()
    } else {
        snackbar.show()
    }
}

fun MaterialTextView.appendColored(
    inputText: String,
    startIndex: Int,
    endIndex: Int,
    textColor: Int,
) {
    val coloredText = SpannableString(inputText)
    coloredText.setSpan(
        ForegroundColorSpan(textColor),
        startIndex,
        endIndex,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    append(coloredText)
}

@Suppress("DEPRECATION")
fun Context.isServiceRunning(clazz: Class<*>): Boolean {
    try {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        for (service in manager!!.getRunningServices(Int.MAX_VALUE)) {
            if (clazz.name == service.service.className) {
                return true
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}

fun Context.killServiceIfRunning(clazz: Class<*>) {
    val intent = Intent(this, clazz)
    if (isServiceRunning(clazz)) {
        stopService(intent)
    }
}

val Context.mainExecutorCompat: Executor get() = ContextCompat.getMainExecutor(this)

private class GetContentFixed(private var type: String) : ActivityResultContract<Unit?, Uri?>() {
    override fun createIntent(context: Context, input: Unit?): Intent {
        return Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(type)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        intent?.let { return it.data }
        return null
    }
}

private class OpenDocumentFixed(private var type: String) : ActivityResultContract<Unit?, Uri?>() {
    override fun createIntent(context: Context, input: Unit?): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(type)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        intent?.let { return it.data }
        return null
    }
}