package org.thecosmicfrog.luasataglance.api

import okhttp3.Interceptor
import okhttp3.Response
import org.thecosmicfrog.luasataglance.BuildConfig

class HttpInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val appVersion = BuildConfig.VERSION_NAME

        val request = original.newBuilder()
            .header("user-agent", "LuasAtAGlance/$appVersion")
            .method(original.method, original.body)
            .build()

        return chain.proceed(request)
    }
}
