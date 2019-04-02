package org.dashevo.dapidemo

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.*
import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.dapidemo.extensions.hide
import org.dashevo.dapidemo.extensions.show
import org.dashevo.dapidemo.model.MainViewModel

class LoginActivity : AppCompatActivity() {

    val balanceTxt: TextView by lazy { findViewById<TextView>(R.id.balance) }
    val addressTxt: TextView by lazy { findViewById<TextView>(R.id.address) }
    val bestChainHeightTxt: TextView by lazy { findViewById<TextView>(R.id.best_chain_height) }
    val blocksLeftTxt: TextView by lazy { findViewById<TextView>(R.id.blocks_left) }

    val loginBtn: Button by lazy { findViewById<Button>(R.id.loginBtn) }
    val username: EditText by lazy { findViewById<EditText>(R.id.username) }
    val progressBar: ProgressBar by lazy { findViewById<ProgressBar>(R.id.progressBar) }

    private lateinit var viewModel: MainViewModel

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        progressBar.hide()

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        viewModel.walletInfoLiveData.observe(this, Observer {
            balanceTxt.text = "balance: ${it?.balance?.toFriendlyString()}"
            addressTxt.text = "address: ${it?.currentReceiveAddress}"
            addressTxt.setOnClickListener { _ ->
                Log.d("address", "${it?.currentReceiveAddress}")
                val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.primaryClip = ClipData.newPlainText("Dash Address",
                        it?.currentReceiveAddress?.toBase58())
                Toast.makeText(this, "copied to clipboard", Toast.LENGTH_LONG).show()
            }
            viewModel.blockchainState.observe(this, Observer {
                bestChainHeightTxt.text = "bestChainHeight: ${it?.bestChainHeight}"
                blocksLeftTxt.text = "blocksLeft: ${it?.blocksLeft}"
            })
        })
        viewModel.djService.observe(this, Observer { djServiceLiveData ->
            Toast.makeText(this, if (djServiceLiveData != null) "Connected" else "Disconnected", Toast.LENGTH_LONG).show()
        })
        loginBtn.setOnClickListener {
            progressBar.show()
            viewModel.createUser(username.text.toString(), this)
        }
        viewModel.currentUser.observe(this, Observer<BlockchainUser> {
            if (it != null) {
                progressBar.hide()
                finish()
            }
        })
    }
}

fun ByteArray.toHexString(): String {
    return this.joinToString("") {
        java.lang.String.format("%02x", it)
    }
}