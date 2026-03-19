package com.roomflix.tv.database.models

import java.io.Serializable

data class Child (
        var id: Int,
        var translations: ArrayList<Translation>
): Serializable