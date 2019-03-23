package org.dashevo.dapidemo.model

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.util.Log
import org.bitcoinj.core.*
import org.bitcoinj.governance.GovernanceObject
import org.bitcoinj.kits.EvolutionWalletAppKit
import org.dashevo.dapiclient.DapiClient
import org.dashevo.dapiclient.callback.DapiRequestCallback
import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.dapiclient.model.JsonRPCResponse
import org.dashevo.dapidemo.MainApplication
import org.dashevo.dapidemo.data.SingleLiveEvent
import org.dashevo.schema.Create
import org.dashj.dashjinterface.WalletAppKitService
import org.dashj.dashjinterface.data.*
import org.jsonorg.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

class MainViewModel(application: Application) : DjInterfaceViewModel(application) {

    companion object {
        const val DAPI_ID = "9ae7bb6e437218d8be36b04843f63a135491c898ff22d1ead73c43e105cc2444"
    }

    var userRegTx: Sha256Hash? = null

    private val _peerConnectivity = PeerConnectivityLiveData(application)
    val peerConnectivity: PeerConnectivityLiveData
        get() = _peerConnectivity

    private val _walletInfoLiveData = WalletInfoLiveData(application)
    val walletInfoLiveData: WalletInfoLiveData
        get() = _walletInfoLiveData

    private val _blockchainState = BlockchainStateLiveData(application)
    val blockchainState: BlockchainStateLiveData
        get() = _blockchainState

    private val _masternodes = MasternodesLiveData(application)
    val masternodes: LiveData<List<Masternode>>
        get() = _masternodes

    private val _governanceObjects = GovernanceLiveData(application)
    val governanceObjects: LiveData<List<GovernanceObject>>
        get() = _governanceObjects

    private val _masternodeSync = MasternodeSyncLiveData(application)
    val masternodeSync: MasternodeSyncLiveData
        get() = _masternodeSync

    private val _showMessageAction = SingleLiveEvent<Pair<Boolean, String>>()
    val showMessageAction
        get() = _showMessageAction

    var currentUsername: String? = null
    val currentUser = MutableLiveData<BlockchainUser>()
    val contacts = MutableLiveData<List<DapiDemoContact>>()
    val pendingContacts = MutableLiveData<List<DapiDemoContact>>()
    val contactRequests = MutableLiveData<List<DapiDemoContact>>()

    private val dapiClient by lazy { DapiClient("http://devnet-maithai.thephez.com", "3000") }

    fun sendFunds1(address: String, amount: Coin) {
        djService.value?.sendFunds(address, amount, object : WalletAppKitService.Result<Transaction> {

            override fun onSuccess(result: Transaction) {
                _showMessageAction.call(Pair(false, result.hashAsString))
            }

            override fun onFailure(ex: Exception) {
                _showMessageAction.call(Pair(true, ex.message!!))
            }
        })
    }

    fun createUser(username: String, ctx: Context) {
        if (userRegTx == null) {
            djService.value?.createUser(username, Coin.parseCoin("0.1"),
                    object : WalletAppKitService.Result<Transaction> {

                        override fun onSuccess(result: Transaction) {
                            userRegTx = result.hash
                            MainApplication.instance.saveUsername(username)
                            currentUsername = username
                            _showMessageAction.call(Pair(false, result.hashAsString))

                            Timer().schedule(object : TimerTask() {
                                override fun run() {
                                    checkDapExists(userRegTx!!, userRegTx!!, ctx)
                                }
                            }, 1000)
                        }

                        override fun onFailure(ex: Exception) {
                            _showMessageAction.call(Pair(true, ex.message!!))
                        }
                    })
        } else {
            checkDapExists(Sha256Hash.wrap(currentUser.value!!.regtxid),
                    Sha256Hash.wrap(currentUser.value!!.subtx.last()), ctx)
        }
    }

    fun getPrivKey(): ECKey {
        val wallet = djService.value?.wallet
        return ECKey.fromPrivate(wallet!!.activeKeyChain.getKeyByPath(
                EvolutionWalletAppKit.EVOLUTION_ACCOUNT_PATH, false).privKeyBytes)
    }

    private fun checkDapExists(userRegTxId: Sha256Hash, userLastSubTx: Sha256Hash, ctx: Context) {
        dapiClient.fetchDapContract(DAPI_ID, object : DapiRequestCallback<HashMap<String, Any>> {
            override fun onSuccess(data: JsonRPCResponse<HashMap<String, Any>>) {
                createProfile(userRegTxId, userLastSubTx)
            }

            override fun onError(errorMessage: String) {
                createContract(userRegTxId, userLastSubTx, ctx)
            }
        })
    }

    private fun createContract(userRegTxId: Sha256Hash, hashPrevSubTx: Sha256Hash, ctx: Context) {
        val dapSchema = JSONObject(ctx.readFromFile("dapi_demo_dap.json"))
        dapiClient.registerDap(dapSchema, userRegTxId, hashPrevSubTx, getPrivKey(), object : DapiRequestCallback<String> {
            override fun onSuccess(txId: JsonRPCResponse<String>) {
                createProfile(userRegTxId, Sha256Hash.wrap(txId.result))
            }

            override fun onError(errorMessage: String) {
                println(errorMessage)
            }
        })
    }

