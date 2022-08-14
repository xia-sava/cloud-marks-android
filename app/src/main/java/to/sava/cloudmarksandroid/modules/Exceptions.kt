package to.sava.cloudmarksandroid.modules

class ServiceAuthenticationException(override val message: String) : RuntimeException(message)

class DirectoryNotFoundException(override val message: String) : RuntimeException(message)

class FileNotFoundException(override val message: String) : RuntimeException(message)

class InvalidJsonException(override val message: String) : RuntimeException(message)
