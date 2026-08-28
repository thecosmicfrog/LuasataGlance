import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.util.ThemeUtil

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        bindPreferenceSummaryToValue(
            findPreference(getString(R.string.pref_key_default_stop)),
            getString(R.string.none)
        )

        bindPreferenceSummaryToValue(
            findPreference(getString(R.string.pref_key_theme)),
            getString(R.string.pref_value_theme_system)
        )
    }

    private fun bindPreferenceSummaryToValue(preference: Preference?, defaultValue: String) {
        preference?.let {
            preference.onPreferenceChangeListener = this

            /* Trigger the listener with the preference's current value. */
            onPreferenceChange(
                preference,
                PreferenceManager
                    .getDefaultSharedPreferences(preference.context)
                    .getString(preference.key, defaultValue)!!
            )
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val stringValue = newValue.toString()

        if (preference.key == getString(R.string.pref_key_theme)) {
            ThemeUtil.applyTheme(preference.context, stringValue)
        }

        if (preference is ListPreference) {
            val prefIndex = preference.findIndexOfValue(stringValue)
            if (prefIndex >= 0) {
                preference.summary = preference.entries[prefIndex]
            }
        } else {
            preference.summary = stringValue
        }

        return true
    }
}
