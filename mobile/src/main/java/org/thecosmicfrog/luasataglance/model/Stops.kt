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
import java.text.Collator
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
        Stop("TPT", R.string.stop_tpt, Constant.RED_LINE, 53.3483500, -6.2292494),
        Stop("SDK", R.string.stop_sdk, Constant.RED_LINE, 53.3488162, -6.2371549),
        Stop("MYS", R.string.stop_mys, Constant.RED_LINE, 53.3492425, -6.2433895),
        Stop("GDK", R.string.stop_gdk, Constant.RED_LINE, 53.3495271, -6.2475862),
        Stop("CON", R.string.stop_con, Constant.RED_LINE, 53.3509029, -6.2499434),
        Stop("BUS", R.string.stop_bus, Constant.RED_LINE, 53.3501005, -6.2515134),
        Stop("ABB", R.string.stop_abb, Constant.RED_LINE, 53.3485991, -6.2581701),
        Stop("JER", R.string.stop_jer, Constant.RED_LINE, 53.3476682, -6.2655384),
        Stop("FOU", R.string.stop_fou, Constant.RED_LINE, 53.3468839, -6.2736719),
        Stop("SMI", R.string.stop_smi, Constant.RED_LINE, 53.3471411, -6.2777831),
        Stop("MUS", R.string.stop_mus, Constant.RED_LINE, 53.3478728, -6.2867337),
        Stop("HEU", R.string.stop_heu, Constant.RED_LINE, 53.3466556, -6.2917352),
        Stop("JAM", R.string.stop_jam, Constant.RED_LINE, 53.3418894, -6.2933258),
        Stop("FAT", R.string.stop_fat, Constant.RED_LINE, 53.3384450, -6.2925119),
        Stop("RIA", R.string.stop_ria, Constant.RED_LINE, 53.3379239, -6.2972259),
        Stop("SUI", R.string.stop_sui, Constant.RED_LINE, 53.3366232, -6.3072776),
        Stop("GOL", R.string.stop_gol, Constant.RED_LINE, 53.3359040, -6.3135696),
        Stop("DRI", R.string.stop_dri, Constant.RED_LINE, 53.3353827, -6.3180813),
        Stop("BLA", R.string.stop_bla, Constant.RED_LINE, 53.3342612, -6.3274123),
        Stop("BLU", R.string.stop_blu, Constant.RED_LINE, 53.3292783, -6.3338672),
        Stop("KYL", R.string.stop_kyl, Constant.RED_LINE, 53.3266325, -6.3435625),
        Stop("RED", R.string.stop_red, Constant.RED_LINE, 53.3168529, -6.3698728),
        Stop("KIN", R.string.stop_kin, Constant.RED_LINE, 53.3036915, -6.3652927),
        Stop("BEL", R.string.stop_bel, Constant.RED_LINE, 53.2989313, -6.3753675),
        Stop("COO", R.string.stop_coo, Constant.RED_LINE, 53.2935057, -6.3843875),
        Stop("HOS", R.string.stop_hos, Constant.RED_LINE, 53.2893911, -6.3788616),
        Stop("TAL", R.string.stop_tal, Constant.RED_LINE, 53.2874846, -6.3746666),
        Stop("FET", R.string.stop_fet, Constant.RED_LINE, 53.2935291, -6.3955244),
        Stop("CVN", R.string.stop_cvn, Constant.RED_LINE, 53.2909858, -6.4068424),
        Stop("CIT", R.string.stop_cit, Constant.RED_LINE, 53.2878282, -6.4189244),
        Stop("FOR", R.string.stop_for, Constant.RED_LINE, 53.2842550, -6.4245845),
        Stop("SAG", R.string.stop_sag, Constant.RED_LINE, 53.2846806, -6.4377305)
    )

    val greenLine: List<Stop> = listOf(
        Stop("BRO", R.string.stop_bro, Constant.GREEN_LINE, 53.3723298, -6.2980845),
        Stop("CAB", R.string.stop_cab, Constant.GREEN_LINE, 53.3641214, -6.2818005),
        Stop("PHI", R.string.stop_phi, Constant.GREEN_LINE, 53.3604269, -6.2789180),
        Stop("GRA", R.string.stop_gra, Constant.GREEN_LINE, 53.3571048, -6.2773370),
        Stop("BRD", R.string.stop_brd, Constant.GREEN_LINE, 53.3540701, -6.2737790),
        Stop("DOM", R.string.stop_dom, Constant.GREEN_LINE, 53.3514031, -6.2656306),
        Stop("PAR", R.string.stop_par, Constant.GREEN_LINE, 53.3530837, -6.2604719),
        Stop("OUP", R.string.stop_oup, Constant.GREEN_LINE, 53.3515905, -6.2610567),
        Stop("OGP", R.string.stop_ogp, Constant.GREEN_LINE, 53.3488443, -6.2599195),
        Stop("MAR", R.string.stop_mar, Constant.GREEN_LINE, 53.3491846, -6.2577558),
        Stop("WES", R.string.stop_wes, Constant.GREEN_LINE, 53.3463269, -6.2590154),
        Stop("TRY", R.string.stop_try, Constant.GREEN_LINE, 53.3453299, -6.2582726),
        Stop("DAW", R.string.stop_daw, Constant.GREEN_LINE, 53.3421210, -6.2579699),
        Stop("STS", R.string.stop_sts, Constant.GREEN_LINE, 53.3390623, -6.2613387),
        Stop("HAR", R.string.stop_har, Constant.GREEN_LINE, 53.3333614, -6.2626376),
        Stop("CHA", R.string.stop_cha, Constant.GREEN_LINE, 53.3307136, -6.2586977),
        Stop("RAN", R.string.stop_ran, Constant.GREEN_LINE, 53.3262911, -6.2561600),
        Stop("BEE", R.string.stop_bee, Constant.GREEN_LINE, 53.3208435, -6.2546447),
        Stop("COW", R.string.stop_cow, Constant.GREEN_LINE, 53.3164803, -6.2534403),
        Stop("MIL", R.string.stop_mil, Constant.GREEN_LINE, 53.3099143, -6.2517338),
        Stop("WIN", R.string.stop_win, Constant.GREEN_LINE, 53.3015888, -6.2507142),
        Stop("DUN", R.string.stop_dun, Constant.GREEN_LINE, 53.2923898, -6.2451342),
        Stop("BAL", R.string.stop_bal, Constant.GREEN_LINE, 53.2860630, -6.2367067),
        Stop("KIL", R.string.stop_kil, Constant.GREEN_LINE, 53.2830096, -6.2239119),
        Stop("STI", R.string.stop_sti, Constant.GREEN_LINE, 53.2793975, -6.2101681),
        Stop("SAN", R.string.stop_san, Constant.GREEN_LINE, 53.2776699, -6.2049294),
        Stop("CPK", R.string.stop_cpk, Constant.GREEN_LINE, 53.2701086, -6.2038486),
        Stop("GLE", R.string.stop_gle, Constant.GREEN_LINE, 53.2662849, -6.2100164),
        Stop("GAL", R.string.stop_gal, Constant.GREEN_LINE, 53.2611359, -6.2059372),
        Stop("LEO", R.string.stop_leo, Constant.GREEN_LINE, 53.2582697, -6.1983854),
        Stop("BAW", R.string.stop_baw, Constant.GREEN_LINE, 53.2550269, -6.1844281),
        Stop("CCK", R.string.stop_cck, Constant.GREEN_LINE, 53.2540326, -6.1699557),
        Stop("LAU", R.string.stop_lau, Constant.GREEN_LINE, 53.2506061, -6.1550060),
        Stop("CHE", R.string.stop_che, Constant.GREEN_LINE, 53.2453460, -6.1458747),
        Stop("BRI", R.string.stop_bri, Constant.GREEN_LINE, 53.2420531, -6.1428726)
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
     * The order stop names are shown in wherever the user picks from a list.
     *
     * A Collator for the device's language sorts an Irish fada in with its base letter.
     */
    val nameOrder: Comparator<CharSequence?>
        get() {
            val collator = Collator.getInstance(Locale.getDefault())
            return compareBy(collator) { it?.toString() ?: "" }
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
