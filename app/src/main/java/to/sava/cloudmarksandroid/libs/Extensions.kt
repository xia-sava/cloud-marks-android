package to.sava.cloudmarksandroid.libs

import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.view.WindowManager
import android.widget.Toast

fun Context.toast(message: CharSequence): Toast =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).apply {
        show()
    }

fun Context.toast(message: Int): Toast =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).apply {
        show()
    }

val Context.clipboardManager: ClipboardManager
    get() = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

val Context.notificationManager: NotificationManager
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

val Context.windowManager: WindowManager
    get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

