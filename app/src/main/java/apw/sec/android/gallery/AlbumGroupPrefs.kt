package apw.sec.android.gallery

import android.content.Context
import android.content.SharedPreferences

object AlbumGroupPrefs {
    private const val PREF_NAME = "album_groups"
    private const val KEY_PREFIX = "group_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setGroup(context: Context, folderName: String, groupName: String) {
        getPrefs(context).edit()
            .putString(KEY_PREFIX + folderName, groupName)
            .apply()
    }

    fun getGroup(context: Context, folderName: String?): String? {
        if (folderName == null) return null
        return getPrefs(context).getString(KEY_PREFIX + folderName, null)
    }

    fun clearGroup(context: Context, folderName: String) {
        getPrefs(context).edit()
            .remove(KEY_PREFIX + folderName)
            .apply()
    }

    fun getAllGroups(context: Context): Map<String, String> {
        val prefs = getPrefs(context)
        val allEntries = prefs.all
        val groups = mutableMapOf<String, String>()

        for ((key, value) in allEntries) {
            if (key.startsWith(KEY_PREFIX) && value is String) {
                val folderName = key.removePrefix(KEY_PREFIX)
                groups[folderName] = value
            }
        }

        return groups
    }
}