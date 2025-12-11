package hu.ait.walletify.ui.plaid
//
//import android.os.Bundle
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.compose.runtime.remember
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.google.android.material.button.MaterialButton
//import com.plaid.link.FastOpenPlaidLink
//import com.plaid.link.Plaid
//import com.plaid.link.PlaidHandler
//import com.plaid.link.configuration.LinkTokenConfiguration
//import com.plaid.link.linkTokenConfiguration
//import com.plaid.link.result.LinkExit
//import com.plaid.link.result.LinkResultHandler
//import com.plaid.link.result.LinkSuccess
//import hu.ait.walletify.R
//
//class LinkActivity : AppCompatActivity() {
//
//    private lateinit var result: TextView
//    private lateinit var tokenResult: TextView
//    private lateinit var prepareButton: MaterialButton
//    private lateinit var openButton: MaterialButton
//    private var plaidHandler: PlaidHandler? = null
//
//    private val linkAccountToPlaid = registerForActivityResult(FastOpenPlaidLink()) { result ->
//        when (result) {
//            is LinkSuccess -> showSuccess(result)
//            is LinkExit -> showFailure(result)
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        result = findViewById(R.id.result)
//        tokenResult = findViewById(R.id.public_token_result)
//
//        prepareButton = findViewById(R.id.prepare_link)
//        prepareButton.setOnClickListener {
//            prepareLink()
//        }
//
//        openButton = findViewById(R.id.open_link)
//        openButton.setOnClickListener {
//            openLink()
//        }
//    }
//
//    private fun prepareLink() {
//        LinkTokenRequester.token.subscribe(::onLinkTokenSuccess, ::onLinkTokenError)
//    }
//
//
//    /**
//     * For all Link configuration options, have a look at the
//     * [parameter reference](https://plaid.com/docs/link/android/#parameter-reference).
//     */
//    private fun openLink() {
//        prepareButton.isEnabled = true
//        openButton.isEnabled = false
//        plaidHandler?.let { linkAccountToPlaid.launch(it) }
//    }
//
//    private fun onLinkTokenSuccess(linkToken: String) {
//        prepareButton.isEnabled = false
//        openButton.isEnabled = true
//        val tokenConfiguration = LinkTokenConfiguration.Builder()
//            .token(linkToken)
//            .build()
//        plaidHandler = Plaid.create(this.application, tokenConfiguration)
//    }
//
//    private fun onLinkTokenError(error: Throwable) {
//        if (error is java.net.ConnectException) {
//            Toast.makeText(this, "Please run `sh start_server.sh <client_id> <sandbox_secret>`", Toast.LENGTH_LONG).show()
//            return
//        }
//        Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
//    }
//
//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.menu, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean =
//        when (item.itemId) {
//            R.id.show_java -> {
//                val intent = Intent(this@MainActivity, MainActivityJava::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                startActivity(intent)
//                true
//            }
//            R.id.show_activity_result -> {
//                val intent = Intent(this@MainActivity, MainActivityStartActivityForResult::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                startActivity(intent)
//                true
//            }
//            R.id.show_activity_result_java -> {
//                val intent = Intent(this@MainActivity, MainActivityStartActivityForResultJava::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                startActivity(intent)
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//
//    private fun showSuccess(success: LinkSuccess) {
//        tokenResult.text = getString(R.string.public_token_result, success.publicToken)
//        result.text = getString(R.string.content_success)
//    }
//
//    private fun showFailure(exit: LinkExit) {
//        tokenResult.text = ""
//        if (exit.error != null) {
//            result.text = getString(R.string.content_exit, exit.error?.displayMessage, exit.error?.errorCode)
//        } else {
//            result.text = getString(R.string.content_cancel, exit.metadata.status?.jsonValue ?: "unknown")
//        }
//    }
//}
////class LinkActivity: AppCompatActivity(
////) {
////
////    private lateinit var result: TextView
////    private lateinit var tokenResult: TextView
////    private lateinit var prepareButton: MaterialButton
////    private lateinit var openButton: MaterialButton
////    private var plaidHandler: PlaidHandler? = null
////
////    private val linkAccountToPlaid =
////        registerForActivityResult(FastOpenPlaidLink()) {
////            when (it) {
////                is LinkSuccess -> {}/* handle LinkSuccess */
////                is LinkExit -> {}/* handle LinkExit */
////            }
////        }
////
////    val plaidHandler2: PlaidHandler =
////        Plaid.create(application, linkTokenConfiguration)
//////    linkAccountToPlaid.launch(plaidHandler)
////    plaidHandler?.let { linkAccountToPlaid.launch(it) }
////
////
////    private val myPlaidResultHandler by lazy {
////        LinkResultHandler(
////            onSuccess = {
////                tokenResult.text = getString(R.string.public_token_result, it.publicToken)
////                result.text = getString(R.string.content_success)
////            },
////            onExit = {
////                tokenResult.text = ""
////                if (it.error != null) {
////                    result.text = getString(
////                        R.string.content_exit,
////                        )
////                } else {
////                    result.text = getString(
////                        R.string.content_cancel,
////                        it.metadata.status?.jsonValue ?: "unknown"
////                    )
////                }
////            }
////        )
////    }
////
////    val resultHandler = LinkResultHandler(
////        onSuccess = {succeeded: LinkSuccess ->
////            // Extract public token from successful link
////            viewModel.exchangePublicToken(succeeded.publicToken)
////        },
////
////        onExit =  { exit: LinkExit ->
////            // Handle user exit or error
////            if (exit.error != null) {
////                Toast.makeText(
////                    context,
////                    "Link error: ${exit.error?.displayMessage}",
////                    Toast.LENGTH_LONG
////                ).show()
////            } else {
////                Toast.makeText(
////                    context,
////                    "Link cancelled",
////                    Toast.LENGTH_SHORT
////                ).show()
////            }
////            viewModel.resetState()
////        }
////    )
////
////
////}