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
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import gliphic.android.R
import gliphic.android.display.abstract_views.BaseMainActivity
import gliphic.android.display.main.WorkspaceTab
import gliphic.android.operation.misc.Log
import mozilla.components.browser.engine.gecko.GeckoEngineView
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.feature.contextmenu.ext.DefaultSelectionActionDelegate
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.toolbar.ToolbarFeature
import mozilla.components.ui.icons.R.drawable
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension

/**
 * The web browser activity.
 *
 * Automatically decrypts all (compatible) messages displayed by communicating with the web browser extension.
 */
class BrowserActivity: BaseMainActivity() {
    private var mozillaComponentsInitialised = false

    private lateinit var components: Components
    private lateinit var toolbarFeature: ToolbarFeature
    private lateinit var sessionFeature: SessionFeature
    private lateinit var sessionUseCases: SessionUseCases
//    private lateinit var swipeRefreshFeature: SwipeRefreshFeature

    companion object {
        const val EXTENSION_URI   = "resource://android/assets/extensions/gliphic_auto_decrypt/"
        const val EXTENSION_ID    = "auto-decrypt@gliphic.co.uk"
        const val NATIVE_APP      = "gliphic_auto_decrypt"  // The connection name for a browser extension.
        const val WEB_EXT_LOG_TAG = "AutoDecryptWebExt"

        var COMPONENTS: Components? = null

        fun clearStorage(applicationContext: Context) {
            if (COMPONENTS == null) {
                Components(applicationContext).sessionStorage.clear()
            }
            else {
                COMPONENTS?.sessionManager?.removeAll()
                COMPONENTS?.sessionStorage?.clear()
                COMPONENTS = null
            }
        }
    }

    override fun onNetworkAvailable(isFirstOnNetworkAvailable: Boolean) {}

    override fun noNetworkOnStart() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Handle communication to and from the web browser extension before displaying browser content. */

        // Do not use the mozilla.components.support.webextensions.WebExtensionController since its install() method
        // requires the engine to be initialised before it it initialised in Components.
        val webExtensionController = GeckoRuntime.getDefault(applicationContext).webExtensionController

        // This delegate handles all communication from and to a specific Port object
        val portDelegate = object : WebExtension.PortDelegate {
            override fun onPortMessage(message : Any, port : WebExtension.Port) {
                Log.d(WEB_EXT_LOG_TAG, "Received message from web extension: $message")

                if (message !is JSONObject) {
                    Log.e(
                            WEB_EXT_LOG_TAG,
                            String.format(
                                    "Received message has invalid type. Expected: %s, actual: %s.",
                                    JSONObject::class.qualifiedName,
                                    message.javaClass.toString()
                            )
                    )
                    return
                }

                val decryptedJsonObject = DecryptedJsonObject(message)
                val publishedTexts      = mutableListOf<String>()

                val cipherTextArray = decryptedJsonObject.getJsonArray()
                for (i in 0 until cipherTextArray.length()) {
                    publishedTexts.add(cipherTextArray[i] as String)
                }

                WorkspaceTab.decryptText(
                        { isSuccessful ->
                            if (isSuccessful) { port.postMessage(decryptedJsonObject.toJsonObject()) }
                        },
                        this@BrowserActivity,
                        decryptedJsonObject,
                        publishedTexts
                )
            }
        }

        // This delegate handles requests to open a port coming from the extension
        val messageDelegate = object : WebExtension.MessageDelegate {
            override fun onConnect(port : WebExtension.Port) {
                Log.d(WEB_EXT_LOG_TAG, "Web extension connected to the app.")

                // Registering the delegate will allow us to receive messages sent through this port.
                port.setDelegate(portDelegate)
            }
        }

