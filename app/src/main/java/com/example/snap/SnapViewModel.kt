package com.example.snap

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portto.solana.web3.Connection
import com.solana.mobilewalletadapter.clientlib.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.portto.solana.web3.PublicKey
import com.portto.solana.web3.SerializeConfig
import com.portto.solana.web3.Transaction
import com.portto.solana.web3.programs.MemoProgram
import com.portto.solana.web3.rpc.types.config.Commitment
import com.portto.solana.web3.util.Cluster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Base58

data class SnapViewState(
    val canTransact: Boolean = false,
    val userAddress: String = "",
    val userLabel: String = "",
    val authToken: String = "",
    val noWallet: Boolean = false,
)

class SnapViewModel: ViewModel() {

    private fun SnapViewState.updateViewState() {
        _state.update { this }
    }

    private val _state = MutableStateFlow(SnapViewState())

    val viewState: StateFlow<SnapViewState>
        get() = _state

    private val api by lazy { Connection(Cluster.DEVNET) }

    init {
        viewModelScope.launch {
            _state.value = SnapViewState()
        }
    }

    fun connect(
        identityUri: Uri,
        iconUri: Uri,
        identityName: String,
        activityResultSender: ActivityResultSender
    ) {
        viewModelScope.launch {
            val walletAdapterClient = MobileWalletAdapter()
            val result = walletAdapterClient.transact(activityResultSender) {
                authorize(
                    identityUri = identityUri,
                    iconUri = iconUri,
                    identityName = identityName,
                    rpcCluster = RpcCluster.Devnet
                )
            }

            when (result) {
                is TransactionResult.Success -> {
                    _state.value.copy(
                        userAddress = PublicKey(result.payload.publicKey).toBase58(),
                        userLabel = result.payload.accountLabel ?: "",
                        authToken = result.payload.authToken,
                        canTransact = true
                    ).updateViewState()

                    Log.d(TAG, "connect: $result")
                }

                is TransactionResult.NoWalletFound -> {
                    _state.value.copy(
                        noWallet = true,
                        canTransact = false
                    ).updateViewState()
                }

                is TransactionResult.Failure -> {
                    _state.value.copy(
                        canTransact = false
                    ).updateViewState()
                }
            }
        }

    }

    fun disconnect() {
        viewModelScope.launch {
            _state.update {
                _state.value.copy(
                    userAddress = "",
                    userLabel = "",
                    authToken = "",
                    canTransact = false
                )
            }
        }
    }

    fun sign_message(
        identityUri: Uri,
        iconUri: Uri,
        identityName: String,
        activityResultSender: ActivityResultSender

    ){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val blockHash = api.getLatestBlockhash(Commitment.FINALIZED)

                val tx = Transaction()
                tx.add(MemoProgram.writeUtf8(PublicKey(_state.value.userAddress), "memoText"))
                tx.setRecentBlockHash(blockHash!!)
                tx.feePayer = PublicKey(_state.value.userAddress)

                val bytes = tx.serialize(SerializeConfig(requireAllSignatures = false))

                val walletAdapterClient = MobileWalletAdapter()
                val result = walletAdapterClient.transact(activityResultSender) {
                    reauthorize(identityUri, iconUri, identityName, _state.value.authToken)
                    signAndSendTransactions(arrayOf(bytes))
                }

                result.successPayload?.signatures?.firstOrNull()?.let { sig ->
                    val readableSig = Base58.encode(sig)
                    Log.d(TAG, "sign_message: https://explorer.solana.com/tx/$readableSig?cluster=devnet")
                }

            }
        }
    }
}