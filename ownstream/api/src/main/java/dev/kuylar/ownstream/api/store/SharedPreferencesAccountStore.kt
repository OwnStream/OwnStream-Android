package dev.kuylar.ownstream.api.store

import android.content.Context
import androidx.core.content.edit

/**
 * Account store backed by SharedPreferences. Only for testing, replace it with Room based one ASAP
 */
class SharedPreferencesAccountStore(context: Context) : AccountStore {
	private val sp = context.getSharedPreferences("accountStore", Context.MODE_PRIVATE)
	override suspend fun getServers(): List<OwnStreamServer> {
		return sp.getStringSet("servers", emptySet())?.map {
			it.split("<|>", limit = 3)
		}?.filter { it.size == 3 }?.map {
			OwnStreamServer(it[0], it[2], it[1])
		} ?: emptyList()
	}

	override suspend fun getServer(id: String): OwnStreamServer? {
		return getServers().firstOrNull { it.id == id }
	}

	override suspend fun addServer(server: OwnStreamServer) {
		sp.edit {
			putStringSet("servers", getServers().toMutableList().apply {
				add(server)
			}.map { "${it.id}<|>${it.name}<|>${it.url}" }.toSet())
		}
	}

	override suspend fun removeServer(server: OwnStreamServer) {
		sp.edit {
			putStringSet("servers", getServers().toMutableList().apply {
				removeAll { it == server }
			}.map { "${it.id}<|>${it.name}<|>${it.url}" }.toSet())
		}
	}

	override suspend fun getAccounts(serverId: String): List<OwnStreamAccount> {
		return sp.getStringSet("accounts.$serverId", emptySet())?.map {
			it.split("<|>", limit = 5)
		}?.filter { it.size == 5 }?.map {
			OwnStreamAccount(it[0], it[1], it[2], it[3], it[4].split(","))
		} ?: emptyList()
	}

	override suspend fun getAccount(
		serverId: String,
		id: String
	): OwnStreamAccount? {
		return getAccounts(serverId).firstOrNull { it.id == id }
	}

	override suspend fun addAccount(account: OwnStreamAccount) {
		sp.edit {
			putStringSet(
				"accounts.${account.serverId}",
				getAccounts(account.serverId).toMutableList().apply {
					add(account)
				}.map {
					it.id + "<|>" + it.serverId + "<|>" + it.username + "<|>" + it.token + "<|>" +
							it.permissions.joinToString(",")
				}.toSet()
			)
		}
	}

	override suspend fun removeAccount(account: OwnStreamAccount) {
		sp.edit {
			putStringSet(
				"accounts.${account.serverId}",
				getAccounts(account.serverId).toMutableList().apply {
					removeAll { it == account }
				}.map {
					it.id + "<|>" + it.serverId + "<|>" + it.username + "<|>" + it.token + "<|>" +
							it.permissions.joinToString(",")
				}.toSet()
			)
		}
	}
}