@file:Suppress("unused")

package dev.kuylar.ownstream.api

import android.os.Build
import android.util.Log
import dev.kuylar.ownstream.api.models.*
import dev.kuylar.ownstream.api.store.AccountStore
import dev.kuylar.ownstream.api.store.OwnStreamAccount
import dev.kuylar.ownstream.api.store.OwnStreamServer
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
import io.ktor.utils.io.charsets.Charset
import kotlinx.serialization.json.JsonObject
import java.net.URLEncoder

class OwnStreamApiClient(val store: AccountStore, val userAgent: String) {
	var serverId: String? = null
		private set
	var accountId: String? = null
		private set
	private var token: String? = null
	private var locale: String? = null
	private val client = HttpClient {
		install(ContentNegotiation) {
			json()
		}
	}
	private var instanceHost: String = ""

	private suspend inline fun <reified T> get(
		url: String,
		query: Map<String, String?>? = null,
		tokenOverride: String? = null
	): ApiResponse<T> {
		var finalUrl = instanceHost.trimEnd('/') + '/' + url.trimStart('/')
		val finalQuery = (query ?: emptyMap()).toMutableMap()
		locale?.let { finalQuery["locale"] = it }

		finalUrl += '?' + finalQuery.entries.filter { it.value != null }.joinToString("&") {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				"${it.key}=${URLEncoder.encode(it.value, Charsets.UTF_8)}"
			} else {
				@Suppress("DEPRECATION")
				"${it.key}=${URLEncoder.encode(it.value)}"
			}
		}
		Log.d(TAG + "_net", "GET $finalUrl")
		val resp = client.get(finalUrl) {
			if ((tokenOverride ?: token )!= null)
				header("Authorization", "Bearer ${tokenOverride ?: token}")
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

	private suspend inline fun <reified T> post(url: String, query: Map<String, String?>? = null, body: Any? = null): ApiResponse<T> {
		var finalUrl = instanceHost.trimEnd('/') + '/' + url.trimStart('/')
		val finalQuery = (query ?: emptyMap()).toMutableMap()
		locale?.let { finalQuery["locale"] = it }

		finalUrl += '?' + finalQuery.entries.filter { it.value != null }.joinToString("&") {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				"${it.key}=${URLEncoder.encode(it.value, Charsets.UTF_8)}"
			} else {
				@Suppress("DEPRECATION")
				"${it.key}=${URLEncoder.encode(it.value)}"
			}
		}
		Log.d(TAG + "_net", "POST $finalUrl")
		val resp = client.post(finalUrl) {
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

	private suspend inline fun <reified T> delete(url: String, query: Map<String, String?>? = null, body: Any? = null): ApiResponse<T> {
		var finalUrl = instanceHost.trimEnd('/') + '/' + url.trimStart('/')
		val finalQuery = (query ?: emptyMap()).toMutableMap()
		locale?.let { finalQuery["locale"] = it }

		finalUrl += '?' + finalQuery.entries.filter { it.value != null }.joinToString("&") {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				"${it.key}=${URLEncoder.encode(it.value, Charsets.UTF_8)}"
			} else {
				@Suppress("DEPRECATION")
				"${it.key}=${URLEncoder.encode(it.value)}"
			}
		}
		val resp = client.post(finalUrl) {
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

	private suspend fun getServer(): OwnStreamServer {
		val servers = store.getServers()
		if (servers.isEmpty()) throw IllegalStateException("API call attempted without any servers set")
		if (servers.size == 1) return servers[0]
		return servers.firstOrNull { it.id == serverId } ?: servers[0]
	}

	suspend fun selectServer(serverId: String): Boolean {
		val selected = store.getServers().firstOrNull { it.id == serverId }
		if (selected == null) {
			Log.w(TAG, "Failed to select server with ID: $serverId")
			return false
		}
		this.serverId = selected.id
		this.instanceHost = selected.url
		Log.i(TAG, "Server selected: ${selected.id}\nthis.serverId: ${this.serverId}\nthis.instanceHost: ${this.instanceHost}")
		return true
	}

	private suspend fun getServerAccounts(): List<OwnStreamAccount> {
		val server = try {
			getServer()
		} catch (e: IllegalStateException) {
			return emptyList()
		}
		return store.getAccounts(server.id)
	}

	private suspend fun getAccount(): OwnStreamAccount {
		val accounts = store.getAccounts(getServer().id)
		if (accounts.size == 1) return accounts[0]
		return accounts.firstOrNull { it.id == accountId } ?: accounts[0]
	}

	suspend fun selectAccount(accountId: String): Boolean {
		val selected = store.getAccounts(getServer().id).firstOrNull { it.id == accountId } ?: return false
		this.accountId = selected.id
		setAuth(selected.token)
		return true
	}

	private suspend fun setAuth(token: String) {
		this.token = if (token.startsWith("Bearer ")) token.substring("Bearer ".length) else token
	}

	fun setLocale(locale: String) {
		this.locale = locale
	}

	suspend fun login(username: String, password: String): ApiResponse<LoginResponse> {
		val resp = post<LoginResponse>("/api/auth/login", body = LoginRequest(username, password));
		return resp
	}

	suspend fun getInfo() = get<InstanceInfo>("/api/info")
	suspend fun whoAmI(token: String? = null) = get<UserResponse>("/api/auth/whoami", tokenOverride = token)
	suspend fun getHomeShelves() = get<List<Shelf>>("/api/home/shelves")
	suspend fun getHomeShelf(id: String) = get<List<ShelfItem>>("/api/home/$id")
	suspend fun getLibraries() = get<List<Library>>("/api/content/library")
	suspend fun getLibraryItems(
		libraryId: String?,
		typeFilter: String?,
		page: Int = 0,
		limit: Int = 40
	) = get<PagedResponse<Content>>(
		"/api/content/library/${libraryId ?: "00000000-0000-0000-0000-000000000000"}", mapOf(
			"typeFilter" to typeFilter,
			"page" to page.toString(),
			"limit" to limit.toString()
		)
	)
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
		body = UpdateWatchProgressRequest(videoId, videoLength, watchedMilliseconds, markWatched)
	)
	suspend fun updateWatchProgress(videoId: String, markWatched: Boolean) = post<Any>(
		"/api/progress/update",
		body = UpdateWatchProgressRequest(videoId, null, null, markWatched)
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
		"/api/manage/libraries/new", body = mapOf(
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
		"/api/manage/inputLibraries/new", body = mapOf(
			"name" to name,
			"path" to path,
			"type" to type,
			"transcodeLibraryId" to transcodeLibraryId
		)
	)
	suspend fun getJobs(delta: Long = 0, page: Int = 0, limit: Int = 20) = get<PagedResponse<Job>>(
		"/api/jobs/list", mapOf(
			"delta" to delta.toString(),
			"page" to page.toString(),
			"limit" to limit.toString()
		)
	)
	suspend fun requeueJob(id: String) = get<SuccessResponse>("/api/jobs/$id/requeue")
	suspend fun stopJob(id: String) = get<SuccessResponse>("/api/jobs/$id/stop")
	suspend fun getAllUsers() = get<List<User>>("/api/manage/users/list")
	suspend fun getUser(id: String) = get<User>("/api/manage/users/$id")
	suspend fun deleteUser(id: String) = delete<JsonObject>("/api/manage/users/$id")
	suspend fun editUser(
		id: String,
		username: String? = null,
		password: String? = null,
		permissions: List<String>? = null
	) = post<User>(
		"/api/manage/users/$id", body = mapOf(
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
		"/api/manage/users/$id/new", body = mapOf(
			"username" to username,
			"password" to password,
		)
	)
	suspend fun createInitialUser(
		id: String,
		username: String? = null,
		password: String? = null,
	) = post<User>(
		"/api/manage/users/$id/setupNew", body = mapOf(
			"username" to username,
			"password" to password,
		)
	)
	suspend fun quickLoginStart(deviceName: String) = get<QuickLoginStartResponse>(
		"/api/auth/remote/start", mapOf(
			"deviceName" to deviceName
		)
	)
	suspend fun quickLoginCheck(token: String) = get<QuickLoginCheckResponse>(
		"/api/auth/remote/check", mapOf(
			"token" to token
		)
	)
	suspend fun quickLoginAuthorize(
		code: String,
		deviceNameHash: String? = null,
		asUser: String? = null
	) = get<QuickLoginAuthorizeResponse>(
		"/api/auth/remote/authorize", mapOf(
			"code" to code,
			"deviceNameHash" to deviceNameHash,
			"asUser" to asUser
		)
	)
	suspend fun quickLoginSessions() = get<List<QuickLoginSession>>("/api/auth/remote/sessions")
	suspend fun search(query: String, type: String = "all", offset: Int = 0, limit: Int = 20) =
		get<PagedResponse<SearchResult>>(
			"/api/search", mapOf(
				"q" to query,
				"type" to type,
				"offset" to offset.toString(),
				"limit" to limit.toString()
			)
		)
	suspend fun getSettings() = get<JsonObject>("/api/settings/get")
	suspend fun updateSettings(config: JsonObject) = post<JsonObject>("/api/settings/update", body = config)
	// TODO: /api/settings/benchmark

	suspend fun getContentMetadata(id: String) = get<ContentMetadata>("/api/metadata/content/$id")
	suspend fun refreshContentMetadata(id: String, apply: Boolean) = post<ContentMetadata>(
		"/api/metadata/content/$id/refresh",
		query = mapOf("apply" to apply.toString())
	)
	suspend fun setContentMetadata(metadata: ContentMetadata) =
		patch<SuccessResponse.WithData<ContentMetadata>>(
			"/api/metadata/content",
			body = metadata
		)
	suspend fun getContentSeasons(id: String) =
		get<List<Season>>("/api/metadata/content/$id/seasons")
	suspend fun getSeasonMetadata(id: String, index: Int) =
		get<SeasonMetadata>("/api/metadata/content/$id/seasons/$index")
	suspend fun refreshSeasonMetadata(id: String, index: Int, apply: Boolean) =
		post<SeasonMetadata>(
			"/api/metadata/content/$id/seasons/$index",
			query = mapOf("apply" to apply.toString())
		)
	suspend fun setSeasonMetadata(metadata: SeasonMetadata) =
		patch<SuccessResponse.WithData<SeasonMetadata>>(
			"/api/metadata/content/${metadata.contentId}/seasons/${metadata.index}",
			body = metadata
		)
	suspend fun getEpisodeMetadata(id: String) =
		get<EpisodeMetadata>("/api/metadata/episode/$id")
	suspend fun refreshEpisodeMetadata(id: String, apply: Boolean) =
		post<EpisodeMetadata>(
			"/api/metadata/episode/$id",
			query = mapOf("apply" to apply.toString())
		)
	suspend fun setEpisodeMetadata(metadata: EpisodeMetadata) =
		patch<SuccessResponse.WithData<EpisodeMetadata>>(
			"/api/metadata/episode/${metadata.id}",
			body = metadata
		)
	suspend fun searchProviders(providerId: String, contentId: String) =
		get<SuccessResponse.WithData<List<PluginContentSearchResult>>>(
			"/api/metadata/search/$providerId", mapOf("contentId" to contentId)
		)

	suspend fun listPlugins() = get<List<PluginItem>>("/api/plugins/list")
	suspend fun enablePlugin(path: String) = post<SuccessResponse>("/api/plugins/enable", mapOf("path" to path))
	suspend fun disablePlugin(path: String) = post<SuccessResponse>("/api/plugins/disable", mapOf("path" to path))
	// TODO: /api/plugins/install
	suspend fun deletePlugin(path: String) = delete<SuccessResponse>("/api/plugins/enable", mapOf("path" to path))
	suspend fun reloadPlugin(path: String) = post<SuccessResponse>("/api/plugins/reload", mapOf("path" to path))

	fun getMediaUrl(
		videoId: String,
		dir: String?,
		file: String,
		query: Map<String, String>? = null
	): String {
		var url = "${instanceHost.trimEnd('/')}/Media/$videoId/"
		if (dir != null) url += "$dir/"
		url += file
		if (query?.isNotEmpty() == true) {
			url += "?" + query.entries.joinToString("&") { "${ue(it.key)}=${ue(it.value)}" }
		}
		return url
	}

	private fun ue(value: String): String {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			URLEncoder.encode(value, Charset.defaultCharset())
		} else {
			@Suppress("DEPRECATION")
			URLEncoder.encode(value)
		}
	}

	companion object {
		private const val TAG = "OwnStreamApiClient"
	}
}