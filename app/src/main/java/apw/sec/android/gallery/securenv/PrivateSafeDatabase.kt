package apw.sec.android.gallery.securenv

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PrivateSafeDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "Images.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "Images"
        private const val COLUMN_ID = "id"
        private const val COLUMN_PATH = "imagePath"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_PATH TEXT)"
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertImagePath(imagePath: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PATH, imagePath)
        }
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun getAllImagePaths(): List<ImageItem> {
        val db = readableDatabase
        val list = mutableListOf<ImageItem>()
        val cursor = db.rawQuery("SELECT $COLUMN_PATH FROM $TABLE_NAME", null)

        if (cursor.moveToFirst()) {
            do {
                val path = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATH))
                list.add(ImageItem(path))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
    
    fun deleteImagePath(imagePath: String): Boolean {
        val db = writableDatabase
        val deletedRows = db.delete(TABLE_NAME, "$COLUMN_PATH = ?", arrayOf(imagePath))
        db.close()
        return deletedRows > 0
    }
}