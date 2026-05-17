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
    var hexInput by remember { mutableStateOf("FFFF00FF") } // Default to Neon Pink for testing!
    var statusText by remember { mutableStateOf("Ready to compile.") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.padding(32.dp).fillMaxSize()) {
        Text("Hexodus: Override Build", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Raw compiler access.", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = hexInput,
            onValueChange = { hexInput = it },
            label = { Text("Enter Hex Color") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            onClick = {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    statusText = "1/4: Compiling APK in memory..."
                    val compiler = ThemeCompiler()
                    
                    val components = mapOf(
                        "system_ui" to true, 
                        "settings" to true, 
                        "status_bar" to true, 
                        "navigation_bar" to true
                    )
                    
                    val apkBytes = compiler.compileTheme(
                        hexColor = hexInput,
                        packageName = "com.hexodus.liquidglass",
                        themeName = "Liquid Glass Overwrite",
                        themedComponents = components
                    )
                    
                    statusText = "2/4: Saving APK to cache..."
                    val apkFile = File(context.cacheDir, "compiled_theme.apk")
                    FileOutputStream(apkFile).use { it.write(apkBytes) }
                    
                    statusText = "3/4: Installing via Shizuku..."
                    
                    // THE REFLECTION CROWBAR: Bypassing the Shizuku private lock
                    val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                    newProcessMethod.isAccessible = true
                    
                    val installCmd = arrayOf("sh", "-c", "pm install -r ${apkFile.absolutePath}")
                    val installProcess = newProcessMethod.invoke(null, installCmd, null, null) as Process
                    installProcess.waitFor()
                    
                    statusText = "4/4: Enabling Overlay..."
                    val enableCmd = arrayOf("sh", "-c", "cmd overlay enable com.hexodus.liquidglass")
                    val enableProcess = newProcessMethod.invoke(null, enableCmd, null, null) as Process
                    enableProcess.waitFor()
                    
                    statusText = "SUCCESS! Restart your phone to apply colors."
                } catch (e: Exception) {
                    statusText = "ERROR: ${e.message}"
                }
            }
        }) {
            Text("COMPILE & INJECT")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text(
                text = statusText, 
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
