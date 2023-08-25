package com.a.hykimeapp

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.a.hykimeapp.databinding.PrefActivityBinding
import com.ime.hyk.HykIme
import com.ime.hyk.setup.SetupActivity

class HykMainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = PrefActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setup()
        binding.btnSetup.setOnClickListener { setup() }
        binding.btnDeploy.setOnClickListener { deploy() }
    }


    private fun setup() {
        if (SetupActivity.shouldSetup()) {
            startActivity(Intent(this, SetupActivity::class.java))
        }
    }

    private fun deploy() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage(context.getText(R.string.deploy_progress))
            setCancelable(false)
        }
        progressDialog.show()
        HykIme.initKeyboardAndDeploy { progressDialog.dismiss() }

    }


}