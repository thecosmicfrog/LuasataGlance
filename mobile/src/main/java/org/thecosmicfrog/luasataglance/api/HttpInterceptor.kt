package org.thecosmicfrog.luasataglance.api

import org.thecosmicfrog.luasataglance.BuildConfig
import retrofit.RequestInterceptor

class HttpInterceptor : RequestInterceptor {

    override fun intercept(request: RequestInterceptor.RequestFacade) {
        val appVersion = BuildConfig.VERSION_NAME
        request.addHeader("user-agent", "LuasAtAGlance/$appVersion")
    }
}
