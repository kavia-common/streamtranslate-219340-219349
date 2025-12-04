package org.example.app

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * PUBLIC_INTERFACE
 * MainActivity is the primary UI entry. It hosts the player view, language selectors,
 * subtitle overlay, and basic controls including CC toggle and Translate toggle.
 * It streams an HLS URL defined in AppConstants (can be changed in settings).
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: PlayerViewModel
    private lateinit var playerView: PlayerView
    private lateinit var subtitleOverlay: TextView
    private lateinit var originalSubtitleView: TextView
    private lateinit var translatedSubtitleView: TextView
    private lateinit var srcSpinner: Spinner
    private lateinit var dstSpinner: Spinner
    private lateinit var playPause: ImageButton
    private lateinit var muteToggle: ImageButton
    private lateinit var ccToggle: CheckBox
    private lateinit var translateToggle: CheckBox
    private lateinit var settingsButton: ImageButton
    private lateinit var errorSnackbar: TextView

    private var player: ExoPlayer? = null
    private var playerInitJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate layout
        setContentView(R.layout.activity_main)

        // Setup ViewModel
        viewModel = ViewModelProvider(this)[PlayerViewModel::class.java]

        // Bind views
        playerView = findViewById(R.id.playerView)
        subtitleOverlay = findViewById(R.id.subtitleOverlay)
        originalSubtitleView = findViewById(R.id.originalSubtitle)
        translatedSubtitleView = findViewById(R.id.translatedSubtitle)
        srcSpinner = findViewById(R.id.srcSpinner)
        dstSpinner = findViewById(R.id.dstSpinner)
        playPause = findViewById(R.id.playPause)
        muteToggle = findViewById(R.id.muteToggle)
        ccToggle = findViewById(R.id.ccToggle)
        translateToggle = findViewById(R.id.translateToggle)
        settingsButton = findViewById(R.id.settingsButton)
        errorSnackbar = findViewById(R.id.errorSnackbar)

        // Populate language spinners
        ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Languages.common
        ).also { adapter ->
            srcSpinner.adapter = adapter
            dstSpinner.adapter = adapter
        }

        // Restore persisted selections
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                // Update spinners only when needed to avoid loops
                (srcSpinner.selectedItem as? String)?.let {
                    if (it != state.srcLang) {
                        setSpinnerSelection(srcSpinner, state.srcLang)
                    }
                } ?: setSpinnerSelection(srcSpinner, state.srcLang)
                (dstSpinner.selectedItem as? String)?.let {
                    if (it != state.dstLang) {
                        setSpinnerSelection(dstSpinner, state.dstLang)
                    }
                } ?: setSpinnerSelection(dstSpinner, state.dstLang)

                translateToggle.isChecked = state.autoTranslate
                ccToggle.isChecked = state.ccEnabled

                // Update subtitles
                if (state.showBothSubtitles) {
                    originalSubtitleView.visibility = View.VISIBLE
                    translatedSubtitleView.visibility = View.VISIBLE
                    originalSubtitleView.text = state.currentOriginalText
                    translatedSubtitleView.text = state.currentTranslatedText
                    subtitleOverlay.visibility = View.GONE
                } else {
                    originalSubtitleView.visibility = View.GONE
                    translatedSubtitleView.visibility = View.GONE
                    subtitleOverlay.visibility = View.VISIBLE
                    subtitleOverlay.text =
                        if (state.autoTranslate) state.currentTranslatedText else state.currentOriginalText
                }

                // Error message (non-intrusive)
                if (state.errorMessage != null) {
                    errorSnackbar.text = state.errorMessage
                    errorSnackbar.visibility = View.VISIBLE
                } else {
                    errorSnackbar.visibility = View.GONE
                }
            }
        }

        // Listeners
        srcSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val lang = Languages.common[position]
                viewModel.updateSrcLanguage(lang)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        dstSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val lang = Languages.common[position]
                viewModel.updateDstLanguage(lang)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        translateToggle.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoTranslate(isChecked)
        }
        ccToggle.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setCcEnabled(isChecked)
        }
        playPause.setOnClickListener {
            player?.let { p ->
                if (p.isPlaying) p.pause() else p.play()
                updatePlayPauseIcon()
            }
        }
        muteToggle.setOnClickListener {
            player?.let { p ->
                p.volume = if (p.volume == 0f) 1f else 0f
                updateMuteIcon()
            }
        }
        settingsButton.setOnClickListener {
            SettingsActivity.start(this)
        }

        initPlayerAndStart()
    }

    private fun setSpinnerSelection(spinner: Spinner, value: String) {
        val index = Languages.common.indexOfFirst { it.equals(value, ignoreCase = true) }
        if (index >= 0) spinner.setSelection(index)
    }

    private fun initPlayerAndStart() {
        playerInitJob?.cancel()
        playerInitJob = lifecycleScope.launch {
            // Create player
            val exo = ExoPlayer.Builder(this@MainActivity).build()
            player = exo
            playerView.player = exo

            val dataSourceFactory = DefaultDataSource.Factory(this@MainActivity)
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(viewModel.resolveStreamUrl(this@MainActivity)))
                .setMimeType("application/x-mpegURL")
                .build()

            val hlsSource: MediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)

            // Optional: attach sidecar VTT if provided in settings
            val sidecarUri = viewModel.resolveSidecarVtt(this@MainActivity)
            val source: MediaSource = if (sidecarUri != null) {
                val textItem = MediaItem.SubtitleConfiguration.Builder(sidecarUri)
                    .setMimeType("text/vtt")
                    .setLanguage("en")
                    .setSelectionFlags(0)
                    .build()
                val textSource = HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(
                        MediaItem.Builder()
                            .setUri(Uri.parse(viewModel.resolveStreamUrl(this@MainActivity)))
                            .setSubtitleConfigurations(listOf(textItem))
                            .build()
                    )
                MergingMediaSource(hlsSource, textSource)
            } else {
                hlsSource
            }

            exo.addListener(viewModel.playerEventsListener)
            // Listen to text cues via Player.Listener (supported across ExoPlayer versions)
            exo.addListener(object : com.google.android.exoplayer2.Player.Listener {
                override fun onCues(cues: com.google.android.exoplayer2.text.CueGroup) {
                    viewModel.onCues(cues.cues)
                }
            })

            exo.setMediaSource(source)
            exo.prepare()
            exo.playWhenReady = true

            updatePlayPauseIcon()
            updateMuteIcon()
        }
    }

    private fun updatePlayPauseIcon() {
        val isPlaying = player?.isPlaying == true
        playPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        playPause.contentDescription = if (isPlaying) getString(R.string.pause) else getString(R.string.play)
    }

    private fun updateMuteIcon() {
        val muted = player?.volume == 0f
        muteToggle.setImageResource(if (muted) R.drawable.ic_volume_off else R.drawable.ic_volume_on)
        muteToggle.contentDescription = if (muted) getString(R.string.unmute) else getString(R.string.mute)
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerView.player = null
        player?.release()
        player = null
    }
}
