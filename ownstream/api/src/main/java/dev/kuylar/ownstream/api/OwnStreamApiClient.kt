package dev.kuylar.ownstream.api

import android.util.Log
import dev.kuylar.ownstream.api.models.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

class OwnStreamApiClient(var instanceHost: String, val userAgent: String) {
	private var token: String? = null
	private var locale: String? = null
	private val client = HttpClient {
		install(ContentNegotiation) {
			json()
		}
	}

	private suspend inline fun <reified T> get(url: String): ApiResponse<T> {
		val resp = client
			.get("${instanceHost.trimEnd('/')}/${url.trimStart('/')}?locale=$locale") {
				if (token != null) header("Authorization", "Bearer $token")
				header("User-Agent", userAgent)
			}
		return ApiResponse(
			resp.status.value,
			try {
				resp.body<T>()
			} catch (e: Exception) {
				Log.e(this.javaClass.name, "Failed to deserialize object", e)
				null
			}
		)
	}

	private suspend inline fun <reified T> post(url: String, body: Any?): ApiResponse<T> {
		val resp = client
			.post("${instanceHost.trimEnd('/')}/${url.trimStart('/')}") {
				contentType(ContentType.Application.Json)
				setBody(body)
				if (token != null) header("Authorization", "Bearer $token")
				header("User-Agent", userAgent)
			}
		return ApiResponse(
			resp.status.value,
			try {
				resp.body<T>()
			} catch (e: Exception) {
				Log.e(this.javaClass.name, "Failed to deserialize object", e)
				null
			}
		)
	}

	fun setAuth(token: String) {
		this.token = if (token.startsWith("Bearer ")) token.substring("Bearer ".length) else token
	}

	fun setLocale(locale: String) {
		this.locale = locale
	}

	suspend fun login(username: String, password: String): ApiResponse<LoginResponse> {
		val resp = post<LoginResponse>("/api/auth/login", LoginRequest(username, password));
		resp.response?.accessToken?.let { setAuth(it) }
		return resp
	}

	suspend fun getInfo() = get<InstanceInfo>("/api/info")
	suspend fun whoAmI() = get<UserResponse>("/api/auth/whoami")
	suspend fun getHomeShelves() = get<List<Shelf>>("/api/home/shelves")
	suspend fun getContentDetails(id: String) = get<Content>("/api/content/$id/details")
	suspend fun getSeasons(id: String) = get<List<Season>>("/api/content/$id/seasons")
	suspend fun getEpisode(id: String) = get<Episode>("/api/content/episode/$id")
	suspend fun getNextEpisode(id: String) = get<Episode>("/api/content/episode/$id/next")
	suspend fun getEpisodes(id: String, season: Int) = get<List<Episode>>("/api/content/$id/seasons/$season/episodes")
	suspend fun getVideo(id: String) = get<Video>("/api/video/$id")
	suspend fun getProgress(videoOrEpisodeId: String) = get<WatchProgressResponse>("/api/progress/$videoOrEpisodeId")
	suspend fun updateWatchProgress(videoId: String, videoLength: Int, watchedMilliseconds: Int, markWatched: Boolean? = null) = post<Any>(
		"/api/progress/update",
		UpdateWatchProgressRequest(videoId, videoLength, watchedMilliseconds, markWatched)
	)
	suspend fun updateWatchProgress(videoId: String, markWatched: Boolean) = post<Any>(
		"/api/progress/update",
		UpdateWatchProgressRequest(videoId, null, null, markWatched)
	)
	suspend fun getEpisodeToWatch(contentId: String) = get<EpisodeToWatchResponse>("/api/progress/upNext/$contentId")

	fun getMediaUrl(videoId: String, file: String) =
		"${instanceHost.trimEnd('/')}/Media/$videoId/$file"

	fun getMediaUrl(videoId: String, dir: String, file: String) =
		"${instanceHost.trimEnd('/')}/Media/$videoId/$dir/$file"
}