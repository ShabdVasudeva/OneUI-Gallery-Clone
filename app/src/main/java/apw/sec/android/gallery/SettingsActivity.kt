package apw.sec.android.gallery

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreferenceCompat
import apw.sec.android.gallery.databinding.ActivitySettingsBinding

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
        private var pendingExportData: String? = null

        // File picker for import
        private val importFilePicker = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { handleImport(it) }
        }

        // File saver for export
        private val exportFileSaver = registerForActivityResult(
            ActivityResultContracts.CreateDocument()
        ) { uri: Uri? ->
            uri?.let {
                pendingExportData?.let { data ->
                    handleExport(it, data)
                }
                pendingExportData = null
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, rootKey)

            // Apply haptic feedback to all switches
            applyHapticsToAllSwitches(preferenceScreen)

            val pref1 = findPreference<Preference>("pvt")
            val pref2 = findPreference<Preference>("about")
            val pref3 = findPreference<Preference>("app_info")
            val pref4 = findPreference<SwitchPreferenceCompat>("ENABLE_FILMSTRIP")
            val floatingTabPref = findPreference<SwitchPreferenceCompat>("ENABLE_FLOATING_TAB_BAR")

            // Import/Export preferences
            val importPref = findPreference<Preference>("Import")
            val exportPref = findPreference<Preference>("Export")

            pref4?.setOnPreferenceChangeListener { _, newValue ->
                val editor = activity?.getSharedPreferences("apw_gallery_preferences", 0)?.edit()
                editor?.putBoolean("ENABLE_FILMSTRIP", newValue as Boolean)
                editor?.apply()
                vibrateOnce()
                true
            }

            floatingTabPref?.setOnPreferenceChangeListener { _, newValue ->
                val editor = activity?.getSharedPreferences("apw_gallery_preferences", 0)?.edit()
                editor?.putBoolean("ENABLE_FLOATING_TAB_BAR", newValue as Boolean)
                editor?.apply()
                vibrateOnce()
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
                    Intent(activity?.applicationContext, AboutActivity::class.java)
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

            // Handle Export
            exportPref?.setOnPreferenceClickListener {
                activity?.let { context ->
                    val exportData = GroupImportExport.exportGroups(context)
                    if (exportData != null) {
                        // Store data temporarily and launch file saver
                        pendingExportData = exportData
                        try {
                            exportFileSaver.launch(GroupImportExport.generateFileName())
                        } catch (e: Exception) {
                            e.printStackTrace()
                            pendingExportData = null
                            Toast.makeText(context, "Failed to open file picker: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                true
            }

            // Handle Import
            importPref?.setOnPreferenceClickListener {
                try {
                    // Launch file picker for JSON files
                    importFilePicker.launch("application/json")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(activity, "Failed to open file picker: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                true
            }
        }

        private fun handleExport(uri: Uri, exportData: String) {
            activity?.let { context ->
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(exportData.toByteArray())
                    }
                    Toast.makeText(context, "Groups exported successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        private fun handleImport(uri: Uri) {
            activity?.let { context ->
                val jsonContent = GroupImportExport.readJsonFromUri(context, uri)
                if (jsonContent != null) {
                    val success = GroupImportExport.importGroups(context, jsonContent)
                    if (success) {
                        AlertDialog.Builder(context)
                            .setTitle("Import Successful")
                            .setMessage("Groups have been imported. Please go back to the Albums tab to see the changes.")
                            .setPositiveButton("OK") { _, _ ->
                                context.startActivity(
                                    Intent(context, MainActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                )
                            }
                            .show()
                    }
                } else {
                    Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun applyHapticsToAllSwitches(group: PreferenceGroup) {
            for (i in 0 until group.preferenceCount) {
                val pref = group.getPreference(i)
                if (pref is SwitchPreferenceCompat) {
                    pref.setOnPreferenceChangeListener { _, _ ->
                        vibrateOnce()
                        true
                    }
                } else if (pref is PreferenceGroup) {
                    applyHapticsToAllSwitches(pref)
                }
            }
        }

        private fun vibrateOnce() {
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}