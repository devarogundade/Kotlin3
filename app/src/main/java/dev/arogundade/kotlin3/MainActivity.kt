package dev.arogundade.kotlin3

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import dev.arogundade.kotlin3.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.walletconnect.Session
import org.walletconnect.nullOnThrow

@SuppressLint("SetTextI18n")
class MainActivity : Activity(), Session.Callback {

    private var txRequest: Long? = null
    private val uiScope = CoroutineScope(Dispatchers.Main)

    private lateinit var binding: ActivityMainBinding

    override fun onStatus(status: Session.Status) {
        when (status) {
            Session.Status.Approved -> sessionApproved()
            Session.Status.Closed -> sessionClosed()
            Session.Status.Connected,
            Session.Status.Disconnected,
            is Session.Status.Error -> {

            }
        }
    }

    override fun onMethodCall(call: Session.MethodCall) {
        Log.d("TAG", "onMethodCall: $call")
    }

    private fun sessionApproved() {
        uiScope.launch {
            binding.apply {
                screenMainStatus.text =
                    "Connected: ${ExampleApplication.session.approvedAccounts()}"
                Log.d("TAG", "sessionApproved: ${ExampleApplication.session.approvedAccounts()}")
                screenMainConnectButton.visibility = View.GONE
                screenMainDisconnectButton.visibility = View.VISIBLE
                screenMainTxButton.visibility = View.VISIBLE
            }
        }
    }

    private fun sessionClosed() {
        uiScope.launch {
            binding.apply {
                screenMainStatus.text = "Disconnected"
                screenMainConnectButton.visibility = View.VISIBLE
                screenMainDisconnectButton.visibility = View.GONE
                screenMainTxButton.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        initialSetup()
        binding.screenMainConnectButton.setOnClickListener {
            ExampleApplication.resetSession()
            ExampleApplication.session.addCallback(this)
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(ExampleApplication.config.toWCUri())
            startActivity(i)
        }
        binding.screenMainDisconnectButton.setOnClickListener {
            ExampleApplication.session.kill()
        }
        binding.screenMainTxButton.setOnClickListener {
            val from = ExampleApplication.session.approvedAccounts()?.first()
                ?: return@setOnClickListener
            val txRequest = System.currentTimeMillis()
            ExampleApplication.session.performMethodCall(
                Session.MethodCall.SendTransaction(
                    txRequest,
                    from,
                    "0x9E47A9f1843Ebd9339C53E0732FbD540A2Ea43AC",
                    null,
                    null,
                    null,
                    "0x5AF3107A4000",
                    ""
                ),
                ::handleResponse
            )
            this.txRequest = txRequest
        }
    }

    private fun initialSetup() {
        val session = nullOnThrow { ExampleApplication.session } ?: return
        session.addCallback(this)
        sessionApproved()
    }

    private fun handleResponse(resp: Session.MethodCall.Response) {
        if (resp.id == txRequest) {
            txRequest = null
            uiScope.launch {
                binding.screenMainResponse.apply {
                    visibility = View.VISIBLE
                    text = "Last response: " + ((resp.result as? String) ?: "Unknown response")
                }
            }
        }
    }

    override fun onDestroy() {
        ExampleApplication.session.removeCallback(this)
        super.onDestroy()
    }
}