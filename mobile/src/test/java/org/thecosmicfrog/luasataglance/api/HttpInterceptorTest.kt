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
package org.thecosmicfrog.luasataglance.api

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.thecosmicfrog.luasataglance.BuildConfig

/**
 * Tests that [HttpInterceptor] inserts the app's user agent on every request.
 */
class HttpInterceptorTest {

    @Test
    fun `the user agent names the app and its version`() {
        val sent = intercept(request(URL))

        assertThat(sent.header("user-agent")).isEqualTo("LuasAtAGlance/${BuildConfig.VERSION_NAME}")
    }

    @Test
    fun `a user agent already on the request is replaced rather than added to`() {
        /* header() overwrites where addHeader() would append, which would send two user agents. */
        val sent = intercept(request(URL).newBuilder().header("user-agent", "SomethingElse/1.0").build())

        assertThat(sent.headers("user-agent")).hasSize(1)
        assertThat(sent.header("user-agent")).isEqualTo("LuasAtAGlance/${BuildConfig.VERSION_NAME}")
    }

    @Test
    fun `the URL and method are left alone`() {
        val sent = intercept(request("$URL?action=times&ver=3&station=TAL"))

        assertThat(sent.url.toString()).isEqualTo("$URL?action=times&ver=3&station=TAL")
        assertThat(sent.method).isEqualTo("GET")
    }

    @Test
    fun `a request body is carried through`() {
        val sent = intercept(request(URL).newBuilder().post("body".toRequestBody()).build())

        assertThat(sent.method).isEqualTo("POST")
        assertThat(sent.body).isNotNull()
    }

    private fun request(url: String) = Request.Builder().url(url).build()

    /**
     * Runs one request through the interceptor and hands back the request it went on to send.
     */
    private fun intercept(request: Request): Request {
        lateinit var sent: Request

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpInterceptor())
            .addInterceptor { chain ->
                sent = chain.request()

                Response.Builder()
                    .request(sent)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("".toResponseBody(null))
                    .build()
            }
            .build()

        client.newCall(request).execute().close()

        return sent
    }

    companion object {
        private const val URL = "https://api.thecosmicfrog.org/cgi-bin/luas-api.php"
    }
}
