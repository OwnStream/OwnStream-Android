package dev.kuylar.ownstream.tv.content

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import android.util.Log
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.store.SharedPreferencesAccountStore
import dev.kuylar.ownstream.tv.BuildConfig
import kotlinx.coroutines.runBlocking

class SearchProvider : ContentProvider() {
	private lateinit var client: OwnStreamApiClient

	override fun onCreate(): Boolean {
		Log.i("SearchProvider", "onCreate")
		client = OwnStreamApiClient(
			SharedPreferencesAccountStore(context!!),
			"OwnStream-Android/${BuildConfig.VERSION_NAME} (Tv-M3-Search)"
		)
		// TODO: Save & load a preferred server & account
		return runBlocking {
			val server = client.store.getServers().firstOrNull()
			if (server == null) {
				Log.w("SearchProvider", "onCreate: no server configured")
				return@runBlocking false
			}
			val account = client.store.getAccounts(server.id).firstOrNull()
			if (account == null) {
				Log.w("SearchProvider", "onCreate: no account configured for ${server.id}")
				return@runBlocking false
			}
			client.selectServer(server.id)
			client.selectAccount(account.id)
			Log.i("SearchProvider", "onCreate: using server ${server.id}, account ${account.id}")
			return@runBlocking true
		}
	}

	override fun query(
		uri: Uri,
		projection: Array<out String?>?,
		selection: String?,
		selectionArgs: Array<out String?>?,
		sortOrder: String?
	): Cursor? {
		val q = selectionArgs?.joinToString("\n")?.takeUnless { it.startsWith("test") } ?: "akira" ?: return null
		Log.i("SearchProvider", "query: $q")
		if (q.isBlank()) return null
		return runBlocking {
			val results = client.search(q, type = "content")
			val cursor = MatrixCursor(
				arrayOf(
					BaseColumns._ID,
					SearchManager.SUGGEST_COLUMN_TEXT_1,
					SearchManager.SUGGEST_COLUMN_TEXT_2,
					SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE,
					SearchManager.SUGGEST_COLUMN_IS_LIVE,
					SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
					SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR,
					SearchManager.SUGGEST_COLUMN_DURATION,
				)
			)
			results.response?.items?.mapNotNull {
				client.getContentDetails(it.contentId ?: it.id).response
			}?.forEachIndexed { i, it ->
				// TODO: Return more data from the server
				val values = arrayOf<Any>(
					i,
					it.translatedTitle ?: it.originalTitle,
					it.translatedDescription ?: it.originalDescription,
					it.images.poster ?: "",
					0,
					"${it.type}/${it.id}",
					it.releasedAt.take(4).toInt(),
					it.runtime.split(":").let { parts ->
						val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
						val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
						val s = parts.getOrNull(2)?.toIntOrNull() ?: 0
						((h * 3600L) + (m * 60L) + s) * 1000L
					}
				)
				Log.i("SearchProvider", "result: ${values.joinToString(" | ")}")
				cursor.addRow(values)
			}
			cursor
		}
	}

	override fun getType(uri: Uri): String? {
		Log.i("SearchProvider", "getType: $uri")
		TODO("Not yet implemented")
	}

	override fun insert(uri: Uri, values: ContentValues?): Uri? = null
	override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String?>?): Int = 0
	override fun update(
		uri: Uri,
		values: ContentValues?,
		selection: String?,
		selectionArgs: Array<out String?>?
	) = 0
}