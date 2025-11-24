package com.dicoding.gunungkerinci.Login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
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
import javax.xml.validation.Validator

class RegistrationActivity : AppCompatActivity() {

    private  lateinit var binding: ActivityRegistrationBinding
    private  lateinit var googleSignInClient: GoogleSignInClient

    private val RC_SIGN_IN = 1001
    private var isFisrstGoogleLogin = true //Mencegah toast muncul 2x

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleLogin()
        setupButton()

        //Reset keadaan setiap halaman dibuka ulang
        isFisrstGoogleLogin = true
    }

    // GOOGLE SIGN-IN SETUP
    private fun setupGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    //BUTTON ACTIONS
    private fun setupButton() {

        //Button Daftar Manual
        binding.buttonRegis.setOnClickListener {
            validateManualRegistration()
        }

        //Masuk dengan Google
        binding.buttonGoogle.setOnClickListener {
        //Paksa sign-out supaya akun Google muncul ulang
            googleSignInClient.signOut().addOnCompleteListener{
                signInWithGoogle()
            }
        }

        //Button Masuk Sekarang
        binding.buttonLogNow.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

    }

    //G GOOGLE SIGN-IN FLOW
    private fun signInWithGoogle() {
        val intent = googleSignInClient.signInIntent
        startActivityForResult(intent, RC_SIGN_IN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java) ?: return

            val email = account.email
            val name = account.displayName

            // Hanya tampilkan toast saat login pertama kali
            if (isFisrstGoogleLogin) {
                Toast.makeText(this, "Registrasi berhasil: $email", Toast.LENGTH_SHORT).show()
                isFisrstGoogleLogin = false
            }

            // Pindah ke halaman verifikasi
            val intent = Intent(this, VerificationActivity::class.java)
            intent.putExtra("email", email)
            intent.putExtra("name", name)
            startActivity(intent)

        } catch (e: ApiException) {
            Toast.makeText(this, "Registrasi gagal: ${e.message}", Toast.LENGTH_SHORT).show()
        }

    }

    // VALIDASI REGISTRASI MANUAL
    private fun validateManualRegistration() {
        val email = binding.emailEditText.text.toString().trim()
        val pass = binding.passwordEditText.text.toString().trim()
        val pass2 = binding.password2EditText.text.toString().trim()

        //validasi email kosong
        if (email.isEmpty()) {
            binding.emailEditText.error = "Email harus diisi"
            binding.emailEditText.requestFocus()
            return
        }
        //Validasi email salah
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Email tidak sesuai"
            binding.emailEditText.requestFocus()
            return
        }

        //Password Kosong
        if (pass.isEmpty()) {
            binding.passwordEditText.error = " Password harus diisi"
            binding.passwordEditText.requestFocus()
            return
        }
        //Password minimal 8 karakter
        if (pass.length < 8) {
            binding.passwordEditText.error = "Password diisi minimal 8 karakter"
            binding.passwordEditText.requestFocus()
            return
        }

        //Password ulang tidak cocok
        if (pass != pass2) {
            binding.password2EditText.error = "Password tidak sesuai"
            binding.password2EditText.requestFocus()
            return
        }

        //Pindah ke halaman verifikasi
        val intent = Intent(this, VerificationActivity::class.java)
        intent.putExtra("email", email)
        startActivity(intent)

    }


}