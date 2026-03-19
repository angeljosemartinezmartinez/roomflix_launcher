package com.roomflix.tv;

public class Constants {

    public class SHARED_PREFERENCES {
        public static final String MAC = "SPMac";
        public static final String URL_BACK = "urlBack";
        public static final String URL_BACK_LANG = "urlBacklANG";
        public static final String LANGUAGE_ID = "IDIOMASHARED";
        /** Idioma elegido por el usuario (no debe ser sobreescrito por sync) */
        public static final String SELECTED_LANGUAGE_CODE = "selectedLanguageCode";
        public static final String HASH_ALL_INFO = "hashallinfo";
        public static final String HASH_UPDATE = "hashupdate";
        public static final String UPDATE_OBJECT = "updateObject";
        public static final String TIMEZONE = "timezone";
        public static final String APK_DATE = "apkDate";
        public static final String URL_LANG = "urlLang";
        public static final String LANG_DEFAULT = "langDefault";
        public static final String BASE_URL = "baseUrl";
        public static final String ID_SUBMENU = "idSubmenu";
        public static final String INFOCARD_INDEX = "infocardIndex";
        public static final String LOGO = "logo";
        public static final String MINI_LOGO = "miniLogo";
        public static final String DEFAULT_CHANNEL = "defaultChannel";
        public static final String ID_MOREAPPS = "idMoreApps";
        public static final String SSID = "ssid";
        public static final String PASS = "pass";
        public static final String ADB_SERVER = "adbServer";  // Legacy
        public static final String CONTROL_API_URL = "controlApiUrl";
        public static final String CONTROL_API_TOKEN = "controlApiToken";
        public static final String CONTROL_DEVICE_ID = "controlDeviceId";
        public static final String CACHED_ALL_INFO = "cachedAllInfo";
        public static final String IS_USING_CACHE = "isUsingCache";
    }

    public class ENVIRONMENT {
        public static final String DEVELOP = "develop";
    }

    public class Codes {
        public static final String SETTINGS = "21212221212219";
        public static final String TEAMVIEWER = "21212221212221";
        public static final String SHOW_IP = "21212221212220";
        public static final String MAC = "21212221212222";

        public static final String DIALOG = "143";
    }

    // Constantes de UI y tiempos
    public class UI {
        // Duración de visualización del HUD en milisegundos
        public static final long HUD_DISPLAY_DURATION_MS = 5000L;
        
        // Delay para inicialización diferida de PlayerActivity
        public static final long PLAYER_INIT_DELAY_MS = 50L;
        
        // Delay para detección de errores de codec
        public static final long CODEC_ERROR_DETECTION_DELAY_MS = 500L;
        
        // Delay para reintento tras error de codec
        public static final long CODEC_RETRY_DELAY_MS = 1000L;
    }

    // URLs y endpoints
    public class URLs {
        // Base URL para EPG (si se necesita centralizar)
        // Nota: La URL del EPG se obtiene desde la configuración de la API
    }
}

