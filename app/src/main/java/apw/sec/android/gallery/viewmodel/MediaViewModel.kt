package apw.sec.android.gallery.viewmodel;

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import apw.sec.android.gallery.MediaFile
import android.net.Uri

class MediaViewModel: ViewModel(){
    private val _mediaList = MutableLiveData<List<MediaFile>>()
    var mediaList: LiveData<List<MediaFile>> = _mediaList

    private val mediaMap: MutableMap<String, MediaFile> = mutableMapOf()

    fun saveMediaData(list: List<MediaFile>){
        _mediaList.value = list
        mediaMap.clear()
        list.forEach { media ->
            mediaMap[media.uri.toString()] = media
        }
    }

    fun getMediaAt(position: Int): MediaFile?{
        return _mediaList.value?.get(position)
    }

    fun getMediaByUri(uri: String): MediaFile?{
        return mediaMap[uri]
    }

    fun getAllData(): List<MediaFile>{
        return mediaMap.values.toList()
    }
}