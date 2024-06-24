package com.example.gridtestapp.ui.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.gridtestapp.logic.viewmodels.ImageWidth
import com.example.gridtestapp.ui.exceptions.ImageLoadException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
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

object CacheManager: KoinComponent {

    private const val ORIGINAL_DIRECTORY: String = "original_image_cache"
    private const val PREVIEW_DIRECTORY: String = "preview_image_cache"

    private lateinit var originalDir: String // Директория для хранения оригинальных картинок
    private lateinit var previewDir: String // Директория для хранения превью

    private lateinit var cacheDir: String

    fun init(context: Context) {
        cacheDir = context.cacheDir.path

        originalDir = "$cacheDir/$ORIGINAL_DIRECTORY"
        previewDir = "$cacheDir/$PREVIEW_DIRECTORY"

        File(originalDir).mkdir()
        File(previewDir).mkdir()
    }

    // Загружаем картинку из интернета и сохраняем в двух экземплярах

    suspend fun loadImage(url: String): Boolean {
        return suspendCancellableCoroutine { cont ->
            if (!URLUtil.isValidUrl(url)) {
                cont.resumeWithException(ImageLoadException(url, "Не валидный урл"))
                return@suspendCancellableCoroutine
            }

            val request: Request = Request.Builder()
                .url(url)
                .build()

            val okHttpClient = trustAllImageClient()

            val response: Response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                cont.resumeWithException(ImageLoadException(url, "code=${response.code} and message=${response.message}"))
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

    private fun trustAllImageClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        val okHttpClient = OkHttpClient
            .Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
        return okHttpClient
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

        val width = get<ImageWidth>().value
        val height = bitmap.height * width / bitmap.width

        val previewBitmap = Bitmap.createScaledBitmap(
            bitmap,
            width,
            height,
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

    private fun urlToOriginalPath(url: String): String = originalDir + "/" + urlToFilename(url)

    private fun urlToPreviewPath(url: String): String = previewDir + "/" + urlToFilename(url)

    // Удаляем превью и оригинал с диска

    fun removeBothImages(url: String) {
        File(urlToOriginalPath(url)).delete()
        File(urlToPreviewPath(url)).delete()
    }
}

