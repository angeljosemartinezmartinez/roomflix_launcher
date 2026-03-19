package com.roomflix.tv.views.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.window.Dialog
import com.roomflix.tv.compose.MoreAppsCarouselScreen
import com.roomflix.tv.compose.epg.PlayerEPGTheme
import com.roomflix.tv.views.activities.MainMenu
import com.roomflix.tv.viewmodel.MainMenuViewModel

/**
 * DialogFragment que muestra el carrusel "Más apps" usando Compose.
 * Usa androidx.compose.ui.window.Dialog para evitar ViewTreeLifecycleOwner not found
 * (el Fragment aporta el LifecycleOwner al árbol de vistas).
 */
class MoreAppsCarouselDialogFragment : DialogFragment() {

    private val viewModel: MainMenuViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainMenuViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PlayerEPGTheme {
                    LaunchedEffect(Unit) {
                        viewModel.loadInstalledApps()
                    }
                    Dialog(
                        onDismissRequest = { dismiss() }
                    ) {
                        MoreAppsCarouselScreen(
                            viewModel = viewModel,
                            onDismiss = { dismiss() },
                            onOpenApp = { pkg ->
                                (activity as? MainMenu)?.launchApp(pkg)
                                dismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}
