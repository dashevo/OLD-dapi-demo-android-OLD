package org.dashevo.dapidemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import org.dashevo.dapiclient.callback.LoginCallback
import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.dapidemo.dapi.DapiDemoClient
import org.dashevo.dapidemo.extensions.hide
import org.dashevo.dapidemo.extensions.show

class LoginActivity : AppCompatActivity() {

    val loginBtn: Button by lazy { findViewById<Button>(R.id.loginBtn) }
    val username: EditText by lazy { findViewById<EditText>(R.id.username) }
    val progressBar: ProgressBar by lazy { findViewById<ProgressBar>(R.id.progressBar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        progressBar.hide()

        loginBtn.setOnClickListener {
            progressBar.show()
            DapiDemoClient.loginOrCreateUser(username.text.toString(), object : LoginCallback {
                override fun onSuccess(blockchainUser: BlockchainUser) {
                    progressBar.hide()
                    finish()
                }

                override fun onError(errorMessage: String) {
                    progressBar.hide()
                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

}