        webExtensionController
                .ensureBuiltIn(EXTENSION_URI, EXTENSION_ID)
                .accept(
                        { webExtension ->
                            Log.d(WEB_EXT_LOG_TAG, "Web extension is installed.")

                            webExtension?.setMessageDelegate(messageDelegate, NATIVE_APP)

                            initialiseMozillaComponentsAndSetContentView()
                        },
                        { throwable ->
                            Log.e(WEB_EXT_LOG_TAG, "Error installing web extension: ${throwable?.message}")
                        }
                )
    }

    private fun initialiseMozillaComponentsAndSetContentView() {
        if (COMPONENTS == null) {
            COMPONENTS = Components(applicationContext)
        }
        components = COMPONENTS as Components

        setContentView(R.layout.activity_browser)

        val toolbar            = findViewById<BrowserToolbar>(R.id.browser_browserToolbar)
        val engineView         = findViewById<GeckoEngineView>(R.id.browser_engineView)
//        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.browser_swipeRefreshLayout)

        toolbar.display.menuBuilder = getBrowserMenuBuilder()

        sessionUseCases = components.sessionUseCases

        sessionFeature = SessionFeature(
                store = components.store,
                goBackUseCase = components.sessionUseCases.goBack,
                engineView = engineView,
                tabId = null
        )

        toolbarFeature = ToolbarFeature(
                toolbar = toolbar,
                store = components.store,
                loadUrlUseCase = components.sessionUseCases.loadUrl,
                searchUseCase = components.defaultSearchUseCase,
                customTabId = null,
                urlRenderConfiguration = null
        )

//        swipeRefreshFeature = SwipeRefreshFeature(
//                store = components.store,
//                reloadUrlUseCase = components.sessionUseCases.reload,
//                swipeRefreshLayout = swipeRefreshLayout,
//                tabId = null
//        )

        mozillaComponentsInitialised = true
        onStart()
    }

    override fun onStart() {
        super.onStart()

        if (mozillaComponentsInitialised) {
            toolbarFeature.start()
            sessionFeature.start()
//            lifecycle.addObserver(swipeRefreshFeature)
        }
    }

    override fun onStop() {
        super.onStop()

        if (mozillaComponentsInitialised) {
            toolbarFeature.stop()
            sessionFeature.stop()
//            lifecycle.removeObserver(swipeRefreshFeature)
        }
    }

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? =
            when (name) {
                GeckoEngineView::class.java.name -> components.engine.createView(context, attrs).apply {
                    selectionActionDelegate = DefaultSelectionActionDelegate(
                            store = components.store,
                            context = context
                    )
                }.asView()
                else -> super.onCreateView(parent, name, context, attrs)
            }

    private fun getBrowserMenuBuilder(): BrowserMenuBuilder {
        return BrowserMenuBuilder(listOf(getBrowserMenuItemToolbar()))
    }

    private fun getBrowserMenuItemToolbar(): BrowserMenuItemToolbar {
        val back = BrowserMenuItemToolbar.Button(
                drawable.mozac_ic_back,
                iconTintColorResource = R.color.colorPrimary,
                contentDescription = "Back"
        ) {
            sessionUseCases.goBack.invoke()
        }

        val forward = BrowserMenuItemToolbar.Button(
                drawable.mozac_ic_forward,
                iconTintColorResource = R.color.colorPrimary,
                contentDescription = "Forward"
        ) {
            sessionUseCases.goForward.invoke()
        }

        val refresh = BrowserMenuItemToolbar.Button(
                drawable.mozac_ic_refresh,
                iconTintColorResource = R.color.colorPrimary,
                contentDescription = "Refresh"
        ) {
            sessionUseCases.reload.invoke()
        }

        val stop = BrowserMenuItemToolbar.Button(
                drawable.mozac_ic_stop,
                iconTintColorResource = R.color.colorPrimary,
                contentDescription = "Stop"
        ) {
            sessionUseCases.stopLoading.invoke()
        }

        return BrowserMenuItemToolbar(listOf(back, forward, refresh, stop))
    }
}