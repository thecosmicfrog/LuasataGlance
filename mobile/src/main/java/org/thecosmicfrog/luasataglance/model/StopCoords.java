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

package org.thecosmicfrog.luasataglance.model;

import android.util.Log;

import org.thecosmicfrog.luasataglance.util.Constant;

public class StopCoords {

    private final String LOG_TAG = StopCoords.class.getSimpleName();

    private double[][] stopCoordsRedLine = new double[][] {
            { 53.34835000, -6.22925800 }, /* The Point */
            { 53.34882200, -6.23714700 }, /* Spencer Dock */
            { 53.34924700, -6.24339400 }, /* Mayor Square/NCI */
            { 53.34952800, -6.24757500 }, /* George's Dock */
            { 53.35064343, -6.25009972 }, /* Connolly */
            { 53.35011668, -6.25158298 }, /* Busáras */
            { 53.34864260, -6.25818800 }, /* Abbey Street */
            { 53.34770945, -6.26526511 }, /* Jervis */
            { 53.34685122, -6.27365506 }, /* Four Courts */
            { 53.34711061, -6.27807534 }, /* Smithfield */
            { 53.34787918, -6.28693736 }, /* Museum */
            { 53.34666463, -6.29169273 }, /* Heuston */
            { 53.34178089, -6.29331028 }, /* James's */
            { 53.33846589, -6.29278457 }, /* Fatima */
            { 53.33790215, -6.29738188 }, /* Rialto */
            { 53.33663693, -6.30726313 }, /* Suir Road */
            { 53.33591621, -6.31330883 }, /* Goldenbridge */
            { 53.33534603, -6.31827628 }, /* Drimnagh */
            { 53.33426652, -6.32752991 }, /* Blackhorse */
            { 53.32932028, -6.33388674 }, /* Bluebell */
            { 53.32663549, -6.34380019 }, /* Kylemore */
            { 53.31675604, -6.36977577 }, /* Red Cow */
            { 53.30364059, -6.36546278 }, /* Kingswood */
            { 53.29929352, -6.37505436 }, /* Belgard */
            { 53.29329602, -6.38408160 }, /* Cookstown */
            { 53.28931347, -6.37892103 }, /* Hospital */
            { 53.28740415, -6.37460375 }, /* Tallaght */
            { 53.29336849, -6.39591122 }, /* Fettercairn */
            { 53.29104699, -6.40653276 }, /* Cheeverstown */
            { 53.28845599, -6.41762638 }, /* Citywest Campus */
            { 53.28424849, -6.42475033 }, /* Fortunestown */
            { 53.28483859, -6.43777514 }, /* Saggart */
    };
    private double[][] stopCoordsGreenLine = new double[][] {
            { 53.37254168, -6.29840233 }, /* Broombridge */
            { 53.36385473, -6.28157952 }, /* Cabra */
            { 53.36009486, -6.27861970 }, /* Phibsborough */
            { 53.35727273, -6.27731346 }, /* Grangegorman */
            { 53.35407420, -6.27392315 }, /* Broadstone - DIT */
            { 53.35124424, -6.26531198 }, /* Dominick */
            { 53.35301900, -6.26047044 }, /* Parnell */
            { 53.35165250, -6.26117601 }, /* O'Connell - Upper */
            { 53.34880980, -6.25992879 }, /* O'Connell - GPO */
            { 53.34915970, -6.25775202 }, /* Marlborough */
            { 53.34623832, -6.25914424 }, /* Westmoreland */
            { 53.34518877, -6.25865324 }, /* Trinity */
            { 53.34209496, -6.25801637 }, /* Dawson */
            { 53.33911033, -6.26139200 }, /* St. Stephen's Green */
            { 53.33364891, -6.26269019 }, /* Harcourt */
            { 53.33060239, -6.25862396 }, /* Charlemont */
            { 53.32613311, -6.25619924 }, /* Ranelagh */
            { 53.32093278, -6.25462210 }, /* Beechwood */
            { 53.31639199, -6.25344193 }, /* Cowper */
            { 53.30967275, -6.25174391 }, /* Milltown */
            { 53.30174559, -6.25064689 }, /* Windy Arbour */
            { 53.29242537, -6.24511617 }, /* Dundrum */
            { 53.28605533, -6.23670495 }, /* Balally */
            { 53.28296371, -6.22410393 }, /* Kilmacud */
            { 53.27934264, -6.21025300 }, /* Stillorgan */
            { 53.27763303, -6.20462036 }, /* Sandyford */
            { 53.27016831, -6.20383715 }, /* Central Park */
            { 53.26626702, -6.20992577 }, /* Glencairn */
            { 53.26114604, -6.20584881 }, /* The Gallops */
            { 53.25829972, -6.19834936 }, /* Leopardstown Valley */
            { 53.25506809, -6.18441796 }, /* Ballyogan Wood */
            { 53.25436204, -6.17160237 }, /* Carrickmines */
            { 53.25063905, -6.15495121 }, /* Laughanstown */
            { 53.24538459, -6.14582634 }, /* Cherrywood */
            { 53.24186949, -6.14277935 }, /* Bride's Glen */
    };
    private double[][] stopCoords = new double[0][0];

    public StopCoords(String line) {
        switch (line) {
            case Constant.RED_LINE:
                stopCoords = stopCoordsRedLine;

                break;

            case Constant.GREEN_LINE:
                stopCoords = stopCoordsGreenLine;

                break;

            default:
                /* If for some reason the line doesn't make sense. */
                Log.wtf(LOG_TAG, "Invalid line specified.");
        }
    }

    public double[][] getStopCoords() {
        return stopCoords;
    }
}
