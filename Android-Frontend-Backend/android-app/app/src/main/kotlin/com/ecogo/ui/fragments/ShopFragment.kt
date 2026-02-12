package com.ecogo.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecogo.R
import com.ecogo.data.MockData
import com.ecogo.data.Product
import com.ecogo.data.RedeemResponse
import com.ecogo.databinding.FragmentShopBinding
import com.ecogo.repository.EcoGoRepository
import com.ecogo.ui.adapters.ProductAdapter
import com.google.android.material.button.MaterialButton
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch

class ShopFragment : Fragment() {
    
    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!
    private val repository = EcoGoRepository()
    
    private var currentPoints = 1250  // Loaded from user data
    private var currentFilter = "all"  // "all", "voucher", "goods"
    
    private lateinit var paymentSheet: PaymentSheet
    private var currentProduct: Product? = null
    private var currentPaymentIntentId: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Stripe PaymentSheet
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
        
        setupUI()
        setupTabs()
        setupRecyclerView()
        loadProducts()
    }
    
    private fun setupUI() {
        binding.textPoints.text = "$currentPoints pts"
        binding.textCash.text = "$50.00 SGD"  // Simulated cash balance
    }
    
    private fun setupTabs() {
        binding.tabAll.setOnClickListener { filterProducts("all") }
        binding.tabVouchers.setOnClickListener { filterProducts("voucher") }
        binding.tabGoods.setOnClickListener { filterProducts("goods") }
    }
    
    private fun filterProducts(filter: String) {
        currentFilter = filter
        loadProducts()
    }
    
    private fun setupRecyclerView() {
        val adapter = ProductAdapter(emptyList()) { product: Product, paymentMethod: String ->
            handlePurchase(product, paymentMethod)
        }
        
        binding.recyclerProducts.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }
    }
    
    private fun loadProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.getShopProducts(
                type = if (currentFilter == "all") null else currentFilter
            )
            
            result.onSuccess { products: List<Product> ->
                // When API returns empty list (e.g. no vouchers in backend), use MockData so voucher/goods tabs show content
                val listToShow = if (products.isEmpty()) {
                    when (currentFilter) {
                        "voucher" -> MockData.PRODUCTS.filter { product -> product.type == "voucher" }
                        "goods" -> MockData.PRODUCTS.filter { product -> product.type == "goods" }
                        else -> MockData.PRODUCTS
                    }
                } else {
                    products
                }
                (binding.recyclerProducts.adapter as ProductAdapter).updateProducts(listToShow)
            }.onFailure { error: Throwable ->
                Toast.makeText(context, "Loading failed: ${error.message}", Toast.LENGTH_SHORT).show()
                val filteredProducts = when (currentFilter) {
                    "voucher" -> MockData.PRODUCTS.filter { product -> product.type == "voucher" }
                    "goods" -> MockData.PRODUCTS.filter { product -> product.type == "goods" }
                    else -> MockData.PRODUCTS
                }
                (binding.recyclerProducts.adapter as ProductAdapter).updateProducts(filteredProducts)
            }
        }
    }
    
    private fun handlePurchase(product: Product, paymentMethod: String) {
        when (paymentMethod) {
            "points" -> redeemWithPoints(product)
            "cash" -> buyWithCash(product)  // Phase 2 implementation
        }
    }
    
    private fun redeemWithPoints(product: Product) {
        if (product.pointsPrice == null) {
            Toast.makeText(context, "This item does not support points redemption", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (currentPoints < product.pointsPrice) {
            showInsufficientPointsDialog(product.pointsPrice)
            return
        }
        
        // Show confirm dialog
        showRedeemConfirmDialog(product) {
            performRedeem(product)
        }
    }
    
    private fun showInsufficientPointsDialog(requiredPoints: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Insufficient Points")
            .setMessage("Need $requiredPoints pts, currently $currentPoints pts\n\n" +
                    "Earn more points through green travel, joining activities, and more!")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showRedeemConfirmDialog(product: Product, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Redemption")
            .setMessage("Use ${product.pointsPrice} pts to redeem\n${product.name}?")
            .setPositiveButton("Confirm") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performRedeem(product: Product) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.redeemProduct(
                userId = "user123",
                productId = product.id,
                productType = product.type
            )
            
            result.onSuccess { response: RedeemResponse ->
                currentPoints -= (product.pointsPrice ?: 0)
                binding.textPoints.text = "$currentPoints pts"
                showSuccessDialog("Successfully redeemed ${product.name}!", "-${product.pointsPrice} pts")
                loadProducts()  // Refresh list
            }.onFailure { error: Throwable ->
                Toast.makeText(context, "Redemption failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showSuccessDialog(message: String, points: String) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_success)
        dialog.window?.setBackgroundDrawableResource(android.R.drawable.screen_background_light_transparent)
        
        val textMessage = dialog.findViewById<TextView>(R.id.text_message)
        val textPoints = dialog.findViewById<TextView>(R.id.text_points)
        val buttonOk = dialog.findViewById<MaterialButton>(R.id.button_ok)
        
        textMessage.text = message
        textPoints.text = points
        textPoints.visibility = View.VISIBLE
        
        buttonOk.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun buyWithCash(product: Product) {
        if (product.cashPrice == null) {
            Toast.makeText(context, "This item does not support cash purchase", Toast.LENGTH_SHORT).show()
            return
        }
        
        currentProduct = product
        
        // Show payment purpose description
        showPaymentInfoDialog(product) {
            initializePayment(product)
        }
    }
    
    private fun showPaymentInfoDialog(product: Product, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Support Green Transportation")
            .setMessage(
                "By purchasing ${product.name} for $${"%.2f".format(product.cashPrice)}, " +
                "you're supporting:\n\n" +
                "🌱 Expanding eco-friendly bus routes\n" +
                "🚴 Bicycle infrastructure maintenance\n" +
                "🌳 Campus tree planting initiatives\n" +
                "♻️ Sustainability education programs\n\n" +
                "Proceed to payment?"
            )
            .setPositiveButton("Continue") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun initializePayment(product: Product) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.createPaymentIntent(
                userId = "user123",
                productId = product.id
            )
            
            result.onSuccess { paymentData: com.ecogo.api.PaymentIntentResponse ->
                // Configure Stripe
                PaymentConfiguration.init(
                    requireContext(),
                    paymentData.publishableKey
                )
                
                // Save PaymentIntent ID
                currentPaymentIntentId = extractPaymentIntentId(paymentData.clientSecret)
                
                // Show payment form
                presentPaymentSheet(paymentData.clientSecret)
            }.onFailure { error: Throwable ->
                Toast.makeText(context, "Payment initialization failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun extractPaymentIntentId(clientSecret: String): String {
        // ClientSecret格式: pi_xxxxx_secret_yyyyy
        return clientSecret.split("_secret_")[0]
    }
    
    private fun presentPaymentSheet(clientSecret: String) {
        paymentSheet.presentWithPaymentIntent(
            clientSecret,
            PaymentSheet.Configuration(
                merchantDisplayName = "EcoGo",
                allowsDelayedPaymentMethods = true
            )
        )
    }
    
    private fun onPaymentSheetResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                // Payment successful, confirm order
                confirmPaymentWithBackend()
            }
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(context, "Payment cancelled", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(context, "Payment failed: ${result.error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun confirmPaymentWithBackend() {
        viewLifecycleOwner.lifecycleScope.launch {
            val product = currentProduct ?: return@launch
            val paymentIntentId = currentPaymentIntentId ?: return@launch
            
            val result = repository.confirmPayment(
                userId = "user123",
                productId = product.id,
                paymentIntentId = paymentIntentId
            )
            
            result.onSuccess { order: com.ecogo.api.OrderDto ->
                showPurchaseSuccessDialog(product)
                loadProducts()  // Refresh list
            }.onFailure { error: Throwable ->
                Toast.makeText(context, "Order confirmation failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showPurchaseSuccessDialog(product: Product) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_purchase_success)
        dialog.window?.setBackgroundDrawableResource(android.R.drawable.screen_background_light_transparent)
        
        val message = dialog.findViewById<TextView>(R.id.text_message)
        val amount = dialog.findViewById<TextView>(R.id.text_amount)
        val impact = dialog.findViewById<TextView>(R.id.text_impact)
        val buttonOk = dialog.findViewById<MaterialButton>(R.id.button_ok)
        
        message.text = "Thank you for purchasing ${product.name}!"
        amount.text = "$${"%.2f".format(product.cashPrice)} SGD"
        impact.text = "Your contribution helps us expand green transportation options on campus"
        
        buttonOk.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
