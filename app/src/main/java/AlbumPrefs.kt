package apw.sec.android.gallery

import android.content.Context


object AlbumPrefs {
        private const val PREF_NAME = "EssentialAlbums"
        private const val KEY_ESSENTIAL_ALBUMS = "essential_album_list"

    
        fun getEssentialAlbums(context: Context): Set<String> {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getStringSet(KEY_ESSENTIAL_ALBUMS, emptySet()) ?: emptySet()
        }

        fun setEssentialAlbums(context: Context, albums: Set<String>) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putStringSet(KEY_ESSENTIAL_ALBUMS, albums).apply()
        }

        fun toggleEssential(context: Context, album: String) {
            val current = getEssentialAlbums(context).toMutableSet()
            if (current.contains(album)) {
                current.remove(album)
            } else {
                current.add(album)
            }
            setEssentialAlbums(context, current)
        }
    }

