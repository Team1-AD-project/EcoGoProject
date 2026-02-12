package com.ecogo.ui.dialogs

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.animation.BounceInterpolator
import com.ecogo.R
import com.ecogo.data.Achievement
import com.ecogo.data.MascotSize
import com.ecogo.databinding.DialogAchievementUnlockBinding

/**
 * AchievementUnlockDialog - Achievement unlock dialog
 *
 * Features:
 * - Lion mascot celebration animation
 * - Badge drop animation
 * - Points reward display
 */
class AchievementUnlockDialog(
    context: Context,
    private val achievement: Achievement,
    private val pointsEarned: Int = 0,
    private val onDismiss: (() -> Unit)? = null
) : Dialog(context) {

    private lateinit var binding: DialogAchievementUnlockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        binding = DialogAchievementUnlockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set dialog background to transparent
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.attributes?.width = android.view.WindowManager.LayoutParams.MATCH_PARENT
        
        setupUI()
        playAnimations()
        
        setOnDismissListener {
            onDismiss?.invoke()
        }
    }

    private fun setupUI() {
        // Set achievement info
        binding.textAchievementName.text = achievement.name
        binding.textAchievementDesc.text = achievement.description
        
        // Set up lion mascot
        binding.mascotCelebrate.apply {
            mascotSize = MascotSize.XLARGE
        }
        
        // Show points reward
        if (pointsEarned > 0) {
            binding.layoutPointsReward.visibility = View.VISIBLE
            binding.textPointsEarned.text = pointsEarned.toString()
        }
        
        // Confirm button
        binding.buttonAwesome.setOnClickListener {
            dismiss()
        }
    }

    private fun playAnimations() {
        // Delay animation start to let the dialog display first
        binding.root.postDelayed({
            // 1. Lion mascot celebration animation
            binding.mascotCelebrate.celebrateAnimation()
            
            // 2. Badge drop animation (after 200ms)
            binding.root.postDelayed({
                playBadgeDropAnimation()
            }, 200)
            
            // 3. Points number animation (after 600ms)
            if (pointsEarned > 0) {
                binding.root.postDelayed({
                    playPointsAnimation()
                }, 600)
            }
        }, 100)
    }

    private fun playBadgeDropAnimation() {
        binding.imageBadgeDrop.apply {
            visibility = View.VISIBLE
            setImageResource(getBadgeDrawable(achievement.id))
            
            // Drop from top to lion mascot's chest
            val startY = -100f
            val endY = 80f  // Lion mascot's chest position
            
            val dropAnimator = ValueAnimator.ofFloat(startY, endY).apply {
                duration = 600
                interpolator = BounceInterpolator()
                addUpdateListener { animation ->
                    translationY = animation.animatedValue as Float
                }
            }
            dropAnimator.start()
        }
    }

    private fun playPointsAnimation() {
        // Animate points number from 0 to target value
        ValueAnimator.ofInt(0, pointsEarned).apply {
            duration = 800
            addUpdateListener { animation ->
                binding.textPointsEarned.text = animation.animatedValue.toString()
            }
            start()
        }
        
        // Spark effect (scale animation)
        binding.layoutPointsReward.apply {
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(300)
                .withEndAction {
                    animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }
    }

    private fun getBadgeDrawable(badgeId: String): Int {
        // 根据徽章ID返回对应的drawable资源
        // 这里简化处理，实际应用中应该有对应的徽章图标
        return when (badgeId) {
            "a1" -> R.drawable.ic_award
            "a2" -> R.drawable.ic_award
            "a3" -> R.drawable.ic_award
            "a4" -> R.drawable.ic_award
            "a5" -> R.drawable.ic_award
            "a6" -> R.drawable.ic_award
            else -> R.drawable.ic_award
        }
    }

    companion object {
        /**
         * 显示成就解锁弹窗
         */
        fun show(
            context: Context,
            achievement: Achievement,
            pointsEarned: Int = 0,
            onDismiss: (() -> Unit)? = null
        ) {
            AchievementUnlockDialog(context, achievement, pointsEarned, onDismiss).show()
        }
    }
}
