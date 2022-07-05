/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.browser

import android.content.Context
import gliphic.android.R
import gliphic.android.operation.misc.BuildConfiguration
import java.util.concurrent.TimeUnit
import mozilla.components.browser.engine.gecko.GeckoEngine
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.session.engine.EngineMiddleware
import mozilla.components.browser.session.storage.SessionStorage
import mozilla.components.browser.session.undo.UndoMiddleware
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.browser.thumbnails.ThumbnailsMiddleware
import mozilla.components.browser.thumbnails.storage.ThumbnailStorage
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.Settings
import mozilla.components.concept.engine.mediaquery.PreferredColorScheme
import mozilla.components.concept.fetch.Client
import mozilla.components.feature.downloads.DownloadMiddleware
import mozilla.components.feature.media.MediaSessionFeature
import mozilla.components.feature.media.middleware.RecordingDevicesMiddleware
import mozilla.components.feature.readerview.ReaderViewMiddleware
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.search.ext.toDefaultSearchEngineProvider
import mozilla.components.feature.search.middleware.SearchMiddleware
import mozilla.components.feature.search.region.RegionMiddleware
import mozilla.components.feature.session.HistoryDelegate
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.sitepermissions.SitePermissionsStorage
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.webnotifications.WebNotificationFeature
import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
import mozilla.components.service.location.LocationService
import org.mozilla.thirdparty.com.google.android.exoplayer2.offline.DownloadService

class Components(private val applicationContext: Context) {
    companion object {
//        const val BROWSER_PREFERENCES = "browser_preferences"
//        const val PREF_LAUNCH_EXTERNAL_APP = "browser_launch_external_app"

        // The annotation prevents unnecessary generation of object and getter to access the value.
        @JvmField val INITIAL_SESSION_URL = getDefaultUrl()

        private fun getDefaultUrl(): String {
            return if (BuildConfiguration.isDebugBuild()) {
                "https://www.mozilla.org"
            }
            else {
                "https://www.google.com"    // "about:blank"
            }
        }
    }

    val engine: Engine by lazy { GeckoEngine(context = applicationContext, defaultSettings = engineSettings) }

    private val engineSettings: Settings by lazy {
        DefaultSettings().apply {
            trackingProtectionPolicy = EngineSession.TrackingProtectionPolicy.recommended()
            historyTrackingDelegate = HistoryDelegate(lazyHistoryStorage)
            preferredColorScheme = PreferredColorScheme.Dark
        }
    }

    val store: BrowserStore by lazy {
        BrowserStore(middleware = listOf(
                DownloadMiddleware(applicationContext, DownloadService::class.java),
                ReaderViewMiddleware(),
                ThumbnailsMiddleware(thumbnailStorage),
                UndoMiddleware(::sessionManagerLookup),
                RegionMiddleware(applicationContext, LocationService.default()),
                SearchMiddleware(applicationContext),
                RecordingDevicesMiddleware(applicationContext)
        ) + EngineMiddleware.create(engine, ::findSessionById))
    }

    private fun sessionManagerLookup(): SessionManager {
        return sessionManager
    }

    private fun findSessionById(tabId: String): Session? {
        return sessionManager.findSessionById(tabId)
    }

    val sessionUseCases: SessionUseCases by lazy { SessionUseCases(store, sessionManager) }

    val sessionManager: SessionManager by lazy {
        SessionManager(engine, store).apply {
            sessionStorage.restore()?.let {
                snapshot -> restore(snapshot)
            }

            if (size == 0) {
                // Add a default session.
                add(Session(initialUrl = INITIAL_SESSION_URL))
            }

            sessionStorage.autoSave(store)
                    .periodicallyInForeground(interval = 30, unit = TimeUnit.SECONDS)
                    .whenGoingToBackground()
                    .whenSessionsChange()

            icons.install(engine, store)

            WebNotificationFeature(
                    applicationContext,
                    engine,
                    icons,
                    R.drawable.ic_notification,
                    permissionStorage,
                    BrowserActivity::class.java
            )

            MediaSessionFeature(applicationContext, MediaSessionService::class.java, store).start()
        }
    }

    val sessionStorage: SessionStorage by lazy { SessionStorage(applicationContext, engine) }

    private val lazyHistoryStorage = lazy { PlacesHistoryStorage(applicationContext) }
//    val historyStorage by lazy { lazyHistoryStorage.value }

    private val permissionStorage: SitePermissionsStorage by lazy { SitePermissionsStorage(applicationContext) }

    private val thumbnailStorage: ThumbnailStorage by lazy { ThumbnailStorage(applicationContext) }

    private val client: Client by lazy { HttpURLConnectionClient() }

    private val icons: BrowserIcons by lazy { BrowserIcons(applicationContext, client) }

    private val tabsUseCases: TabsUseCases by lazy { TabsUseCases(store, sessionManager) }

    private val searchUseCases: SearchUseCases by lazy {
        SearchUseCases(store, store.toDefaultSearchEngineProvider(), tabsUseCases)
    }

    val defaultSearchUseCase by lazy {
        { searchTerms: String ->
            searchUseCases.defaultSearch.invoke(
                    searchTerms = searchTerms,
                    searchEngine = null,
                    parentSessionId = null
            )
        }
    }

//    private val preferences: SharedPreferences = applicationContext.getSharedPreferences(
//            BROWSER_PREFERENCES,
//            Context.MODE_PRIVATE
//    )
//
//    private val webAppManifestStorage: ManifestStorage by lazy { ManifestStorage(applicationContext) }
//
//    val webAppInterceptor: WebAppInterceptor by lazy { WebAppInterceptor(applicationContext, webAppManifestStorage) }
//
//    val appLinksInterceptor: AppLinksInterceptor by lazy {
//        AppLinksInterceptor(
//                applicationContext,
//                interceptLinkClicks = true,
//                launchInApp = { preferences.getBoolean(PREF_LAUNCH_EXTERNAL_APP, false) },
//                launchFromInterceptor = true
//        )
//    }
}