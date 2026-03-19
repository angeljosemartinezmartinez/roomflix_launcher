package com.roomflix.tv.database.models

import java.io.Serializable

data class Translation(
        var language:String? = "",
        var picture:String=  ""
):Serializable