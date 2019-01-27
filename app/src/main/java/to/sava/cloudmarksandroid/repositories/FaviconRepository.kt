package to.sava.cloudmarksandroid.repositories

import to.sava.cloudmarksandroid.models.Favicon

class FaviconRepository: Repository<Favicon>() {
    override val modelClass = Favicon::class.java
}