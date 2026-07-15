package moe.shizuku.manager.filemanager.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import moe.shizuku.manager.app.ThemeHelper

class FileManagerActivity : ComponentActivity() {

    private val viewModel: FileManagerViewModel by viewModels { FileManagerViewModelFactory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            FileManagerScreen(
                viewModel = viewModel,
                onBack = { finish() }
            )
        }
    }
}
