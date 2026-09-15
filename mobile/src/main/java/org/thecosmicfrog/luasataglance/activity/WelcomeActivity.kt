/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2026 Aaron Hastings
 *
 * This file is part of Luas at a Glance.
 *
 * Luas at a Glance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Luas at a Glance is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Luas at a Glance.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thecosmicfrog.luasataglance.activity

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.PathInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.databinding.ActivityWelcomeBinding
import org.thecosmicfrog.luasataglance.util.Preferences
import org.thecosmicfrog.luasataglance.util.ThemeUtil

/**
 * The first thing a user sees after installing the app.
 */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    private var tramAnimator: ObjectAnimator? = null

    private var isLeaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWelcomeBinding.inflate(layoutInflater)

        setContentView(binding.root)

        applyWindowInsets()
        checkCurrentTheme()
        listenForThemeChanges()

        binding.buttonWelcomeGetStarted.setOnClickListener {
            Preferences.saveWelcomeShown(this, true)

            animateExit()
        }

        /* Stop the tram from re-animating if a user picks a new theme. */
        val hasAnimatedIn = savedInstanceState?.getBoolean(STATE_HAS_ANIMATED_IN) == true

        animateTram(hasAnimatedIn)

        if (hasAnimatedIn) {
            showContentImmediately()
        } else {
            animateContentIn()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(STATE_HAS_ANIMATED_IN, true)
    }

    override fun onPause() {
        super.onPause()

        /* Prevent the Welcome screen showing on every launch. */
        if (isFinishing) Preferences.saveWelcomeShown(this, true)
    }

    override fun onDestroy() {
        super.onDestroy()

        tramAnimator?.cancel()
        tramAnimator = null

        binding.imageviewWelcomeTram.animate().cancel()
    }

    /**
     * Keep the title clear of the status bar and the "Get started" button clear of the navigation bar, and put light icons in both
     * bars.
     */
    private fun applyWindowInsets() {
        enableEdgeToEdge()

        /* Ensure the system status and navigation bars are light in colour regardless of light/dark mode. */
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.updatePadding(top = insets.top, bottom = insets.bottom)

            WindowInsetsCompat.CONSUMED
        }
    }

    /**
     * Bring the tram in from off the right and "brake" it to a stop.
     *
     * @param alreadyArrived Put the tram straight at its stopped position, for a recreate after a theme change.
     */
    private fun animateTram(alreadyArrived: Boolean) {
        val stage = binding.framelayoutWelcomeStage
        val tram = binding.imageviewWelcomeTram

        stage.doOnLayout {
            /* The tram's nose comes to rest on the left edge of the card below it. */
            val noseOffset = tram.width * NOSE_OFFSET_FRACTION
            val restX = resources.getDimension(R.dimen.activity_horizontal_margin) - noseOffset

            if (alreadyArrived) {
                tram.translationX = restX

                return@doOnLayout
            }

            tram.translationX = stage.width.toFloat()

            tramAnimator = ObjectAnimator.ofFloat(tram, View.TRANSLATION_X, stage.width.toFloat(), restX).apply {
                duration = DURATION_TRAM_MS
                interpolator = INTERPOLATOR_ARRIVE

                start()
            }
        }
    }

    /**
     * Fade the title in as it falls from the top, then bring the card and the button up behind it.
     */
    private fun animateContentIn() {
        binding.textviewWelcomeTitle.apply {
            alpha = 0f
            translationY = -resources.getDimension(R.dimen.welcome_title_slide)

            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(DELAY_TITLE_MS)
                .setDuration(DURATION_TITLE_MS)
                .setInterpolator(INTERPOLATOR_DECELERATE)
        }

        val rise = resources.getDimension(R.dimen.welcome_content_rise)

        listOf(binding.cardviewWelcomeTheme, binding.buttonWelcomeGetStarted).forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = rise

            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(DELAY_CONTENT_MS + index * DELAY_CONTENT_STAGGER_MS)
                .setDuration(DURATION_CONTENT_MS)
                .setInterpolator(INTERPOLATOR_DECELERATE)
        }
    }

    /**
     * Run [animateContentIn] backwards, then let the tram pull out of the stop and take the screen with it.
     */
    private fun animateExit() {
        /* "Get started" stays tappable through its own fade, and a second tap would restart the departure. */
        if (isLeaving) return

        isLeaving = true

        binding.buttonWelcomeGetStarted.isEnabled = false

        /* A theme tapped on the way out recreates the activity, which drops the animation and leaves the screen up. */
        binding.togglegroupWelcomeTheme.children.forEach { it.isEnabled = false }

        binding.textviewWelcomeTitle.animate()
            .alpha(0f)
            .translationY(-resources.getDimension(R.dimen.welcome_title_slide))
            .setStartDelay(DELAY_CONTENT_OUT_STAGGER_MS * 2)
            .setDuration(DURATION_CONTENT_OUT_MS)
            .setInterpolator(INTERPOLATOR_DEPART)

        val drop = resources.getDimension(R.dimen.welcome_content_rise)

        /* The button led the way in last, so it leads the way out first. */
        listOf(binding.buttonWelcomeGetStarted, binding.cardviewWelcomeTheme).forEachIndexed { index, view ->
            view.animate()
                .alpha(0f)
                .translationY(drop)
                .setStartDelay(index * DELAY_CONTENT_OUT_STAGGER_MS)
                .setDuration(DURATION_CONTENT_OUT_MS)
                .setInterpolator(INTERPOLATOR_DEPART)
        }

        departTram()
    }

    /**
     * Accelerate the tram off the left edge and close the screen once its tail has gone with it.
     */
    private fun departTram() {
        val tram = binding.imageviewWelcomeTram

        /* A tap before the tram has finished arriving departs from wherever it got to. */
        tramAnimator?.cancel()
        tramAnimator = null

        /* The drawable is much wider than an average phone screen so most of the travel is the body streaming past. */
        tram.animate()
            .translationX(-tram.width.toFloat())
            .setStartDelay(DELAY_TRAM_DEPART_MS)
            .setDuration(DURATION_TRAM_DEPART_MS)
            .setInterpolator(INTERPOLATOR_DEPART)
            .withEndAction { finish() }
    }

    /**
     * Put the content where [animateContentIn] would leave it, for a recreate after a theme change.
     */
    private fun showContentImmediately() {
        listOf(binding.textviewWelcomeTitle, binding.cardviewWelcomeTheme, binding.buttonWelcomeGetStarted).forEach {
            it.alpha = 1f
            it.translationY = 0f
        }
    }

    /**
     * Check the button for the theme the app is already in, before anything listens for a change.
     */
    private fun checkCurrentTheme() {
        val theme = Preferences.theme(this)

        binding.togglegroupWelcomeTheme.check(
            when (theme) {
                getString(R.string.pref_value_theme_light) -> R.id.button_welcome_theme_light
                getString(R.string.pref_value_theme_dark) -> R.id.button_welcome_theme_dark
                else -> R.id.button_welcome_theme_system
            }
        )
    }

    /**
     * Write a tapped theme to the same preference SettingsFragment reads, then apply it.
     */
    private fun listenForThemeChanges() {
        binding.togglegroupWelcomeTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            /* The listener fires twice for a change, once for the button being cleared and once for the one being checked. */
            if (!isChecked) return@addOnButtonCheckedListener

            val theme = when (checkedId) {
                R.id.button_welcome_theme_light -> getString(R.string.pref_value_theme_light)
                R.id.button_welcome_theme_dark -> getString(R.string.pref_value_theme_dark)
                else -> getString(R.string.pref_value_theme_system)
            }

            Preferences.saveTheme(this, theme)

            ThemeUtil.applyPickedTheme(this, theme)
        }
    }

    companion object {
        private const val STATE_HAS_ANIMATED_IN = "hasAnimatedIn"

        private const val DURATION_TRAM_MS = 1800L

        /* How far into the drawable the nose tip is. */
        private const val NOSE_OFFSET_FRACTION = 4f / 571f

        private const val DELAY_TITLE_MS = 150L
        private const val DURATION_TITLE_MS = 500L

        private const val DELAY_CONTENT_MS = 400L
        private const val DELAY_CONTENT_STAGGER_MS = 100L
        private const val DURATION_CONTENT_MS = 400L

        private const val DELAY_CONTENT_OUT_STAGGER_MS = 60L
        private const val DURATION_CONTENT_OUT_MS = 250L

        private const val DELAY_TRAM_DEPART_MS = 150L
        private const val DURATION_TRAM_DEPART_MS = 1700L

        /* Decelerate easing, for everything arriving. */
        private val INTERPOLATOR_DECELERATE = PathInterpolator(0f, 0f, 0.2f, 1f)

        /* Emphasized accelerate, the mirror of the above. Used for everything leaving when "Get started" is tapped. */
        private val INTERPOLATOR_DEPART = PathInterpolator(0.3f, 0f, 1f, 1f)

        /* Emphasized decelerate. A harder version of the above. Used only for the tram coming to a stand. */
        private val INTERPOLATOR_ARRIVE = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
    }
}
