package com.veezutech.minipassenger

import androidx.navigation3.runtime.NavKey
import com.veezutech.map.MapRoute
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val navKeySerializersModule: SerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(MapRoute::class)
    }
}