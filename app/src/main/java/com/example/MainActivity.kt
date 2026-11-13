package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.billing.PlayBillingManager
import com.example.data.repository.AppRepository
import com.example.ui.MainApp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var repository: AppRepository
    private lateinit var billingManager: PlayBillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = AppRepository(applicationContext)
        com.example.data.auth.FirebaseAuthManager.initialize(applicationContext)
        billingManager = PlayBillingManager.getInstance(applicationContext, repository)
        billingManager.startConnection()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    MainApp(repository = repository)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::billingManager.isInitialized) {
            billingManager.startConnection()
        }
    }
}
