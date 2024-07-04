package com.example.gridtestapp.core.cache

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
import org.apache.commons.io.FileUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File
import java.io.FileNotFoundException
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

    private lateinit var originalFileDir: File // Директория для хранения оригинальных картинок
    private lateinit var previewFileDir: File // Директория для хранения превью

    fun init(context: Context) {
        val cacheDir = context.cacheDir.path

        originalFileDir = File("$cacheDir/$ORIGINAL_DIRECTORY")
        previewFileDir = File("$cacheDir/$PREVIEW_DIRECTORY")

        originalFileDir.mkdir()
        previewFileDir.mkdir()
    }

    // Загружаем картинку из интернета и сохраняем в двух экземплярах

    suspend fun loadImage(url: String): Boolean {
        return suspendCancellableCoroutine { cont ->
            if (!URLUtil.isValidUrl(url)) {
                cont.resumeWithException(ImageLoadException(url, "Не валидный урл", validUrl = false))
                return@suspendCancellableCoroutine
            }

            val request: Request = Request.Builder()
                .url(url)
                .build()

            val okHttpClient = trustAllImageClient()

            val response: Response = try {
                okHttpClient.newCall(request).execute()
            } catch (exception: Exception) {
                cont.resumeWithException(ImageLoadException(url, validUrl = true, exception))
                return@suspendCancellableCoroutine
            }
                if (!response.isSuccessful) {
                val errorMessage = response.message.ifEmpty {
                    "Ошибка ${response.code}"
                }
                cont.resumeWithException(ImageLoadException(url, errorMessage, validUrl = true))
                response.close()
                return@suspendCancellableCoroutine
            }

            val inputStream = response.body!!.byteStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            response.close()

            try {
                saveOriginalImage(url, bitmap)
                savePreviewImage(url, bitmap)
            } catch (exception: FileNotFoundException) {
                cont.resumeWithException(ImageLoadException(url, validUrl = true, exception))
                return@suspendCancellableCoroutine
            } catch (throwable: Throwable) {
                cont.resumeWithException(ImageLoadException(url, validUrl = true, throwable))
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
        val out = FileOutputStream(urlToOriginalPath(url))
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.close()
    }

    // Сохраняем и сжимаем превью

    private fun savePreviewImage(url: String, bitmap: Bitmap) {
        val out = FileOutputStream(urlToPreviewPath(url))

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
        return getImageBitmap(filePath.path)!!
    }

    // Картинка-превью

    fun previewImageBitmap(url: String) : ImageBitmap? {
        val filePath = urlToPreviewPath(url)
        return getImageBitmap(filePath.path)
    }

    // Загружаем в ImageBitmap с диска

    private fun getImageBitmap(filePath: String): ImageBitmap? {
        val file = BitmapFactory.decodeFile(filePath) ?: return null
        return file.asImageBitmap()
    }

    fun isCached( url: String): Boolean {
        return urlToOriginalPath(url).exists() && urlToPreviewPath(url).exists()
    }

    fun isNotCached(url: String): Boolean = !isCached(url)

    // Преобразуем урл в имя файла, учитываются хост, путь и параметры запроса

    private fun urlToFilename(url: String): String {
        val uri = Uri.parse(url)
        return "${uri.host}_${uri.pathSegments.joinToString("_")}" + if (uri.query != null) "_${uri.query!!.replace(":", "_")}" else ""
    }

    private fun urlToOriginalPath(url: String): File = File(originalFileDir, urlToFilename(url))

    private fun urlToPreviewPath(url: String): File = File(previewFileDir, urlToFilename(url))

    // Удаляем превью и оригинал с диска

    fun removeBothImages(url: String) {
        urlToOriginalPath(url).delete()
        urlToPreviewPath(url).delete()
    }

    fun clearAll() {
        FileUtils.cleanDirectory(previewFileDir)
        FileUtils.cleanDirectory(originalFileDir)
    }
}

