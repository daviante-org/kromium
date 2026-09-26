package org.daviante.kromium.jcef.network

import org.daviante.kromium.api.network.KromiumAssets

internal class JcefAssets : KromiumAssets {
    @Volatile override var filter: org.daviante.kromium.api.network.KromiumAssetFilter? = null
}
