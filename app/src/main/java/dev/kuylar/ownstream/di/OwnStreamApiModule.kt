package dev.kuylar.ownstream.di

import android.content.Context
import android.util.Log
import androidx.core.os.ConfigurationCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.kuylar.ownstream.BuildConfig
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.store.SharedPreferencesAccountStore
import java.util.Locale

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
			val store = SharedPreferencesAccountStore(context)
			val client = OwnStreamApiClient(
				store,
				"OwnStream-Android/${BuildConfig.VERSION_NAME} (Android)"
			)
			val locales = ConfigurationCompat.getLocales(context.resources.configuration)
			val locale = locales.get(0) ?: Locale.getDefault()
			val localeString = "${locale.language}_${locale.country}"
			client.setLocale(localeString)
			initializedClient = client
			Log.i("OwnStreamApiClient_di", "Created new OSAC with locale $localeString")
		}
		return initializedClient
	}
}
