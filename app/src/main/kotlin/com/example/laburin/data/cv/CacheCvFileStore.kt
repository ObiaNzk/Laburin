package com.example.laburin.data.cv

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** The CV being shown lives in the app's private cache: no other app can read it, and the system may clear it. */
class CacheCvFileStore @Inject constructor(@param:ApplicationContext private val context: Context) : CvFileStore {

    private val file get() = File(File(context.cacheDir, DIR), FILE_NAME)

    override suspend fun save(bytes: ByteArray): String = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        file.absolutePath
    }

    /** Small file in the app's cache: fast enough to delete from onCleared. */
    override fun delete() {
        file.delete()
    }

    private companion object {
        const val DIR = "cv"
        const val FILE_NAME = "mi-cv.pdf"
    }
}
