package com.example.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface GitHubGistApi {

    @GET("gists/{gist_id}")
    suspend fun getGist(
        @Path("gist_id") gistId: String,
        @Header("Authorization") authHeader: String,
        @Header("Accept") accept: String = "application/vnd.github.v3+json"
    ): Response<GistResponse>

    @PATCH("gists/{gist_id}")
    suspend fun updateGist(
        @Path("gist_id") gistId: String,
        @Header("Authorization") authHeader: String,
        @Body request: GistUpdateRequest,
        @Header("Accept") accept: String = "application/vnd.github.v3+json"
    ): Response<GistResponse>

    companion object {
        private const val BASE_URL = "https://api.github.com/"

        fun create(): GitHubGistApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val moshi = com.squareup.moshi.Moshi.Builder()
                .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GitHubGistApi::class.java)
        }
    }
}
