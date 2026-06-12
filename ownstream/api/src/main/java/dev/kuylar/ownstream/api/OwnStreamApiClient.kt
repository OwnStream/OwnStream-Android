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
import kotlinx.serialization.json.JsonObject

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

	private suspend inline fun <reified T> patch(url: String, body: Any?): ApiResponse<T> {
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

	private suspend inline fun <reified T> delete(url: String, body: Any? = null): ApiResponse<T> {
		val resp = client
			.post("${instanceHost.trimEnd('/')}/${url.trimStart('/')}") {
				if (body != null) {
					contentType(ContentType.Application.Json)
					setBody(body)
				}
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
	suspend fun getHomeShelf(id: String) = get<List<ShelfItem>>("/api/home/$id")
	suspend fun getLibraries() = get<List<Library>>("/api/content/library")
	suspend fun getLibraryItems(
		libraryId: String,
		typeFilter: String,
		page: Int = 0,
		limit: Int = 40
	) = get<PagedResponse<Content>>("/api/content/library/$libraryId?typeFilter=$typeFilter&page=$page&limit=$limit")

	suspend fun getContentDetails(id: String) = get<Content>("/api/content/$id/details")
	suspend fun getSeasons(id: String) = get<List<Season>>("/api/content/$id/seasons")
	suspend fun getEpisode(id: String) = get<Episode>("/api/content/episode/$id")
	suspend fun getNextEpisode(id: String) = get<Episode>("/api/content/episode/$id/next")
	suspend fun getEpisodes(id: String, season: Int) = get<List<Episode>>("/api/content/$id/seasons/$season/episodes")
	suspend fun getVideo(id: String) = get<Video>("/api/video/$id")
	suspend fun getProgress(videoOrEpisodeId: String) = get<WatchProgressResponse>("/api/progress/$videoOrEpisodeId")
	suspend fun updateWatchProgress(
		videoId: String,
		videoLength: Int,
		watchedMilliseconds: Int,
		markWatched: Boolean? = null
	) = post<Any>(
		"/api/progress/update",
		UpdateWatchProgressRequest(videoId, videoLength, watchedMilliseconds, markWatched)
	)
	suspend fun updateWatchProgress(videoId: String, markWatched: Boolean) = post<Any>(
		"/api/progress/update",
		UpdateWatchProgressRequest(videoId, null, null, markWatched)
	)
	suspend fun getEpisodeToWatch(contentId: String) = get<EpisodeToWatchResponse>("/api/progress/upNext/$contentId")
	suspend fun getManagementLibraries() = get<List<Library>>("/api/manage/libraries/list")
	suspend fun getLibrary(id: String) = get<Library>("/api/manage/libraries/$id")
	suspend fun deleteLibrary(id: String) = delete<SuccessResponse>("/api/manage/libraries/$id")
	suspend fun editLibrary(
		id: String,
		name: String? = null,
	) = patch<Library>(
		"/api/manage/libraries/$id", mapOf(
			"name" to name,
		)
	)
	suspend fun createLibrary(
		id: String,
		name: String,
		path: String,
	) = post<SuccessResponse.WithData<Library>>(
		"/api/manage/libraries/new", mapOf(
			"name" to name,
			"path" to path,
		)
	)
	suspend fun getInputLibraries() = get<List<InputLibrary>>("/api/manage/inputLibraries/list")
	suspend fun getInputLibrary(id: String) = get<InputLibrary>("/api/manage/inputLibraries/$id")
	suspend fun scanInputLibrary(id: String) = get<SuccessResponse>("/api/manage/inputLibraries/$id/scan")
	suspend fun deleteInputLibrary(id: String) = delete<SuccessResponse>("/api/manage/inputLibraries/$id")
	suspend fun editInputLibrary(
		id: String,
		name: String? = null,
		type: String? = null,
		transcodeLibraryId: String? = null
	) = patch<InputLibrary>(
		"/api/manage/inputLibraries/$id", mapOf(
			"name" to name,
			"type" to type,
			"transcodeLibraryId" to transcodeLibraryId
		)
	)
	suspend fun createInputLibrary(
		id: String,
		name: String,
		path: String,
		type: String,
		transcodeLibraryId: String
	) = post<SuccessResponse.WithData<InputLibrary>>(
		"/api/manage/inputLibraries/new", mapOf(
			"name" to name,
			"path" to path,
			"type" to type,
			"transcodeLibraryId" to transcodeLibraryId
		)
	)
	suspend fun getJobs(delta: Long = 0, page: Int = 0, limit: Int = 20) = get<PagedResponse<Job>>("/api/manage/jobs?delta=$delta&page=$page&limit=$limit")
	suspend fun requeueJob(id: String) = get<SuccessResponse>("/api/manage/jobs/$id/requeue")
	suspend fun stopJob(id: String) = get<SuccessResponse>("/api/manage/jobs/$id/stop")
	suspend fun getAllUsers() = get<List<User>>("/api/manage/users/list")
	suspend fun getUser(id: String) = get<User>("/api/manage/users/$id")
	suspend fun deleteUser(id: String) = delete<JsonObject>("/api/manage/users/$id")
	suspend fun editUser(
		id: String,
		username: String? = null,
		password: String? = null,
		permissions: List<String>? = null
	) = post<User>(
		"/api/manage/users/$id", mapOf(
			"username" to username,
			"password" to password,
			"permissions" to permissions,
		)
	)
	suspend fun createUser(
		id: String,
		username: String? = null,
		password: String? = null,
	) = post<User>(
		"/api/manage/users/$id/new", mapOf(
			"username" to username,
			"password" to password,
		)
	)
	suspend fun createInitialUser(
		id: String,
		username: String? = null,
		password: String? = null,
	) = post<User>(
		"/api/manage/users/$id/setupNew", mapOf(
			"username" to username,
			"password" to password,
		)
	)
	suspend fun quickLoginStart(deviceName: String) = get<QuickLoginStartResponse>("/api/auth/remote/start?deviceName=$deviceName")
	suspend fun quickLoginCheck(token: String) = get<QuickLoginCheckResponse>("/api/auth/remote/check?token=$token")
	suspend fun quickLoginAuthorize(code: String, deviceNameHash: String? = null, asUser: String? = null) = get<QuickLoginAuthorizeResponse>("/api/auth/remote/authorize?code=$code&deviceNameHash=$deviceNameHash&asUser=$asUser")
	suspend fun quickLoginSessions() = get<List<QuickLoginSession>>("/api/auth/remote/sessions")
	suspend fun search(query: String, type: String = "all", offset: Int = 0, limit: Int = 20) = get<SearchResponse>("/api/search?q=$query&type=$type&offset=$offset&limit=$limit")
	suspend fun getSettings() = get<JsonObject>("/api/settings/get")
	suspend fun updateSettings(config: JsonObject) = post<JsonObject>("/api/settings/update", config)
	// TODO: /api/settings/benchmark


	fun getMediaUrl(videoId: String, file: String) =
		"${instanceHost.trimEnd('/')}/Media/$videoId/$file"

	fun getMediaUrl(videoId: String, dir: String, file: String) =
		"${instanceHost.trimEnd('/')}/Media/$videoId/$dir/$file"
}