package com.example.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

/**
 * الحصول على مسار حفظ مجلد التحميلات المتوافق مع Scoped Storage (أندرويد 10+)
 */
fun getDownloadDirectory(context: Context): File {
    // استخدام المجلد العام للتحميلات في حالة توفره، وإلا المجلد الخاص بالتطبيق
    val baseDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // في أندرويد 10+ (Scoped Storage) يتم استخدام المجلد العام للتحميلات
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
    } else {
        // في أندرويد 9 وما قبله يتم استهداف مجلد التحميلات العام مباشرة
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
    }

    // التأكد من إنشاء المجلد في حالة عدم وجوده
    if (!baseDir.exists()) {
        baseDir.mkdirs()
    }

    return baseDir
}
