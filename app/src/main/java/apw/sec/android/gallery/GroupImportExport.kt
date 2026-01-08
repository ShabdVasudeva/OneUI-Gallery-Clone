package apw.sec.android.gallery

import android.content.Context
import android.net.Uri
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object GroupImportExport {

    private const val FILE_NAME_PREFIX = "gallery_groups_"
    private const val FILE_EXTENSION = ".json"

    fun exportGroups(context: Context): String? {
        val allItems = AlbumRepository.albumItems
        val groups = allItems.filterIsInstance<AlbumItem.Group>()

        if (groups.isEmpty()) {
            Toast.makeText(context, "No groups to export", Toast.LENGTH_SHORT).show()
            return null
        }

        val jsonArray = JSONArray()

        groups.forEach { group ->
            val groupObj = JSONObject()
            groupObj.put("groupName", group.groupName)

            val albumsArray = JSONArray()
            group.albums.forEach { album ->
                albumsArray.put(album.folderName)
            }

            groupObj.put("albums", albumsArray)
            jsonArray.put(groupObj)
        }

        val exportData = JSONObject()
        exportData.put("version", 1)
        exportData.put("exportDate", System.currentTimeMillis())
        exportData.put("groups", jsonArray)

        return exportData.toString(2)
    }

    fun importGroups(context: Context, jsonContent: String): Boolean {
        try {
            val jsonObject = JSONObject(jsonContent)
            val version = jsonObject.optInt("version", 1)

            if (version != 1) {
                Toast.makeText(context, "Unsupported file version", Toast.LENGTH_SHORT).show()
                return false
            }

            val groupsArray = jsonObject.getJSONArray("groups")
            var importedCount = 0

            for (i in 0 until groupsArray.length()) {
                val groupObj = groupsArray.getJSONObject(i)
                val groupName = groupObj.getString("groupName")
                val albumsArray = groupObj.getJSONArray("albums")

                // Apply the group to each album
                for (j in 0 until albumsArray.length()) {
                    val folderName = albumsArray.getString(j)
                    AlbumGroupPrefs.setGroup(context, folderName, groupName)
                }

                importedCount++
            }

            Toast.makeText(
                context,
                "Successfully imported $importedCount group(s)",
                Toast.LENGTH_SHORT
            ).show()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to import: ${e.message}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    fun readJsonFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "$FILE_NAME_PREFIX$timestamp$FILE_EXTENSION"
    }
}