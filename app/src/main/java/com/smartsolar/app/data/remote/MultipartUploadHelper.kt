package com.smartsolar.app.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

/**
 * Safely reads user-selected images via ContentResolver (Photo Picker URIs)
 * and enforces MIME type & maximum file size checks before multipart upload.
 */
object MultipartUploadHelper {

    private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024 // 5 MB limit
    private val allowedMimeTypes = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")

    sealed class PreparedPart {
        data class Valid(val part: MultipartBody.Part) : PreparedPart()
        data class Invalid(val arabicReason: String) : PreparedPart()
    }

    fun prepareImagePart(
        context: Context,
        uri: Uri,
        formFieldName: String
    ): PreparedPart {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)?.lowercase() ?: "image/jpeg"
        if (mimeType !in allowedMimeTypes) {
            return PreparedPart.Invalid("نوع الملف غير مدعوم. يُسمح فقط بصور JPG و PNG و WEBP.")
        }

        val fileName = resolveSafeFileName(context, uri, mimeType)

        return try {
            resolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                var total = 0
                while (true) {
                    val read = input.read(chunk)
                    if (read == -1) break
                    total += read
                    if (total > MAX_IMAGE_BYTES) {
                        return PreparedPart.Invalid("حجم الصورة يتجاوز الحد المسموح (5 ميجابايت).")
                    }
                    buffer.write(chunk, 0, read)
                }
                val bytes = buffer.toByteArray()
                if (bytes.isEmpty()) {
                    return PreparedPart.Invalid("تعذر قراءة بيانات الصورة المختارة.")
                }
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                PreparedPart.Valid(
                    MultipartBody.Part.createFormData(formFieldName, fileName, requestBody)
                )
            } ?: PreparedPart.Invalid("تعذر فتح الصورة المختارة من الجهاز.")
        } catch (e: Exception) {
            PreparedPart.Invalid("حدث خطأ أثناء تجهيز الصورة للرفع: ${e.localizedMessage ?: "خطأ غير معروف"}")
        }
    }

    fun textPart(value: String): RequestBody {
        return value.toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())
    }

    private fun resolveSafeFileName(context: Context, uri: Uri, mimeType: String): String {
        val ext = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        var displayName: String? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) displayName = cursor.getString(idx)
            }
        }
        val sanitized = displayName
            ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
            ?.take(60)
            ?.takeIf { it.isNotBlank() }
        return sanitized ?: "smart_solar_${System.currentTimeMillis()}.$ext"
    }
}
