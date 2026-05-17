package com.hexodus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import rikka.shizuku.Shizuku
import com.hexodus.core.ThemeCompiler

@Composable
fun HexodusApp() {
    var hexInput by remember { mutableStateOf("FF000000") } 
    var statusText by remember { mutableStateOf("Ready to compile.") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.padding(32.dp).fillMaxSize()) {
        Text("Hexodus: Dirty Dev Build", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = hexInput,
            onValueChange = { hexInput = it },
            label = { Text("Enter Hex (e.g. FF000000 for pitch black)") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            onClick = {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    statusText = "Compiling APK in memory..."
                    val compiler = ThemeCompiler()
                    val apkBytes = compiler.compileTheme(
                        hexColor = hexInput,
                        packageName = "com.hexodus.customtheme",
                        themeName = "Liquid Glass",
                        themedComponents = mapOf("system_ui" to true, "settings" to true)
                    )
                    
                    statusText = "Saving APK..."
                    val apkFile = File(context.cacheDir, "compiled_theme.apk")
                    FileOutputStream(apkFile).use { it.write(apkBytes) }
                    
                    statusText = "Installing via Shizuku..."
                    val process = Shizuku.newProcess(arrayOf("sh", "-c", "pm install -r ${apkFile.absolutePath}"), null, null)
                    process.waitFor()
                    
                    statusText = "Enabling overlay..."
                    val enableCmd = "cmd overlay enable com.hexodus.customtheme"
                    Shizuku.newProcess(arrayOf("sh", "-c", enableCmd), null, null).waitFor()
                    
                    statusText = "SUCCESS! Reboot phone to apply."
                } catch (e: Exception) {
                    statusText = "Error: ${e.message}"
                }
            }
        }) {
            Text("COMPILE & APPLY")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(statusText)
    }
}
