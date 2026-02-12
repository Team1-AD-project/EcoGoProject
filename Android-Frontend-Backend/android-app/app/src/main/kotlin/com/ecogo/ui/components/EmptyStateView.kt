package com.ecogo.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.ecogo.R
import com.ecogo.data.MascotEmotion
import com.ecogo.data.MascotSize
import com.ecogo.databinding.ViewEmptyStateBinding

/**
 * EmptyStateView - Empty state component
 *
 * Supported states:
 * - NETWORK_ERROR: Lion mascot confused expression + "Connection lost"
 * - NO_DATA: Lion mascot sleeping + "No content here yet"
 * - LOADING: Lion mascot running animation + "Loading..."
 */
class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewEmptyStateBinding

    enum class State {
        NETWORK_ERROR,
        NO_DATA,
        LOADING
    }

    init {
        binding = ViewEmptyStateBinding.inflate(LayoutInflater.from(context), this, true)
        binding.mascotEmpty.mascotSize = MascotSize.LARGE
    }

    fun setState(state: State, actionText: String? = null, onActionClick: (() -> Unit)? = null) {
        when (state) {
            State.NETWORK_ERROR -> {
                binding.mascotEmpty.setEmotion(MascotEmotion.CONFUSED)
                binding.textEmptyTitle.text = context.getString(R.string.empty_network_error_title)
                binding.textEmptyDesc.text = context.getString(R.string.empty_network_error_desc)
                
                if (actionText != null) {
                    binding.buttonAction.text = actionText
                    binding.buttonAction.visibility = VISIBLE
                    binding.buttonAction.setOnClickListener { onActionClick?.invoke() }
                } else {
                    binding.buttonAction.visibility = GONE
                }
            }
            State.NO_DATA -> {
                binding.mascotEmpty.setEmotion(MascotEmotion.SLEEPING)
                binding.textEmptyTitle.text = context.getString(R.string.empty_no_data_title)
                binding.textEmptyDesc.text = context.getString(R.string.empty_no_data_desc)
                binding.buttonAction.visibility = GONE
            }
            State.LOADING -> {
                binding.mascotEmpty.setEmotion(MascotEmotion.NORMAL)
                binding.textEmptyTitle.text = context.getString(R.string.empty_loading_title)
                binding.textEmptyDesc.text = context.getString(R.string.empty_loading_desc)
                binding.buttonAction.visibility = GONE
                
                // Start breathing animation to indicate loading
                binding.mascotEmpty.invalidate()
            }
        }
    }
}
