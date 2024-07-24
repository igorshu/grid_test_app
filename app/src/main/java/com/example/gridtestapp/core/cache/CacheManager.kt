package com.example.gridtestapp.core.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.compose.ui.graphics.Color
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
import kotlin.math.floor
import kotlin.math.min

/*
*
*   Наш Кеш-менеджер
*
*   Умеет загружать картинки из интернета и сохранять в двух вариантах,
*   для превью и оригинал.
*
 */

typealias AndroidColor = android.graphics.Color

object CacheManager: KoinComponent {

    private const val ORIGINAL_DIRECTORY: String = "original_image_cache"
    private const val PREVIEW_DIRECTORY: String = "preview_image_cache"

    private lateinit var originalFileDir: File // Директория для хранения оригинальных картинок
    private lateinit var previewFileDir: File // Директория для хранения превью

    fun init(context: Context) {
        context.cacheDir.path.also { cacheDir ->
            File(cacheDir, ORIGINAL_DIRECTORY).apply {
                originalFileDir = this
                mkdirs()
            }
            File(cacheDir, PREVIEW_DIRECTORY).apply {
                previewFileDir = this
                mkdirs()
            }
        }
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

            val inputStream = response.body?.byteStream()
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

    private fun getAvgColor(bitmap: Bitmap, directionX: Int, directionY: Int, x: Int, y: Int, step: Int): Color {
        val avgColor = try {
            val count = 5
            val pixels = mutableListOf<Int>()
            repeat(count) {
                val pixelX = x + directionX * it * step
                val pixelY = y + directionY * it * step
                val pixel = bitmap.getPixel(pixelX, pixelY)
                pixels.add(pixel)
            }

            val size = pixels.size
            val colors =  pixels.fold(intArrayOf(0, 0, 0)) { acc, it ->
                acc[0] += AndroidColor.red(it)
                acc[1] += AndroidColor.green(it)
                acc[2] += AndroidColor.blue(it)
                acc
            }.map { it / size }

            Color(red = colors[0], green = colors[1], blue = colors[2])
        } catch (e: Exception) {
            Log.d("color", e.toString())
            e.printStackTrace()
            return Color.Cyan
        }
        return avgColor
    }

    fun imageColors(bitmap: Bitmap): ImageColors {
        return with(bitmap) {
            val step = floor(width.toFloat() / 25f).toInt()
            ImageColors(
                topLeft = getAvgColor(this, 1, 1, 0, 0, step),
                topRight = getAvgColor(this, -1, 1, width - 1, 0, step),
                bottomRight = getAvgColor(this, -1, -1, width - 1, height - 1, step),
                bottomLeft = getAvgColor(this, 1, -1, 0, height - 1, step),
                center = getAvgColor(this, 1, -1, width/2 - 10, height/2 - 10, step),
            )
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

        val squaredBitmap = createSquaredBitmap(bitmap)

        val side = min((get<ImageWidth>().pxWidth * 0.95).toInt(), squaredBitmap.width)

        val previewBitmap = Bitmap.createScaledBitmap(squaredBitmap, side, side, false)
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
        out.close()
    }

    private fun createSquaredBitmap(srcBmp: Bitmap): Bitmap {
        val side = min(srcBmp.width.toDouble(), srcBmp.height.toDouble()).toInt()
        val dstBmp = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(dstBmp)
        canvas.drawBitmap(srcBmp, (side - srcBmp.width).toFloat() / 2, (side - srcBmp.height).toFloat() / 2, null)

        return dstBmp
    }

    // Большая картинка

    fun originalImageBitmap(url: String) : ImageBitmap? {
        val filePath = urlToOriginalPath(url)
        return getImageBitmap(filePath.path)
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
        return "${uri.host}_${uri.pathSegments.joinToString("_")}_" + (uri.query?.replace(":", "_") ?: "")
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

