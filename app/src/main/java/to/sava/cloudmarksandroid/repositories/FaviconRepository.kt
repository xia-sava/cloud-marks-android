package to.sava.cloudmarksandroid.repositories

import to.sava.cloudmarksandroid.models.Favicon

class FaviconRepository {
    private val access by lazy { RealmAccess(Favicon::class) }


    fun findFavicon(domain: String): Favicon? {
        return access.find("domain", domain)
    }

    fun saveFavicons(favicons: List<Favicon>) {
        access.save(favicons)
    }
}
