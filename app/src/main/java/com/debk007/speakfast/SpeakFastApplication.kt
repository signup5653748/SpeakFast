[Earlier content for app/src/main/java/com/debk007/speakfast/SpeakFastApplication.kt (3054 chars, already written to disk). The module surface below may not show every signature you need — re-read with read_file before editing this file again, OR before importing/calling anything from it in another file. Do not recreate it from scratch and do not guess a function's parameters from memory.
Module surface:
import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.io.IOException
import java.io.OutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@HiltAndroidApp
class SpeakFastApplication : Application() {
    @Suppress("UNUSED_PARAMETER") // the service reads its own active window, no context needed
    @Suppress("NewApi") // only reachable through the API 30+ screenshot path in triggerOcrFallback
    @Suppress("NewApi") // Bitmap.wrapHardwareBuffer is API 30+, guarded by the caller]