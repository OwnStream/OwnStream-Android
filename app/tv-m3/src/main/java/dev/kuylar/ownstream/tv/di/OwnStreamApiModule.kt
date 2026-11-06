package dev.kuylar.ownstream.tv.di

import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.kuylar.ownstream.tv.BuildConfig
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.store.SharedPreferencesAccountStore

@Module
@InstallIn(SingletonComponent::class)
object OwnStreamApiModule {
	private lateinit var initializedClient: OwnStreamApiClient
	@Provides
	fun provideOwnStreamApiClient(
		@ApplicationContext context: Context
	): OwnStreamApiClient {
		// for some reason singletons dont wanna work here
		if (!this::initializedClient.isInitialized) {
			Log.i("OwnStreamApiClient_di", "Creating new OSAC")
			val store = SharedPreferencesAccountStore(context)
			val client = OwnStreamApiClient(
				store,
				"OwnStream-Android/${BuildConfig.VERSION_NAME} (Tv-M3)"
			)
			client.setLocale("en_US")
			initializedClient = client
		}
		return initializedClient
	}
}
