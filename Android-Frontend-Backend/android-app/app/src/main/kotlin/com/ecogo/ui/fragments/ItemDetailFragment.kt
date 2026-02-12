package com.ecogo.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ecogo.R
import com.ecogo.data.MascotSize
import com.ecogo.data.MockData
import com.ecogo.data.Outfit
import com.ecogo.databinding.FragmentItemDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Item detail page
 * Supports preview/try-on and purchase
 */
class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!
    
    private val args: ItemDetailFragmentArgs by navArgs()
    private var itemId: String = ""
    private var isOwned = false
    private var currentOutfit = Outfit(head = "none", face = "none", body = "shirt_nus", badge = "none")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        itemId = args.itemId
        
        setupUI()
        loadItemDetail()
        setupAnimations()
    }
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnPurchase.setOnClickListener {
            purchaseItem()
        }
        
        binding.btnEquip.setOnClickListener {
            equipItem()
        }
        
        binding.btnTryOn.setOnClickListener {
            togglePreview()
        }
    }
    
    private fun loadItemDetail() {
        val item = MockData.SHOP_ITEMS.find { it.id == itemId }
        
        if (item == null) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Error")
                .setMessage("Item not found")
                .setPositiveButton("OK") { _, _ ->
                    findNavController().navigateUp()
                }
                .show()
            return
        }
        
        // Display item info
        binding.textName.text = item.name
        binding.textType.text = when (item.type) {
            "head" -> "Headwear"
            "face" -> "Face"
            "body" -> "Body"
            "badge" -> "Badge"
            else -> item.type
        }
        binding.textCost.text = "${item.cost} pts"

        // Set mascot to display current outfit
        binding.mascotPreview.apply {
            mascotSize = MascotSize.XLARGE
            outfit = currentOutfit
        }
        
        // Check if already owned
        isOwned = item.owned
        updateButtonStates()
    }
    
    private fun updateButtonStates() {
        if (isOwned) {
            binding.btnPurchase.visibility = View.GONE
            binding.btnEquip.visibility = View.VISIBLE
            binding.btnTryOn.text = "Preview Outfit"
        } else {
            binding.btnPurchase.visibility = View.VISIBLE
            binding.btnEquip.visibility = View.GONE
            binding.btnTryOn.text = "Try On Preview"
        }
    }
    
    private fun togglePreview() {
        val item = MockData.SHOP_ITEMS.find { it.id == itemId } ?: return

        val newOutfit = toggleOutfitSlot(item.type, item.id)
        currentOutfit = newOutfit
        binding.mascotPreview.outfit = newOutfit
        binding.mascotPreview.celebrateAnimation()

        val isPreviewing = isSlotEquipped(item.type, item.id)
        binding.btnTryOn.text = getPreviewButtonText(isPreviewing)
    }

    private fun toggleOutfitSlot(type: String, id: String): Outfit {
        return when (type) {
            "head" -> currentOutfit.copy(head = if (currentOutfit.head == id) "none" else id)
            "face" -> currentOutfit.copy(face = if (currentOutfit.face == id) "none" else id)
            "body" -> currentOutfit.copy(body = if (currentOutfit.body == id) "none" else id)
            "badge" -> currentOutfit.copy(badge = if (currentOutfit.badge == id) "none" else id)
            else -> currentOutfit
        }
    }

    private fun isSlotEquipped(type: String, id: String): Boolean {
        return when (type) {
            "head" -> currentOutfit.head == id
            "face" -> currentOutfit.face == id
            "body" -> currentOutfit.body == id
            "badge" -> currentOutfit.badge == id
            else -> false
        }
    }

    private fun getPreviewButtonText(isPreviewing: Boolean): String {
        return if (isPreviewing) {
            if (isOwned) "Cancel Preview" else "Cancel Try-On"
        } else {
            if (isOwned) "Preview Outfit" else "Try On Preview"
        }
    }
    
    private fun purchaseItem() {
        val item = MockData.SHOP_ITEMS.find { it.id == itemId } ?: return
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Purchase")
            .setMessage("Use ${item.cost} pts to purchase ${item.name}?")
            .setPositiveButton("OK") { _, _ ->
                performPurchase(item.cost)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performPurchase(cost: Int) {
        // TODO: Call API to purchase
        isOwned = true
        updateButtonStates()

        // Show purchase success dialog
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_purchase_success)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val title = dialog.findViewById<TextView>(R.id.text_title)
        val message = dialog.findViewById<TextView>(R.id.text_message)
        val button = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.button_ok)
        
        title?.text = "Purchase Successful"
        message?.text = "Added to your closet\nPoints spent: -$cost"
        button?.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun equipItem() {
        // Equip item (return to Profile and apply outfit)
        android.widget.Toast.makeText(
            requireContext(),
            "Equipped",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        
        findNavController().navigateUp()
    }
    
    private fun setupAnimations() {
        val popIn = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_in)
        binding.cardItem.startAnimation(popIn)
        
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.cardInstructions.startAnimation(slideUp)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
