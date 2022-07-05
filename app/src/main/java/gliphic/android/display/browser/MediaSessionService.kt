/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.browser

import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.media.service.AbstractMediaSessionService

/**
 * See [AbstractMediaSessionService].
 */
class MediaSessionService(browserStore: BrowserStore) : AbstractMediaSessionService() {
    override val store: BrowserStore by lazy { browserStore }
}