    private fun createProfile(userRegTxId: Sha256Hash, hashPrevSubTx: Sha256Hash) {
        val userObj = Create.createDapObject("user")
        userObj.put("bio", "Hey, I am $currentUsername, a DapiDemo User :D")
        userObj.put("displayName", currentUsername)

        dapiClient.sendDapObject(userObj, DAPI_ID, userRegTxId, hashPrevSubTx, getPrivKey(),
                object : DapiRequestCallback<String> {
                    override fun onSuccess(txId: JsonRPCResponse<String>) {
                        getUser(currentUsername!!, txId.result)
                    }

                    override fun onError(errorMessage: String) {
                        println(errorMessage)
                    }
                })
    }

    private fun getUser(currentUsername: String, stateTxId: String? = null) {
        dapiClient.getUser(currentUsername, object : DapiRequestCallback<BlockchainUser> {
            override fun onSuccess(data: JsonRPCResponse<BlockchainUser>) {
                if (stateTxId != null) {
                    data.result?.subtx?.add(stateTxId)
                }
                currentUser.value = data.result
            }

            override fun onError(errorMessage: String) {
                println(errorMessage)
            }
        })
    }

    fun createContactRequest(username: String, cb: DapiRequestCallback<String>) {
        dapiClient.getUser(username, object : DapiRequestCallback<BlockchainUser> {
            override fun onSuccess(data: JsonRPCResponse<BlockchainUser>) {
                val currentUser = this@MainViewModel.currentUser.value!!
                val contactObj = Create.createDapObject("contact")
                contactObj.put("user", data.result!!.regtxid)
                contactObj.put("username", username)

                val me = JSONObject()
                me.put("id", currentUser.regtxid)
                me.put("username", currentUser.uname)

                contactObj.put("sender", me)

                val userRegTxId = Sha256Hash.wrap(currentUser.regtxid)
                val lastSubTxHash = if (currentUser.subtx.isNotEmpty()) {
                    Sha256Hash.wrap(currentUser.subtx.last())
                } else {
                    userRegTxId
                }
                dapiClient.sendDapObject(contactObj, DAPI_ID, userRegTxId, lastSubTxHash, getPrivKey(),
                        object : DapiRequestCallback<String> {
                            override fun onSuccess(data: JsonRPCResponse<String>) {
                                currentUser.subtx.add(data.result!!)
                                this@MainViewModel.currentUser.postValue(currentUser)
                                cb.onSuccess(data)
                            }

                            override fun onError(errorMessage: String) {
                                cb.onError(errorMessage)
                            }
                        })
            }

            override fun onError(errorMessage: String) {
                println(errorMessage)
            }
        })
    }

    fun loadContacts() {
        if (currentUser.value != null) {
            //TODO: Callback hell
            dapiClient.fetchDapObjects(DAPI_ID, "contact",
                    object : DapiRequestCallback<List<DapiDemoContact>> {
                        override fun onSuccess(data: JsonRPCResponse<List<DapiDemoContact>>) {
                            val contactRequestsReceived = arrayListOf<DapiDemoContact>()
                            data.result?.let { contactRequestsReceived.addAll(it) }
                            dapiClient.fetchDapObjects(DAPI_ID, "contact",
                                    object : DapiRequestCallback<List<DapiDemoContact>> {
                                        override fun onSuccess(data: JsonRPCResponse<List<DapiDemoContact>>) {
                                            val contactRequestsSent = arrayListOf<DapiDemoContact>()
                                            data.result?.let { contactRequestsSent.addAll(it) }

                                            val contactRequestsReceivedUserIds = contactRequestsReceived.map {
                                                it.sender.id
                                            }
                                            val contacts = contactRequestsSent.filter {
                                                it.user in contactRequestsReceivedUserIds
                                            }
                                            val contactsIds = contacts.map { it.user }

                                            this@MainViewModel.contacts.postValue(contacts)
                                            this@MainViewModel.pendingContacts.postValue(contactRequestsSent.filter {
                                                it.user !in contactsIds
                                            })
                                            this@MainViewModel.contactRequests.postValue(contactRequestsReceived.filter {
                                                it.sender.id !in contactsIds
                                            })

                                        }

                                        override fun onError(errorMessage: String) {
                                            Log.d("Error", "Error $errorMessage")

                                        }
                                    }, mapOf("blockchainUserId" to currentUser.value!!.regtxid))
                        }

                        override fun onError(errorMessage: String) {
                            Log.d("Error", "Error $errorMessage")

                        }
                    }, mapOf("data.user" to currentUser.value!!.regtxid))
        }
    }
}

fun Context.readFromFile(fileName: String): String {
    val returnString = StringBuilder()
    var fIn: InputStream? = null
    var isr: InputStreamReader? = null
    var input: BufferedReader? = null
    try {
        fIn = resources.assets.open(fileName)
        isr = InputStreamReader(fIn)
        input = BufferedReader(isr)
        returnString.append(input.use { it.readText() })
    } catch (e: Exception) {
        e.message
    } finally {
        try {
            isr?.close()
            fIn?.close()
            input?.close()
        } catch (e2: Exception) {
            e2.message
        }

    }
    return returnString.toString()
}
