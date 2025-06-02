package apw.sec.android.gallery

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import apw.sec.android.gallery.databinding.ActivitySettingsBinding
import android.app.*

class SettingsActivity : AppCompatActivity() {

    private var _binding: ActivitySettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportFragmentManager.beginTransaction()
            .replace(R.id.placeHolder, SettingsFrag())
            .commit()
        binding.toolbar.setNavigationButtonAsBack()    
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    class SettingsFrag : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, rootKey)

            val pref1 = findPreference<Preference>("pvt")
            val pref2 = findPreference<Preference>("about")
            val pref3 = findPreference<Preference>("app_info")
            val pref4 = findPreference<SwitchPreferenceCompat>("ENABLE_FILMSTRIP")

            pref4?.setOnPreferenceChangeListener { _, newValue ->
                val editor = activity?.getSharedPreferences("apw_gallery_preferences", 0)?.edit()
                editor?.putBoolean("ENABLE_FILMSTRIP", newValue as Boolean)
                editor?.apply()
                true
            }
            pref2?.setOnPreferenceClickListener {
                activity?.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/AndroidPortWorldDiscussion"))
                )
                true
            }
            
            pref3?.setOnPreferenceClickListener {
                activity?.startActivity(
                    Intent(activity?.getApplicationContext(), AboutActivity::class.java)
                )
                true
            }

            pref1?.setOnPreferenceClickListener {
                activity?.let { context ->
                    AlertDialog.Builder(context)
                        .setTitle("About Private Safe")
                        .setMessage(
                            "The private safe is a newly open-source secure environment feature in our gallery app to keep your media safe. " +
                            "You can delete your original picture after saving it in the private safe. " +
                            "Make sure not to delete application data to prevent private safe data loss. " +
                            "For other queries, kindly give feedback and follow us on our Telegram channel."
                        )
                        .setNegativeButton("Close", null)
                        .setPositiveButton("Visit our channel") { _, _ ->
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://t.me/AndroidPortWorldDiscussion")
                                )
                            )
                        }
                        .show()
                }
                true
            }
        }
    }
}