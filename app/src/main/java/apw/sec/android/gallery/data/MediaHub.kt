package apw.sec.android.gallery.data;

import apw.sec.android.gallery.MediaFile

object MediaHub {
    
    private val registry = mutableMapOf<String, List<MediaFile>>()

    fun save(key: String, mediaFiles: List<MediaFile>){
        registry[key] = mediaFiles
    }

    fun get(key: String): List<MediaFile>?{
        return registry[key]
    }

    fun remove(key: String){
        registry.remove(key)
    }
}