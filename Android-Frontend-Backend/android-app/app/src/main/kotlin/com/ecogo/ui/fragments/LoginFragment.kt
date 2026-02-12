package com.ecogo.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecogo.R
import androidx.lifecycle.lifecycleScope
import com.ecogo.api.MobileLoginRequest
import com.ecogo.api.RetrofitClient
import com.ecogo.auth.TokenManager
import kotlinx.coroutines.launch
import com.ecogo.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("DEBUG_LOGIN", "LoginFragment onViewCreated")
        
        binding.buttonSignIn.setOnClickListener {
            val inputNusnetId = binding.editNusnetId.text.toString()
            val inputPassword = binding.editPassword.text.toString()

            Log.d("DEBUG_LOGIN", "SignIn button clicked")

            if (inputNusnetId.isEmpty() || inputPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter NUSNET ID and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (inputNusnetId == "123" && inputPassword == "123") {
                handleTestLogin()
                return@setOnClickListener
            }

            performApiLogin(inputNusnetId, inputPassword)
        }
        
        binding.buttonRegister.setOnClickListener {
            Log.d("DEBUG_LOGIN", "Register button clicked - attempting navigate")
            Toast.makeText(requireContext(), "Navigating to registration page...", Toast.LENGTH_SHORT).show()
            try {
                findNavController().navigate(R.id.action_login_to_signup)
                Log.d("DEBUG_LOGIN", "Navigate to signup completed successfully")
                Toast.makeText(requireContext(), "Navigation command executed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("DEBUG_LOGIN", "Navigation to signup FAILED: ${e.message}", e)
                Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun handleTestLogin() {
        Log.d("DEBUG_LOGIN", "Test account login successful")
        val prefs = requireContext().getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_logged_in", true).apply()
        Toast.makeText(requireContext(), "Test Account Login Successful!", Toast.LENGTH_SHORT).show()
        try {
            findNavController().navigate(R.id.action_login_to_home)
        } catch (e: Exception) {
            Log.e("DEBUG_LOGIN", "Navigation FAILED: ${e.message}", e)
        }
    }

    private fun performApiLogin(inputNusnetId: String, inputPassword: String) {
        lifecycleScope.launch {
            try {
                binding.buttonSignIn.isEnabled = false
                binding.buttonSignIn.text = "Signing in..."

                val request = MobileLoginRequest(userid = inputNusnetId, password = inputPassword)
                val response = RetrofitClient.apiService.login(request)

                binding.buttonSignIn.isEnabled = true
                binding.buttonSignIn.text = "Sign In"

                if (response.success && response.data != null) {
                    handleLoginSuccess(response.data)
                } else {
                    val msg = "Code: ${response.code}, Success: ${response.success}, Data: ${response.data}"
                    Log.e("DEBUG_LOGIN", "Login failed checks: $msg")
                    Toast.makeText(requireContext(), "Debug: $msg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("DEBUG_LOGIN", "Login error: ${e.message}", e)
                binding.buttonSignIn.isEnabled = true
                binding.buttonSignIn.text = "Sign In"
                Toast.makeText(requireContext(), getLoginErrorMessage(e), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleLoginSuccess(loginData: com.ecogo.api.MobileLoginResponse) {
        val userInfo = loginData.userInfo
        Log.d("DEBUG_LOGIN", "Login successful: ${userInfo.nickname}")

        TokenManager.init(requireContext())
        TokenManager.saveToken(token = loginData.token, userId = userInfo.userid, username = userInfo.nickname)

        val prefs = requireContext().getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
        val isVip = userInfo.vip?.active == true
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putBoolean("is_vip", isVip)
            putString("nusnet_id", userInfo.userid)
            apply()
        }

        Toast.makeText(requireContext(), "Login Success! Navigating...", Toast.LENGTH_SHORT).show()
        findNavController().navigate(com.ecogo.R.id.action_login_to_home)
    }

    private fun getLoginErrorMessage(e: Exception): String {
        return when (e) {
            is retrofit2.HttpException -> when (e.code()) {
                401 -> "Invalid credentials"
                404 -> "User not found"
                500 -> "Server error"
                else -> "Network error: ${e.code()}"
            }
            is java.net.ConnectException -> "Cannot connect to server. Check your internet connection."
            else -> "Error: ${e.message}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
