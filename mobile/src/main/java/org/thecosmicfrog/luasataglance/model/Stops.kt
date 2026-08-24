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
package org.thecosmicfrog.luasataglance.model

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.util.Constant
import java.util.Locale

/**
 * A stop on the Luas network.
 *
 * @param id        The API's three-letter stop ID, for example "TAL". Sent to luas-api.php as the station parameter.
 * @param nameRes   The stop's name, resolved against the device's language by [Stops.name].
 * @param line      [Constant.RED_LINE] or [Constant.GREEN_LINE].
 * @param latitude  Latitude, used to place the stop's marker on the map.
 * @param longitude Longitude, used to place the stop's marker on the map.
 */
data class Stop(
    val id: String,
    @param:StringRes val nameRes: Int,
    val line: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Every Luas stop, and the lookups that turn one form of a stop into another.
 *
 * The stop ID is the only stable name a stop has. The name the user sees changes with the device language, and the name the API
 * sends back is always English, so neither works as a key. Everything here is keyed by the ID, and display names come from string
 * resources.
 */
object Stops {

    val redLine: List<Stop> = listOf(
        Stop("TPT", R.string.stop_tpt, Constant.RED_LINE, 53.34835000, -6.22925800),
        Stop("SDK", R.string.stop_sdk, Constant.RED_LINE, 53.34882200, -6.23714700),
        Stop("MYS", R.string.stop_mys, Constant.RED_LINE, 53.34924700, -6.24339400),
        Stop("GDK", R.string.stop_gdk, Constant.RED_LINE, 53.34952800, -6.24757500),
        Stop("CON", R.string.stop_con, Constant.RED_LINE, 53.35064343, -6.25009972),
        Stop("BUS", R.string.stop_bus, Constant.RED_LINE, 53.35011668, -6.25158298),
        Stop("ABB", R.string.stop_abb, Constant.RED_LINE, 53.34864260, -6.25818800),
        Stop("JER", R.string.stop_jer, Constant.RED_LINE, 53.34770945, -6.26526511),
        Stop("FOU", R.string.stop_fou, Constant.RED_LINE, 53.34685122, -6.27365506),
        Stop("SMI", R.string.stop_smi, Constant.RED_LINE, 53.34711061, -6.27807534),
        Stop("MUS", R.string.stop_mus, Constant.RED_LINE, 53.34787918, -6.28693736),
        Stop("HEU", R.string.stop_heu, Constant.RED_LINE, 53.34666463, -6.29169273),
        Stop("JAM", R.string.stop_jam, Constant.RED_LINE, 53.34178089, -6.29331028),
        Stop("FAT", R.string.stop_fat, Constant.RED_LINE, 53.33846589, -6.29278457),
        Stop("RIA", R.string.stop_ria, Constant.RED_LINE, 53.33790215, -6.29738188),
        Stop("SUI", R.string.stop_sui, Constant.RED_LINE, 53.33663693, -6.30726313),
        Stop("GOL", R.string.stop_gol, Constant.RED_LINE, 53.33591621, -6.31330883),
        Stop("DRI", R.string.stop_dri, Constant.RED_LINE, 53.33534603, -6.31827628),
        Stop("BLA", R.string.stop_bla, Constant.RED_LINE, 53.33426652, -6.32752991),
        Stop("BLU", R.string.stop_blu, Constant.RED_LINE, 53.32932028, -6.33388674),
        Stop("KYL", R.string.stop_kyl, Constant.RED_LINE, 53.32663549, -6.34380019),
        Stop("RED", R.string.stop_red, Constant.RED_LINE, 53.31675604, -6.36977577),
        Stop("KIN", R.string.stop_kin, Constant.RED_LINE, 53.30364059, -6.36546278),
        Stop("BEL", R.string.stop_bel, Constant.RED_LINE, 53.29929352, -6.37505436),
        Stop("COO", R.string.stop_coo, Constant.RED_LINE, 53.29329602, -6.38408160),
        Stop("HOS", R.string.stop_hos, Constant.RED_LINE, 53.28931347, -6.37892103),
        Stop("TAL", R.string.stop_tal, Constant.RED_LINE, 53.28740415, -6.37460375),
        Stop("FET", R.string.stop_fet, Constant.RED_LINE, 53.29336849, -6.39591122),
        Stop("CVN", R.string.stop_cvn, Constant.RED_LINE, 53.29104699, -6.40653276),
        Stop("CIT", R.string.stop_cit, Constant.RED_LINE, 53.28845599, -6.41762638),
        Stop("FOR", R.string.stop_for, Constant.RED_LINE, 53.28424849, -6.42475033),
        Stop("SAG", R.string.stop_sag, Constant.RED_LINE, 53.28483859, -6.43777514)
    )

    val greenLine: List<Stop> = listOf(
        Stop("BRO", R.string.stop_bro, Constant.GREEN_LINE, 53.37254168, -6.29840233),
        Stop("CAB", R.string.stop_cab, Constant.GREEN_LINE, 53.36385473, -6.28157952),
        Stop("PHI", R.string.stop_phi, Constant.GREEN_LINE, 53.36009486, -6.27861970),
        Stop("GRA", R.string.stop_gra, Constant.GREEN_LINE, 53.35727273, -6.27731346),
        Stop("BRD", R.string.stop_brd, Constant.GREEN_LINE, 53.35407420, -6.27392315),
        Stop("DOM", R.string.stop_dom, Constant.GREEN_LINE, 53.35124424, -6.26531198),
        Stop("PAR", R.string.stop_par, Constant.GREEN_LINE, 53.35301900, -6.26047044),
        Stop("OUP", R.string.stop_oup, Constant.GREEN_LINE, 53.35165250, -6.26117601),
        Stop("OGP", R.string.stop_ogp, Constant.GREEN_LINE, 53.34880980, -6.25992879),
        Stop("MAR", R.string.stop_mar, Constant.GREEN_LINE, 53.34915970, -6.25775202),
        Stop("WES", R.string.stop_wes, Constant.GREEN_LINE, 53.34623832, -6.25914424),
        Stop("TRY", R.string.stop_try, Constant.GREEN_LINE, 53.34518877, -6.25865324),
        Stop("DAW", R.string.stop_daw, Constant.GREEN_LINE, 53.34209496, -6.25801637),
        Stop("STS", R.string.stop_sts, Constant.GREEN_LINE, 53.33911033, -6.26139200),
        Stop("HAR", R.string.stop_har, Constant.GREEN_LINE, 53.33364891, -6.26269019),
        Stop("CHA", R.string.stop_cha, Constant.GREEN_LINE, 53.33060239, -6.25862396),
        Stop("RAN", R.string.stop_ran, Constant.GREEN_LINE, 53.32613311, -6.25619924),
        Stop("BEE", R.string.stop_bee, Constant.GREEN_LINE, 53.32093278, -6.25462210),
        Stop("COW", R.string.stop_cow, Constant.GREEN_LINE, 53.31639199, -6.25344193),
        Stop("MIL", R.string.stop_mil, Constant.GREEN_LINE, 53.30967275, -6.25174391),
        Stop("WIN", R.string.stop_win, Constant.GREEN_LINE, 53.30174559, -6.25064689),
        Stop("DUN", R.string.stop_dun, Constant.GREEN_LINE, 53.29242537, -6.24511617),
        Stop("BAL", R.string.stop_bal, Constant.GREEN_LINE, 53.28605533, -6.23670495),
        Stop("KIL", R.string.stop_kil, Constant.GREEN_LINE, 53.28296371, -6.22410393),
        Stop("STI", R.string.stop_sti, Constant.GREEN_LINE, 53.27934264, -6.21025300),
        Stop("SAN", R.string.stop_san, Constant.GREEN_LINE, 53.27763303, -6.20462036),
        Stop("CPK", R.string.stop_cpk, Constant.GREEN_LINE, 53.27016831, -6.20383715),
        Stop("GLE", R.string.stop_gle, Constant.GREEN_LINE, 53.26626702, -6.20992577),
        Stop("GAL", R.string.stop_gal, Constant.GREEN_LINE, 53.26114604, -6.20584881),
        Stop("LEO", R.string.stop_leo, Constant.GREEN_LINE, 53.25829972, -6.19834936),
        Stop("BAW", R.string.stop_baw, Constant.GREEN_LINE, 53.25506809, -6.18441796),
        Stop("CCK", R.string.stop_cck, Constant.GREEN_LINE, 53.25436204, -6.17160237),
        Stop("LAU", R.string.stop_lau, Constant.GREEN_LINE, 53.25063905, -6.15495121),
        Stop("CHE", R.string.stop_che, Constant.GREEN_LINE, 53.24538459, -6.14582634),
        Stop("BRI", R.string.stop_bri, Constant.GREEN_LINE, 53.24186949, -6.14277935)
    )

    val all: List<Stop> = redLine + greenLine

    private val byId: Map<String, Stop> = all.associateBy { it.id }

    /**
     * Maps English name to stop ID, built from the English resources whatever language the device is set to.
     */
    @Volatile
    private var englishNameToId: Map<String, String>? = null

    /**
     * Maps the name shown to the user back to a stop ID, for the language the app is currently running in.
     */
    @Volatile
    private var nameToId: Pair<Locale, Map<String, String>>? = null

    /**
     * The stop with the given stop ID.
     *
     * @param id Stop ID.
     * @return The stop.
     */
    fun byId(id: String?): Stop? = id?.let { byId[it] }

    /**
     * The stop's name in the device's language.
     *
     * @param context Context.
     * @param id      Stop ID, for example "TAL".
     * @return The name to display.
     */
    fun name(context: Context, id: String?): String? = byId(id)?.let { context.getString(it.nameRes) }

    /**
     * The line a stop belongs to - either [Constant.RED_LINE] or [Constant.GREEN_LINE].
     *
     * @param id Stop ID.
     * @return The line the stop is on.
     */
    fun line(id: String?): String? = byId(id)?.line

    /**
     * The stop ID for a name shown to the user, for example, the stop picked in the spinner or read back from a favourites file.
     * Matched against the device's current language.
     *
     * @param context Context.
     * @param name    Stop name as displayed.
     * @return The stop ID, or null if no stop in this language has that name.
     */
    fun idForName(context: Context, name: String?): String? {
        if (name == null) return null

        val locale = context.resources.configuration.locales[0]
        val cached = nameToId

        val map = if (cached != null && cached.first == locale) {
            cached.second
        } else {
            all.associate { context.getString(it.nameRes) to it.id }.also { nameToId = locale to it }
        }

        return map[name]
    }

    /**
     * The stop ID for an English stop name.
     *
     * The API answers in English regardless of the device's language, so this reads the English resources directly rather than
     * whichever ones are loaded.
     *
     * @param context Context.
     * @param name    Stop name in English.
     * @return The stop ID.
     */
    fun idForEnglishName(context: Context, name: String?): String? {
        if (name == null) return null

        val map = englishNameToId ?: buildEnglishNameToId(context).also { englishNameToId = it }

        return map[name]
    }

    /**
     * Translates a destination from the English name the API sends into the device's language.
     *
     * @param context Context.
     * @param apiName Destination as it arrived from the API, always English.
     * @return The translated name, or [apiName] unchanged when the destination is not one of our stops.
     */
    fun localiseApiName(context: Context, apiName: String?): String? {
        if (apiName == null) return null

        return name(context, idForEnglishName(context, apiName)) ?: apiName
    }

    /**
     * Builds the English name to stop ID map by reading every stop's name from a Context forced to English.
     *
     * @param context Context.
     * @return English stop name to stop ID.
     */
    private fun buildEnglishNameToId(context: Context): Map<String, String> {
        val english = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(Locale.ENGLISH) }
        )

        return all.associate { english.getString(it.nameRes) to it.id }
    }
}
