package com.osfans.trime.settings

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.osfans.trime.R
import com.osfans.trime.databinding.PrefActivityBinding
import com.osfans.trime.ime.core.Trime
import com.osfans.trime.setup.SetupActivity
import com.osfans.trime.util.RimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber


class PrefMainActivity :
    AppCompatActivity(), CoroutineScope by MainScope() {

    lateinit var binding: PrefActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = PrefActivityBinding.inflate(layoutInflater)
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
        Trime.getServiceOrNull()?.initKeyboard()
        launch {
            try {
                RimeUtils.deploy(this@PrefMainActivity)
            } catch (ex: Exception) {
                Timber.e(ex, "Deploy Exception")
            } finally {
                progressDialog.dismiss()
            }
        }
    }


}
