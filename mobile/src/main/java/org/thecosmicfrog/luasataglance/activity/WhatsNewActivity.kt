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

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.databinding.ActivityWhatsNewBinding

class WhatsNewActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityWhatsNewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityWhatsNewBinding.inflate(layoutInflater)
        val rootView = viewBinding.root

        setContentView(rootView)

        formatAndSetWhatsNewTitles()
        formatAndSetWhatsNewContent()
    }

    private fun formatAndSetWhatsNewTitles() {
        val textViewWhatsNewTitleCurrent = viewBinding.textviewWhatsnewTitleCurrent
        textViewWhatsNewTitleCurrent.text = String.format(
                getString(R.string.whatsnew_title_current),
                getString(R.string.version_name),
                getString(R.string.release_date)
        )
    }

    /**
     * Turns each line of `whatsnew_content_current` into a bulleted paragraph.
     */
    private fun formatAndSetWhatsNewContent() {
        val gapWidth = resources.getDimensionPixelSize(R.dimen.whatsnew_bullet_gap)
        val bulletRadius = resources.getDimensionPixelSize(R.dimen.whatsnew_bullet_radius)
        val bulletColor = ContextCompat.getColor(this, R.color.on_surface)

        val content = SpannableStringBuilder()
        getString(R.string.whatsnew_content_current).split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            .forEachIndexed { index, line ->
                if (index > 0) content.append('\n')
                val start = content.length
                content.append(line)
                content.setSpan(
                    BulletSpan(gapWidth, bulletColor, bulletRadius),
                    start,
                    content.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

        viewBinding.textviewWhatsnewContentCurrent.text = content
    }
}

