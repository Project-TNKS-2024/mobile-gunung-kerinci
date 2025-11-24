package com.dicoding.gunungkerinci.Login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.Profile.ProfileFragment
import com.dicoding.gunungkerinci.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleLogin()
        setupButtons()
    }

    // GOOGLE SIGN-IN SETUP
    private fun setupGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            //.requestIdToken(getString(R.string.defaul_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    // BUTTON LISTENER
    private fun setupButtons() {

        // Login Manual
        binding.buttonMasuk.setOnClickListener { validateManualLogin() }

        // Login dengan Google
        binding.buttonGoogle.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener{
                signInWithGoogle()
            }
        }

        // Pindah ke Register
        binding.buttonRegisNow.setOnClickListener{
            startActivity(Intent(this, RegistrationActivity::class.java))
            finish()
        }

        binding.buttonForgetPass.setOnClickListener {
            startActivity(Intent(this, ForgetEmailActivity::class.java))
            finish()
        }

    }

    // GOOGLE LOGIN FLOW
    private fun signInWithGoogle() {
        val intent = googleSignInClient.signInIntent
        startActivityForResult(intent, RC_SIGN_IN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleGoogleLogin(task)
        }
    }

    private fun handleGoogleLogin(task: com.google.android.gms.tasks.Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)

            val email = account?.email ?: "unknown@gmail.com"
            val name = account?.displayName ?: "Pengguna"

            Toast.makeText(this, "Login Google Berhasil", Toast.LENGTH_SHORT).show()

            // Masuk ke halaman profile
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("email", email)
            intent.putExtra("name", name)
            intent.putExtra("open_fragment", "profile")
            startActivity(intent)
            finish()

        } catch (e: ApiException) {
            Toast.makeText(this, "Login Google gagal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // MANUAL LOGIN
    private fun validateManualLogin() {

        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty()) {
            binding.emailEditText.error = "Email harus diisi"
            binding.emailEditText.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Format email salah"
            binding.emailEditText.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.passwordEditText.error = "Password harus diisi"
            binding.passwordEditText.requestFocus()
            return
        }

        if (password.length < 8) {
            binding.passwordEditText.error = "Password diisi minimal 8 karakter"
            binding.passwordEditText.requestFocus()
            return
        }

        Toast.makeText(this, "Login Berhasil", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("email", email)
        intent.putExtra("open_fragment", "profile")
        startActivity(intent)
        finish()

    }
}