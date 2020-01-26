package to.sava.cloudmarksandroid.databases

import android.util.Base64
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import to.sava.cloudmarksandroid.databases.dao.FaviconDao
import to.sava.cloudmarksandroid.databases.dao.MarkNodeDao
import to.sava.cloudmarksandroid.databases.models.Favicon
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.databases.models.MarkType

@Database(entities = [MarkNode::class, Favicon::class], version = 1)
@TypeConverters(MarkTypeConverter::class, ByteArrayConverter::class)
abstract class CloudMarksAndroidDatabase : RoomDatabase() {

    abstract fun markNodeDao(): MarkNodeDao
    abstract fun faviconDao(): FaviconDao
}

class ByteArrayConverter {
    @TypeConverter
    fun fromByteArray(byteArray: ByteArray): String {
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    @TypeConverter
    fun toByteArray(base64: String): ByteArray {
        return Base64.decode(base64, Base64.NO_WRAP)
    }
}

class MarkTypeConverter {
    @TypeConverter
    fun toMarkType(typeValue: Int): MarkType? {
        return MarkType.values().first { it.rawValue == typeValue }
    }

    @TypeConverter
    fun fromMarkType(markType: MarkType): Int {
        return markType.rawValue
    }
}

