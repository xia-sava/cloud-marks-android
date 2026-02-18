package to.sava.cloudmarksandroid.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import to.sava.cloudmarksandroid.databases.dao.FaviconDao
import to.sava.cloudmarksandroid.databases.dao.MarkNodeDao
import to.sava.cloudmarksandroid.databases.models.Favicon
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.databases.models.MarkType

@Database(entities = [MarkNode::class, Favicon::class], version = 1, exportSchema = false)
@TypeConverters(MarkTypeConverter::class, ByteArrayConverter::class)
abstract class CloudMarksAndroidDatabase : RoomDatabase() {

    abstract fun markNodeDao(): MarkNodeDao
    abstract fun faviconDao(): FaviconDao
}

class ByteArrayConverter {
    @TypeConverter
    fun fromByteArray(byteArray: ByteArray): String {
        return java.util.Base64.getEncoder().encodeToString(byteArray)
    }

    @TypeConverter
    fun toByteArray(base64: String): ByteArray {
        return java.util.Base64.getDecoder().decode(base64)
    }
}

class MarkTypeConverter {
    @TypeConverter
    fun toMarkType(typeValue: Int): MarkType? {
        return MarkType.values().firstOrNull { it.rawValue == typeValue }
    }

    @TypeConverter
    fun fromMarkType(markType: MarkType): Int {
        return markType.rawValue
    }
}
