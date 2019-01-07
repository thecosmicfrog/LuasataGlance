/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2019 Aaron Hastings
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

package org.thecosmicfrog.luasataglance.activity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.thecosmicfrog.luasataglance.util.Constant;

public class PagerAdapter extends FragmentStatePagerAdapter {

    private int numTabs;

    public PagerAdapter(FragmentManager fm, int n) {
        super(fm);

        numTabs = n;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return LineFragment.newInstance(Constant.RED_LINE);

            case 1:
                return LineFragment.newInstance(Constant.GREEN_LINE);

            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return numTabs;
    }
}
