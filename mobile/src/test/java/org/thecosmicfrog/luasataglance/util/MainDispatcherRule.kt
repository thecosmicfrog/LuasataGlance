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
package org.thecosmicfrog.luasataglance.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Points Dispatchers.Main at a test dispatcher for one test, and puts it back afterwards.
 *
 * LineViewModel launches its work on viewModelScope, which uses Dispatchers.Main. Swapping that for a test dispatcher lets a
 * test say when the work runs, with runCurrent() and advanceUntilIdle(). Delays run on a virtual clock too, so
 * advanceTimeBy(3500.milliseconds) skips the auto-reload interval rather than the test sitting there for 3.5 seconds.
 *
 * @param testDispatcher Dispatcher to install. StandardTestDispatcher queues work until the test asks for it to run.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val testDispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
