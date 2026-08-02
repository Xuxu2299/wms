package com.ruoyi.wms.pda.data.api

import com.ruoyi.wms.pda.data.prefs.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 单例。
 *
 * - baseUrl 从 [SessionManager.serverUrl] 读取，必须以 "/" 结尾；
 *   若 SessionManager 中未配置，则回退到默认地址 http://192.168.1.100:8080/
 * - 通过拦截器统一注入 `Authorization: Bearer <token>` 请求头（token 为空时不注入，
 *   以保证 login / captchaImage 等接口可正常调用）
 * - 内置 HttpLoggingInterceptor，输出请求 / 响应日志，便于调试
 * - 当服务器地址变更后，调用 [recreate] 重建 Retrofit，使新地址生效
 */
object ApiClient {

    /** 默认服务器地址（带尾部斜杠，符合 Retrofit baseUrl 规范） */
    private const val DEFAULT_BASE_URL = "http://192.168.1.100:8080/"

    /** 网络超时时间（秒） */
    private const val DEFAULT_TIMEOUT = 30L

    @Volatile
    private var retrofit: Retrofit? = null

    @Volatile
    private var apiCache: WmsApi? = null

    /** 获取 WMS API 接口实例（已缓存，[recreate] 后自动重建） */
    val api: WmsApi
        get() {
            apiCache?.let { return it }
            return synchronized(this) {
                apiCache ?: getRetrofit().create(WmsApi::class.java).also { apiCache = it }
            }
        }

    /**
     * 重建 Retrofit。当 [SessionManager.serverUrl] 变更后调用，使新地址生效。
     */
    fun recreate() {
        synchronized(this) {
            retrofit = null
            apiCache = null
        }
    }

    private fun getRetrofit(): Retrofit {
        retrofit?.let { return it }
        return synchronized(this) {
            retrofit ?: Retrofit.Builder()
                .baseUrl(buildBaseUrl())
                .client(buildClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .also { retrofit = it }
        }
    }

    /** 读取并规范化 baseUrl，保证以 "/" 结尾 */
    private fun buildBaseUrl(): String {
        val url = SessionManager.serverUrl.trim()
        return if (url.isEmpty()) DEFAULT_BASE_URL else ensureTrailingSlash(url)
    }

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"

    /** 构建 OkHttpClient：Bearer Token 拦截器 + 日志拦截器 + 超时设置 */
    private fun buildClient(): OkHttpClient {
        // Bearer Token 鉴权拦截器（每次请求实时读取 token）
        val authInterceptor = Interceptor { chain ->
            val builder = chain.request().newBuilder()
            val token = SessionManager.token
            if (token.isNotEmpty()) {
                builder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        }

        // 日志拦截器
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
