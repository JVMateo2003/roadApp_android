package com.jvmapp.roadapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsuario: EditText
    private lateinit var etContraseña: EditText
    private lateinit var btnIngresar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        initComponent()
        initListener()
    }

    private fun initComponent() {
        etUsuario = findViewById(R.id.etUsuario)
        etContraseña = findViewById(R.id.etContraseña)
        btnIngresar = findViewById(R.id.btnIngresar)
    }

    private fun initListener() {
        btnIngresar.setOnClickListener {

            val usuario = etUsuario.text.toString().trim()
            val contraseña = etContraseña.text.toString().trim()

            if (usuario.isEmpty() || contraseña.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contraseña != "1234") {



                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else {
                guardarUsuario(usuario)
                irHome()
            }

        }
    }

    private fun guardarUsuario(usuario: String) {
        val prefs = getSharedPreferences("roadapp_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("usuario", usuario)
            .apply()
    }

    private fun irHome() {
        val intent = Intent(this, HomeAvtivity::class.java)
        startActivity(intent)
        finish()
    }
}

