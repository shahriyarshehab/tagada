package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import com.example.auth.AuthManager
import com.example.ui.TagadaApp
import com.example.ui.TagadaViewModel
import com.example.ui.theme.TagadaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TagadaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Attempt silent background Google Sign-In if previous credentials exist
        val credentialManager = CredentialManager.create(this)
        AuthManager.attemptAutoSignIn(
            context = this,
            credentialManager = credentialManager,
            onAuthSuccess = { user ->
                viewModel.setUser(user)
            },
            onUnauthenticated = {
                // Keep current state or offline local storage
            },
            scope = lifecycleScope
        )

        setContent {
            val darkMode by viewModel.darkMode.collectAsState()

            TagadaTheme(darkTheme = darkMode) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) { innerPadding ->
                    TagadaApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
