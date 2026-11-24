package com.vozcomunidad.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun accionConMicrofono(alPermitir: () -> Unit, alDenegar: () -> Unit): () -> Unit {
    val contexto = LocalContext.current
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) alPermitir() else alDenegar()
    }
    return {
        val concedido = ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (concedido) alPermitir() else pedir.launch(Manifest.permission.RECORD_AUDIO)
    }
}
