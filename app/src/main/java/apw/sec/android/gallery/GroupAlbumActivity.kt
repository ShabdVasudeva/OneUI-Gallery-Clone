package apw.sec.android.gallery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import apw.sec.android.gallery.AlbumAdapter
import apw.sec.android.gallery.Albums
import apw.sec.android.gallery.R
import apw.sec.android.gallery.GroupAlbumsFragment
import dev.oneuiproject.oneui.layout.ToolbarLayout

class GroupAlbumActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_album)

        val groupName = intent.getStringExtra("groupName") ?: return

        val toolbar = findViewById<ToolbarLayout>(R.id.toolbar)
        toolbar.setNavigationButtonAsBack()
        toolbar.toolbar.title = groupName

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.placeholder,
                GroupAlbumsFragment.newInstance(groupName)
            )
            .commit()
    }

}
