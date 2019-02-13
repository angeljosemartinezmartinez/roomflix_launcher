package verion.desing.launcher.database.models

import com.google.gson.annotations.SerializedName

data class Data (

        @SerializedName("name")
        var name: String? = "",
        @SerializedName("nativeName")
        var nativeName: String? = "",
        @SerializedName("code")
        var code: String? = "",
        @SerializedName("picture")
        var picture: String? ="",
        @SerializedName("textsApp")
        var  textsApp: TextsApp? = null
)
