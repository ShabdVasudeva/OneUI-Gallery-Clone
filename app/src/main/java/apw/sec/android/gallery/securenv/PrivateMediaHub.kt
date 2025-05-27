package apw.sec.android.gallery.securenv

import apw.sec.android.gallery.MediaFile

object PrivateMediaHub {

    private val registry = mutableMapOf<String, List<ImageItem>>()

    public fun save(key: String, mediaFiles: List<ImageItem>){
        registry[key] = mediaFiles
    }

    public fun get(key: String): List<ImageItem>?{
        return registry[key]
    }

    public fun remove(key: String){
        registry.remove(key)
    }

}