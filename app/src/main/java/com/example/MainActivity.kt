package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.CalculatorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CalculatorViewModel
import com.example.viewmodel.CalculatorViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract central repository from Application context
        val app = application as CalculatorApplication
        val viewModelFactory = CalculatorViewModelFactory(app.repository)
        val viewModel: CalculatorViewModel by viewModels { viewModelFactory }

        setContent {
            val isDark by viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(darkTheme = isDark) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    CalculatorScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
