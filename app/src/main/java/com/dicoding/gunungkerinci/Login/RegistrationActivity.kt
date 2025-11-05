package com.dicoding.gunungkerinci.Login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.dicoding.gunungkerinci.databinding.ActivityRegistrationBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Konfigurasi Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail() //minta email
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.buttonRegis.setOnClickListener {

            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val passulang = binding.password2EditText.text.toString().trim()


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

            // Validasi password ulang
            if (password != passulang) {
                binding.password2EditText.error = "Password tidak sesuai"
                binding.password2EditText.requestFocus()
                return@setOnClickListener
            }

            //Jika
            val intent = Intent(this, VerificationActivity::class.java)
            intent.putExtra("email", email)
            startActivity(intent)

        }

        binding.buttonLogNow.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            //finish()
        }

        binding.buttonLanguage.setOnClickListener {

            //finish()
        }

        binding.buttonGoogle.setOnClickListener {
            signInWithGoogle()
            //finish
        }

    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    //Fungsi handle hasil login
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                val email = account.email
                val name = account.displayName

                Toast.makeText(this, "Resigtrasi berhasil: $name", Toast.LENGTH_SHORT).show()

                //Lanjut ke VerificationActivity atau Dashboard
                val intent = Intent(this, VerificationActivity::class.java)
                intent.putExtra("email", email)
                intent.putExtra("name", name)
                startActivity(intent)
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Registrasi gagal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}