package apw.sec.android.gallery

import android.content.Context

object AlbumNamePrefs {

    private const val PREF = "album_names"

    fun setName(context: Context, folder: String, name: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(folder, name)
            .apply()
    }

    fun getName(context: Context, folder: String): String? {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(folder, null)
    }

    fun clear(context: Context, folder: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .remove(folder)
            .apply()
    }
}

