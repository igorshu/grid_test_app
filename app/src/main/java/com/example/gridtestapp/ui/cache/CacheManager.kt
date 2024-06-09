package com.example.gridtestapp.ui.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.gridtestapp.MainActivity
import com.example.gridtestapp.ui.CELL_COUNT_PORTRAIT
import com.example.gridtestapp.ui.exceptions.ImageLoadException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
*
*   Наш Кеш-менеджер
*
*   Умеет загружать картинки из интернета и сохранять в двух вариантах,
*   для превью и оригинал.
*
 */

object CacheManager {

    private const val originalDirectory: String = "original_image_cache"
    private const val previewDirectory: String = "preview_image_cache"

    lateinit var cacheDir: String

    fun init(context: Context) {
        cacheDir = context.cacheDir.path

        File(originalDir()).mkdir()
        File(previewDir()).mkdir()
    }

    // Загружаем картинку из интернета и сохраняем в двух экземлярах

    suspend fun loadImage(url: String): Boolean {
        return suspendCancellableCoroutine { cont ->
            if (!URLUtil.isValidUrl(url)) {
                cont.resumeWithException(ImageLoadException(url))
                return@suspendCancellableCoroutine
            }

            val request: Request = Request.Builder()
                .url(url)
                .build()

            val response: Response = OkHttpClient().newCall(request).execute()
            if (!response.isSuccessful) {
                cont.resumeWithException(ImageLoadException(url))
                return@suspendCancellableCoroutine
            }

            val inputStream = response.body!!.byteStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)

            try {
                saveOriginalImage(url, bitmap)
                savePreviewImage(url, bitmap)
            } catch (throwable: Throwable) {
                cont.resumeWithException(throwable)
                return@suspendCancellableCoroutine
            }

            cont.resume(true)
        }
    }

    // Сохраняем оригинальную картинку

    private fun saveOriginalImage(url: String, bitmap: Bitmap) {
        val file = File(urlToOriginalPath(url))
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.close()
    }

    // Сохраняем и сжимаем превью

    private fun savePreviewImage(url: String, bitmap: Bitmap) {
        val file = File(urlToPreviewPath(url))
        val out = FileOutputStream(file)
        val previewBitmap = Bitmap.createScaledBitmap(
            bitmap,
            MainActivity.minSide / CELL_COUNT_PORTRAIT,
            MainActivity.minSide / CELL_COUNT_PORTRAIT,
            false)
        previewBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.close()
    }

    // Большая картинка

    fun originalImageBitmap(url: String) : ImageBitmap {
        val filePath = urlToOriginalPath(url)
        return getImageBitmap(filePath)!!
    }

    // Картинка-превью

    fun previewImageBitmap(url: String) : ImageBitmap? {
        val filePath = urlToPreviewPath(url)
        return getImageBitmap(filePath)
    }

    // Загружаем в ImageBitmap с диска

    private fun getImageBitmap(filePath: String): ImageBitmap? {
        val file = BitmapFactory.decodeFile(filePath) ?: return null
        return file.asImageBitmap()
    }

    fun isCached( url: String): Boolean {
        return File(urlToOriginalPath(url)).exists() && File(urlToPreviewPath(url)).exists()
    }

    fun isNotCached(url: String): Boolean = !isCached(url)

    // Преобразуем урл в имя файла, учитываются хост, путь и параметры запроса

    private fun urlToFilename(url: String): String {
        val uri = Uri.parse(url)
        return "${uri.host}_${uri.pathSegments.joinToString("_")}" + if (uri.query != null) "_${uri.query!!.replace(":", "_")}" else ""
    }

    // Директория для хранения оригинальных картинок

    private fun originalDir(): String = "$cacheDir/$originalDirectory"

    // Директория для хранения превью

    private fun previewDir(): String = "$cacheDir/$previewDirectory"

    private fun urlToOriginalPath(url: String): String = originalDir() + "/" + urlToFilename(url)

    private fun urlToPreviewPath(url: String): String = previewDir() + "/" + urlToFilename(url)

    // Удаляем превью и оригинал с диска

    fun removeBothImages(url: String) {
        File(urlToOriginalPath(url)).delete()
        File(urlToPreviewPath(url)).delete()
    }
}

