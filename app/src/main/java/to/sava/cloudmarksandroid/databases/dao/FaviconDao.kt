package to.sava.cloudmarksandroid.databases.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import to.sava.cloudmarksandroid.databases.models.Favicon

@Dao
interface FaviconDao {
    @Query("SELECT * FROM favicon WHERE domain = :domain")
    suspend fun findFavicon(domain: String): Favicon?

    @Query("SELECT * FROM favicon WHERE domain IN (:domains)")
    suspend fun findFavicons(domains: List<String>): List<Favicon>

    @Query("SELECT * FROM favicon")
    suspend fun findAllFavicons(): List<Favicon>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(favicon: Favicon): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(favicons: List<Favicon>): List<Long>
}