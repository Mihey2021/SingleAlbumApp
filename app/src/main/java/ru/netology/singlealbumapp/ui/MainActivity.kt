package ru.netology.singlealbumapp.ui

import android.annotation.SuppressLint
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import ru.netology.singlealbumapp.BuildConfig
import ru.netology.singlealbumapp.R
import ru.netology.singlealbumapp.adapters.SongsAdapter
import ru.netology.singlealbumapp.adapters.actionListener
import ru.netology.singlealbumapp.databinding.ActivityMainBinding
import ru.netology.singlealbumapp.dto.Album
import ru.netology.singlealbumapp.dto.Track
import ru.netology.singlealbumapp.viewmodels.SingleAlbumViewModel
import kotlin.coroutines.EmptyCoroutineContext

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: SingleAlbumViewModel by viewModels()
    private var adapter: SongsAdapter? = null
    private var mediaPlayer: MediaPlayer? = MediaPlayer()

    private lateinit var binding: ActivityMainBinding
    private lateinit var album: Album
    private var job: Job? = null

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.getAlbum()
        }

        with(binding) {
            next.setOnClickListener {
                val album = viewModel.getCurrentAlbum()
                val track = viewModel.getNextTrack() ?: return@setOnClickListener
                if (album != null)
                    play(album, track)
            }
            previous.setOnClickListener {
                val album = viewModel.getCurrentAlbum()
                val track = viewModel.getPrevTrack() ?: return@setOnClickListener
                if (album != null)
                    play(album, track)
            }
            stop.setOnClickListener {
                val track = viewModel.getCurrentTrack() ?: return@setOnClickListener
                if (track.initInPlayer == true) {
                    track.initInPlayer = false
                    track.isPlayed = false
                    adapter?.notifyDataSetChanged()
                }
                releaseMediaPlayer()
            }
        }

        viewModel.getAlbum()

        val songsRecyclerView = binding.songsList
        viewModel.data.observe(this) {
            if (binding.swipeRefreshLayout.isRefreshing)
                binding.swipeRefreshLayout.isRefreshing = false

            releaseMediaPlayer()

            val isError = it.error != null

            binding.songsList.visibility = if(isError) View.GONE else View.VISIBLE
            binding.retryButton.visibility = if(isError) View.VISIBLE else View.GONE

            if (isError) {
                showDialog(it.error ?: getString(R.string.unknown_error), getString(R.string.error_msg), isError = true)
                binding.retryButton.setOnClickListener {
                    viewModel.getAlbum()
                }
                return@observe
            }
            val trackSettings = viewModel.getTrackSettings()
            album = it.album ?: return@observe

            with(binding) {
                mainAlbumTitle.text = album.title
                mainExecutor.text = album.artist
                mainPublished.text = album.published
                mainGenre.text = album.genre
            }
            val tracks = album.tracks
            tracks.filter { track -> track.id in trackSettings }.map {filteringTrack -> filteringTrack.isLiked = true }

            adapter = SongsAdapter(object : actionListener {
                override fun onLikeListener(track: Track) {
                    like(it.album, track)
                }

                override fun onPlayListener(track: Track) {
                    play(it.album, track)
                }

                override fun onMenuClickListenerOne(track: Track) {
                    showDialog(
                        "${getString(R.string.album)}: ${track.albumTitle}\n${getString(R.string.track)}: ${track.file}",
                        getString(R.string.additional_action_one)
                    )
                }

                override fun onMenuClickListenerTwo(track: Track) {
                    showDialog(
                        "${getString(R.string.album)}: ${track.albumTitle}\n${getString(R.string.track)}: ${track.file}",
                        getString(R.string.additional_action_two)
                    )
                }

                override fun onRemoveListener(track: Track) {
                    val album = viewModel.getCurrentAlbum()
                    val nextTrack = viewModel.getNextTrack() ?: return
                    viewModel.removeTrack(track)
                    viewModel.saveTrackSettings(album?.tracks?.filter { it.isLiked } ?: emptyList())
                    if (album?.tracks?.size == 0) {
                        releaseMediaPlayer()
                        adapter?.notifyDataSetChanged()
                        return
                    }
                    if (album != null)
                        play(album, nextTrack)
                }
            })

            binding.mainPlay.setOnClickListener {
                val currentTrack = viewModel.getCurrentTrack() ?: return@setOnClickListener
                play(album, currentTrack)
            }

            songsRecyclerView.adapter = adapter
            adapter?.submitList(tracks)
        }

    }

    private fun releaseMediaPlayer() {
        job?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        binding.mainPlay.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        initializeSeekBar()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun play(album: Album, track: Track) {
        track.isPlayed = !track.isPlayed
        viewModel.play(album, track)
        binding.mainPlay.setImageResource(if (track.isPlayed) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24)
        adapter?.notifyDataSetChanged()
        job?.cancel()
        if (!track.initInPlayer) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setOnCompletionListener {
                val nextTrack = viewModel.getNextTrack() ?: return@setOnCompletionListener
                play(album, nextTrack)
            }
            mediaPlayer?.setDataSource("${BuildConfig.BASE_URL}${track.file}")
            track.initInPlayer = true
            mediaPlayer?.prepareAsync()
            mediaPlayer?.setOnPreparedListener {
                it.start()
                initializeSeekBar(mediaPlayer)
            }
            return
        }
        if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        } else {
            mediaPlayer?.pause()
        }
        initializeSeekBar(mediaPlayer)
    }

    @SuppressLint("SetTextI18n")
    private fun initializeSeekBar(mediaPlayer: MediaPlayer? = null) {
        if (mediaPlayer != null) {
            binding.duration.text = getFormattingTimeString(mediaPlayer.duration)
        } else {
            binding.duration.text = ""
        }
        binding.currentPosition.text = ""
        val seekBar = binding.seekBar
        seekBar.max = mediaPlayer?.duration ?: 0
        seekBar.progress = mediaPlayer?.currentPosition ?: 0
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    mediaPlayer?.seekTo(progress)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        with(CoroutineScope(EmptyCoroutineContext)) {
            try {
                job = launch {
                    while (mediaPlayer?.isPlaying == true) {
                        withContext(Dispatchers.Main) {
                            val currentPosition = mediaPlayer.currentPosition
                            seekBar.progress = currentPosition
                            binding.currentPosition.text =
                                if (currentPosition == 0) "0:00" else getFormattingTimeString(
                                    currentPosition
                                )
                        }
                        delay(500)
                    }
                }
            } catch (e: Exception) {
                showDialog(e.message.toString(), getString(R.string.error_msg), isError = true)
            }
        }
    }

    private fun getFormattingTimeString(duration: Int): String {
        val min = duration / 1000 / 60
        val sec = duration / 1000 % 60
        return "$min:${if (sec == 0) "00" else if (sec > 9) sec else "0$sec"}"
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun like(album: Album, track: Track) {
        viewModel.like(album, track)
        adapter?.notifyDataSetChanged()
    }

    private fun showDialog(message: String, title: String, isError: Boolean = false) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setIcon(if (isError) R.drawable.ic_baseline_error_24 else R.drawable.ic_baseline_music_note_24)
            .setCancelable(!isError)
            .setPositiveButton(getString(R.string.ok_text)) { dialog, _ ->
                dialog.dismiss()
            }
            .apply {
                if (isError) setNegativeButton(getString(R.string.retry)) { dialog, _ ->
                    viewModel.getAlbum()
                    dialog.dismiss()
                }
            }
            .show()
    }

