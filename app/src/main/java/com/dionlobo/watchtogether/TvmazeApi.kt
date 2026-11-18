package com.dionlobo.watchtogether

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

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

    suspend fun search(query: String): List<TvShow> =
        service.search(query).map { it.show }.filter { it.id != 0 }

    suspend fun episodes(showId: Int): List<TvEpisode> =
        service.episodes(showId).map {
            TvEpisode(
                id = it.id,
                name = it.name ?: "Episode ${it.number ?: 0}",
                season = it.season ?: 0,
                number = it.number ?: 0,
                airdate = it.airdate ?: "",
                imageUrl = it.image?.medium ?: ""
            )
        }
}
