@file:Suppress("DEPRECATION")

package dev.rohitverma882.heimdoo

import android.app.IntentService
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log

import androidx.documentfile.provider.DocumentFile

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import kotlin.system.exitProcess

class FileCacheService : IntentService("FileCacheService") {
    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "onHandleIntent: $action")
        if (action.equals(ACTION_COPY_TO_CACHE)) {
            copyToCache(intent!!)
        }
    }

    private fun copyToCache(intent: Intent) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(KEY_SRC_FILE_URI, Uri::class.java)
        } else {
            intent.getParcelableExtra(KEY_SRC_FILE_URI)
        }
        val destDir = intent.getStringExtra(KEY_DEST_DIR)
        if (intent.getBooleanExtra(KEY_DELETE_DEST_DIR_CONTENT, false)) {
            try {
                val file = File(destDir!!)
                file.deleteRecursively()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        var outFile: String? = null
        try {
            outFile = copyUri(uri, destDir)
        } catch (e: Exception) {
            Utilities.logException(e)
        }
        Intent(ACTION_FINISHED_COPY_TO_CACHE).apply {
            putExtra(HeimdooService.KEY_RESULT, outFile)
            setPackage(packageName)
        }.also {
            sendBroadcast(it)
        }
    }

    private fun copyUri(uri: Uri?, dest: String?): String? {
        if (uri == null || dest.isNullOrEmpty()) return null
        val outFolder = File(dest)
        if (!outFolder.exists()) outFolder.mkdirs()
        var filename: String? = null
        try {
            filename = String.format(
                "%s.%s", Utilities.getFileBaseName(uri.path), Utilities.getFileExtension(uri.path)
            )
            try {
                val docFile = DocumentFile.fromSingleUri(this, uri)
                val name = docFile?.name
                if (!name.isNullOrBlank()) filename = name
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val outFile: File? = filename?.let { File(dest, it) }
        outFile?.let { file: File ->
            file.createNewFile()
            var bos: BufferedOutputStream? = null
            var bis: BufferedInputStream? = null
            try {
                bos = BufferedOutputStream(FileOutputStream(file))
                bis = BufferedInputStream(contentResolver.openInputStream(uri))
                val buffer = ByteArray(65536)
                var numBytes: Int
                while (bis.read(buffer).also { numBytes = it } != -1) {
                    bos.write(buffer, 0, numBytes)
                }
            } catch (e: IOException) {
                Utilities.logException(e)
                return null
            } finally {
                bos?.close()
                bis?.close()
            }
        }
        return outFile?.absolutePath
    }

    @Deprecated("Deprecated in Java")
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
        exitProcess(0)
        @Suppress("UNREACHABLE_CODE") throw RuntimeException("System.exit returned normally, while it was supposed to halt JVM.")
    }

    companion object {
        private const val TAG = "FileCacheService"
        const val ACTION_COPY_TO_CACHE = "dev.rohitverma882.heimdoo.action.COPY_TO_CACHE"
        const val ACTION_FINISHED_COPY_TO_CACHE = "dev.rohitverma882.heimdoo.action.FINISHED_COPY_TO_CACHE"
        const val KEY_DELETE_DEST_DIR_CONTENT = "key.delete.dest.dir.content"
        const val KEY_DEST_DIR = "key.dest.dir"
        const val KEY_SRC_FILE_URI = "key.src.file.uri"
    }
}