//Раскомментировать если нужно ставить medialPlayer на паузу при сворачивании Activity
//    @SuppressLint("NotifyDataSetChanged")
//    override fun onPause() {
//        mediaPlayer?.pause()
//        val track = viewModel.getCurrentTrack()
//        if (track?.initInPlayer == true) {
//            track.isPlayed = false
//            binding.mainPlay.setImageResource(R.drawable.ic_baseline_play_arrow_24)
//            adapter?.notifyDataSetChanged()
//        }
//        super.onPause()
//    }

//Раскомментировать если нужно останавливать medialPlayer при onStop Activity
//    @SuppressLint("NotifyDataSetChanged")
//    override fun onStop() {
//
//        val track = viewModel.getCurrentTrack()
//        if(track?.initInPlayer == true)
//        {
//            track.initInPlayer = false
//            track.isPlayed = false
//            adapter?.notifyDataSetChanged()
//            binding.mainPlay.setImageResource(R.drawable.ic_baseline_play_arrow_24)
//        }
//        job?.cancel()
//        mediaPlayer?.stop()
//        mediaPlayer?.release()
//        mediaPlayer = null
//        super.onStop()
//    }

    override fun onDestroy() {
        job?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onResume() {
        job?.cancel()
        initializeSeekBar(mediaPlayer)
        super.onResume()
    }

}
