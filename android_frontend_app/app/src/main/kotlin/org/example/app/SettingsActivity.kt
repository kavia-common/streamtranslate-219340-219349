package org.example.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * PUBLIC_INTERFACE
 * SettingsActivity allows the user to configure:
 * - Stream URL
 * - Auto-translate toggle
 * - Translation latency simulation
 * - Optional sidecar VTT URL
 */
class SettingsActivity : ComponentActivity() {

    private lateinit var urlInput: EditText
    private lateinit var vttInput: EditText
    private lateinit var autoTranslateSwitch: Switch
    private lateinit var latencySeek: SeekBar
    private lateinit var latencyValue: TextView
    private lateinit var saveButton: Button
    private val repo by lazy { PreferencesRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        urlInput = findViewById(R.id.urlInput)
        vttInput = findViewById(R.id.vttInput)
        autoTranslateSwitch = findViewById(R.id.autoTranslateSwitch)
        latencySeek = findViewById(R.id.latencySeek)
        latencyValue = findViewById(R.id.latencyValue)
        saveButton = findViewById(R.id.saveButton)

        lifecycleScope.launch {
            val current = repo.getSettings()
            urlInput.setText(current.streamUrl)
            vttInput.setText(current.sidecarVttUrl ?: "")
            autoTranslateSwitch.isChecked = current.autoTranslate
            latencySeek.progress = (current.translationLatencyMs / 50).coerceIn(0, 40)
            latencyValue.text = getString(R.string.ms_format, current.translationLatencyMs)
        }

        latencySeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                latencyValue.text = getString(R.string.ms_format, progress * 50)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        saveButton.setOnClickListener {
            val url = urlInput.text?.toString()?.trim().orEmpty()
            val vtt = vttInput.text?.toString()?.trim().orEmpty().ifBlank { null }
            val auto = autoTranslateSwitch.isChecked
            val latency = latencySeek.progress * 50

            lifecycleScope.launch {
                repo.saveSettings(
                    UserSettings(
                        streamUrl = url.ifBlank { AppConstants.DEFAULT_HLS_URL },
                        autoTranslate = auto,
                        translationLatencyMs = latency,
                        sidecarVttUrl = vtt
                    )
                )
                Toast.makeText(this@SettingsActivity, R.string.saved, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        // PUBLIC_INTERFACE
        fun start(context: Context) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }
}
