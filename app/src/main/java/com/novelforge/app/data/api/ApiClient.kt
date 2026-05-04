package com.novelforge.app.data.api

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.novelforge.app.BuildConfig
import com.novelforge.app.data.preference.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {
    
    private var settingsManager: SettingsManager? = null
    private var cachedOkHttpClient: OkHttpClient? = null
    private var cachedRetrofit: Retrofit? = null
    private var cachedBaseUrl: String? = null
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun initialize(context: Context) {
        settingsManager = SettingsManager(context)
    }

    private fun getSettingsManager(): SettingsManager {
        return settingsManager ?: throw IllegalStateException("ApiClient not initialized. Call initialize() first.")
    }

    private fun createOkHttpClient(apiKey: String): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private fun getRetrofit(): Retrofit {
        val baseUrl = getBaseUrl()
        val apiKey = getApiKey()
        
        // 如果缓存的 baseUrl 和 apiKey 没有变化，复用现有实例
        if (cachedRetrofit != null && cachedBaseUrl == baseUrl) {
            return cachedRetrofit!!
        }

        val okHttpClient = createOkHttpClient(apiKey)
        
        cachedOkHttpClient = okHttpClient
        cachedBaseUrl = baseUrl
        cachedRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl + "/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        
        return cachedRetrofit!!
    }

    fun getApiKey(): String {
        return getSettingsManager().getApiKeySync()
    }

    fun getBaseUrl(): String {
        return getSettingsManager().getCurrentEndpointUrlSync()
    }

    fun getModel(): String {
        return getSettingsManager().getCurrentModelSync()
    }
    
    val grokApiService: GrokApiService
        get() = getRetrofit().create(GrokApiService::class.java)
    
    fun getStreamingOkHttpClient(): OkHttpClient {
        val apiKey = getApiKey()
        return cachedOkHttpClient ?: createOkHttpClient(apiKey).also {
            cachedOkHttpClient = it
        }
    }
    
    fun getJson(): Json = json

    // 刷新客户端（当设置变更时调用）
    fun refreshClient() {
        cachedRetrofit = null
        cachedOkHttpClient = null
        cachedBaseUrl = null
    }
}
