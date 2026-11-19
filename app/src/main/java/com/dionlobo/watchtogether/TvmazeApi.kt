package com.dionlobo.watchtogether

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.LinkedHashMap

data class TvImage(val medium: String? = null, val original: String? = null)

data class TvShow(
    val id: Int = 0,
    val name: String = "",
    val image: TvImage? = null,
    val summary: String? = null
)

data class TvSearchResult(val show: TvShow = TvShow())

data class TvEpisodeDto(
    val id: Int = 0,
    val name: String? = null,
    val season: Int? = null,
    val number: Int? = null,
    val airdate: String? = null,
    val image: TvImage? = null
)

interface TvmazeService {
    @GET("search/shows")
    suspend fun search(@Query("q") query: String): List<TvSearchResult>

    @GET("shows/{id}/episodes")
    suspend fun episodes(@Path("id") showId: Int): List<TvEpisodeDto>
}

object TvmazeApi {
    private val service: TvmazeService = Retrofit.Builder()
        .baseUrl("https://api.tvmaze.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TvmazeService::class.java)

    private val searchCache = LinkedHashMap<String, List<TvShow>>()
    private val episodeCache = LinkedHashMap<Int, List<TvEpisode>>()

    private fun <K, V> cachePut(cache: LinkedHashMap<K, V>, key: K, value: V, max: Int) {
        synchronized(cache) {
            while (cache.size >= max) cache.remove(cache.keys.first())
            cache[key] = value
        }
    }

    fun cachedSearch(query: String): List<TvShow>? = synchronized(searchCache) {
        searchCache[query.trim().lowercase()]
    }

    suspend fun search(query: String): List<TvShow> = withContext(Dispatchers.IO) {
        val key = query.trim().lowercase()
        synchronized(searchCache) { searchCache[key] }?.let { return@withContext it }
        val result = service.search(query).map { it.show }.filter { it.id != 0 }
        cachePut(searchCache, key, result, 40)
        result
    }

    fun cachedEpisodes(showId: Int): List<TvEpisode>? = synchronized(episodeCache) {
        episodeCache[showId]
    }

    suspend fun episodes(showId: Int): List<TvEpisode> = withContext(Dispatchers.IO) {
        synchronized(episodeCache) { episodeCache[showId] }?.let { return@withContext it }
        val result = service.episodes(showId).map {
            TvEpisode(
                id = it.id,
                name = it.name ?: "Episode ${it.number ?: 0}",
                season = it.season ?: 0,
                number = it.number ?: 0,
                airdate = it.airdate ?: "",
                imageUrl = it.image?.medium ?: ""
            )
        }.sortedWith(compareBy({ it.season }, { it.number }))
        cachePut(episodeCache, showId, result, 24)
        result
    }
}
