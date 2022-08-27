package to.sava.cloudmarksandroid.modules

import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.view.WindowManager
import android.widget.Toast
import androidx.work.WorkManager

fun Context.toast(message: CharSequence, length: Int = Toast.LENGTH_SHORT): Toast =
    Toast.makeText(this, message, length).apply {
        show()
    }

fun Context.toast(message: Int, length: Int = Toast.LENGTH_SHORT): Toast =
    Toast.makeText(this, message, length).apply {
        show()
    }

val Context.clipboardManager: ClipboardManager
    get() = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

val Context.notificationManager: NotificationManager
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

val Context.windowManager: WindowManager
    get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

val Context.workerManager: WorkManager
    get() = WorkManager.getInstance(this)
