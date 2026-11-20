package dev.kuylar.ownstream.api.store

interface AccountStore {
	suspend fun getServers(): List<OwnStreamServer>
	suspend fun getServer(id: String): OwnStreamServer?
	suspend fun addServer(server: OwnStreamServer)
	suspend fun removeServer(server: OwnStreamServer)
	suspend fun getAccounts(serverId: String): List<OwnStreamAccount>
	suspend fun getAccount(serverId: String, id: String): OwnStreamAccount?
	suspend fun addAccount(account: OwnStreamAccount)
	suspend fun removeAccount(account: OwnStreamAccount)
}