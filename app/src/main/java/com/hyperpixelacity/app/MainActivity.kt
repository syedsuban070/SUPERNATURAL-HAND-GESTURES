package com.hyperpixelacity.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.hyperpixelacity.app.feature.studio.StudioViewModel
import com.hyperpixelacity.app.core.ui.HyperTheme
import com.hyperpixelacity.app.feature.studio.HyperAppScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: StudioViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { HyperTheme { HyperAppScreen(viewModel) } }
    }
}
