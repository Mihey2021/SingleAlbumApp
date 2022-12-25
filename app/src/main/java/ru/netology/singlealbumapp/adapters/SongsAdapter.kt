package ru.netology.singlealbumapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.singlealbumapp.R
import ru.netology.singlealbumapp.databinding.SongItemBinding
import ru.netology.singlealbumapp.dto.Track

interface actionListener {
    fun onLikeListener(track: Track)
    fun onRemoveListener(track: Track)
    fun onPlayListener(track: Track)
    fun onMenuClickListenerOne(track: Track)
    fun onMenuClickListenerTwo(track: Track)
}

class SongsAdapter(
    private val actionListener: actionListener,
) : ListAdapter<Track, SongsAdapter.SongsAdapterViewHolder>(PostDiffCallback()) {

    class SongsAdapterViewHolder(
        private val binding: SongItemBinding,
        private val actionListener: actionListener,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(track: Track) {
            binding.apply {
                songTitle.text = track.file
                albumTitle.text = track.albumTitle
                play.setOnClickListener {
                    actionListener.onPlayListener(track)
                }
                play.setImageResource(if (track.isPlayed) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24)
                if (track.isPlayed) mainButtonsGroup.visibility =
                    View.VISIBLE else mainButtonsGroup.visibility = View.GONE
                like.isChecked = track.isLiked
                like.setOnClickListener {
                    actionListener.onLikeListener(track)
                }
                remove.setOnClickListener {
                    actionListener.onRemoveListener(track)
                }

                more.setOnClickListener {
                    PopupMenu(it.context, it).apply {
                        inflate(R.menu.song_actions)
                        setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.additionalOne -> {
                                    actionListener.onMenuClickListenerOne(track)
                                    true
                                }
                                R.id.additionalTwo -> {
                                    actionListener.onMenuClickListenerTwo(track)
                                    true
                                }
                                else -> false
                            }
                        }
                    }.show()
                }

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsAdapterViewHolder {
        val binding = SongItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongsAdapterViewHolder(binding, actionListener)
    }

    override fun onBindViewHolder(holder: SongsAdapterViewHolder, position: Int) =
        holder.bind(getItem(position))
}

class PostDiffCallback : DiffUtil.ItemCallback<Track>() {
    override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
        return (oldItem.id == newItem.id)
    }

    override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem == newItem
    }

    //Уберем мерцание
    override fun getChangePayload(oldItem: Track, newItem: Track): Any = Unit
}