package com.example.utils

import com.yausername.youtubedl_android.YoutubeDLException
import java.net.UnknownHostException

/**
 * تحويل استثناءات YoutubeDL وأخطاء النظام إلى رسائل بالعربية مفهومة للمستخدم
 */
fun mapYoutubeDLException(e: Exception): String {
    val message = e.message ?: ""

    return when {
        e is UnknownHostException || message.contains("Unable to download webpage", ignoreCase = true) || message.contains("No address associated with hostname", ignoreCase = true) -> {
            "فشل الاتصال بالإنترنت. يرجى التحقق من اتصال شبكتك وإعادة المحاولة."
        }
        message.contains("Unsupported URL", ignoreCase = true) || message.contains("is not a valid URL", ignoreCase = true) -> {
            "الرابط غير مدعوم أو غير صحيح. يرجى إدخال رابط يوتيوب صالح."
        }
        message.contains("Video unavailable", ignoreCase = true) || message.contains("Private video", ignoreCase = true) || message.contains("this video has been removed", ignoreCase = true) -> {
            "الفيديو غير متاح أو تم حذفه أو خاص."
        }
        message.contains("No space left on device", ignoreCase = true) || message.contains("ENOSPC", ignoreCase = true) -> {
            "مساحة التخزين غير كافية على الجهاز لإتمام التحميل والدمج."
        }
        message.contains("merging failed", ignoreCase = true) || message.contains("FFmpeg", ignoreCase = true) -> {
            "حدث خطأ أثناء دمج الصوت بالفيديو. التأكد من وجود مساحة مؤقتة كافية."
        }
        e is YoutubeDLException -> {
            "حدث خطأ أثناء معالجة الفيديو: ${e.localizedMessage ?: "خطأ غير معروف"}"
        }
        else -> {
            e.localizedMessage ?: "حدث خطأ غير متوقع أثناء التحميل"
        }
    }
}
