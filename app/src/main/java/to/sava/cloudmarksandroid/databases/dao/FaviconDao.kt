package to.sava.cloudmarksandroid.databases.dao

import androidx.room.*
import to.sava.cloudmarksandroid.databases.models.Favicon

@Dao
interface FaviconDao {
    @Query("SELECT * FROM favicon WHERE domain = :domain")
    fun findFavicon(domain: String): Favicon?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(favicon: Favicon): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(favicons: List<Favicon>): List<Long>
}