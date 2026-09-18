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
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.databinding.ActivityWhatsNewBinding
import org.thecosmicfrog.luasataglance.databinding.ItemWhatsnewBinding

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
        viewBinding.textviewWhatsnewTitleCurrent.text = getString(
            R.string.whatsnew_title_current,
            getString(R.string.version_name),
            getString(R.string.release_date)
        )
    }

    /**
     * Shows each line of `whatsnew_content_current` as a bulleted item, one `item_whatsnew` row per line.
     */
    private fun formatAndSetWhatsNewContent() {
        val container = viewBinding.linearlayoutWhatsnewContentCurrent

        getString(R.string.whatsnew_content_current).split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            .forEachIndexed { index, line ->
                val item = ItemWhatsnewBinding.inflate(layoutInflater, container, false)
                item.textviewWhatsnewItem.text = line

                /* The container's own paddingTop separates the first item from the version title. */
                if (index == 0) item.root.updateLayoutParams<MarginLayoutParams> { topMargin = 0 }

                container.addView(item.root)
            }
    }
}
