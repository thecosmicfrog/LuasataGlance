package org.thecosmicfrog.luasataglance.activity;

import SettingsFragment
import android.graphics.Color
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentActivity
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.databinding.ActivitySettingsBinding

class SettingsActivity : FragmentActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)

        /* Use dialog theme. */
        setTheme(android.R.style.Theme_Material_Light_Dialog)

        window.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            requestFeature(Window.FEATURE_NO_TITLE)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }
}
