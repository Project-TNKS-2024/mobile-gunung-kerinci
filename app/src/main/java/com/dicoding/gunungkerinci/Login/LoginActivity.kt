package com.dicoding.gunungkerinci.Login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.defaul_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)

        binding.buttonMasuk.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            //Jika email kosong
            if(email.isEmpty()) {
                binding.emailEditText.error = "Email harus diisi"
                binding.emailEditText.requestFocus()
                return@setOnClickListener
            }

            //Jika email tidak sesuai
            if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailEditText.error = "Email tidak sesuai"
                binding.emailEditText.requestFocus()
                return@setOnClickListener
            }

            //Jika password kosong
            if(password.isEmpty()) {
                binding.passwordEditText.error = "Password harus diisi"
                binding.passwordEditText.requestFocus()
                return@setOnClickListener
            }

            //Validasi panjang password
            if (password.length < 8) {
                binding.passwordEditText.error = "Password minimal 8 karater"
                binding.passwordEditText.requestFocus()
                return@setOnClickListener
            }
            
            LoginFirebase(email, password)
        }

        binding.buttonGoogle.setOnClickListener {

        }

        binding.buttonLanguage.setOnClickListener {

            //finish()
        }

        binding.buttonRegisNow.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.buttonForgetPass.setOnClickListener {
            val intent = Intent(this, ForgetEmailActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun LoginFirebase(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) {
                if (it.isSuccessful) {
                    Toast.makeText(this,"Login berhasil", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "${it.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            //MENANGANI PROSES LOGIN GOOGLE
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                //Jika berhasil
                val  account = task.getResult(ApiException::class.java)
                val idToken = account.idToken

                // Kirim idToken ke backend API kamu jika ada
                // Atau lanjutkan proses login di app
                Toast.makeText(this, "Login Berhasil: ${account.email}", Toast.LENGTH_SHORT).show()

            } catch (e: ApiException) {
                Toast.makeText(this, "Login Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}