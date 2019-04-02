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
        const val DAPI_ID = "e72ad53cf7113788bf399b4e6df7b83726d24acde5109066fb829dd4065ee4d9"
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
                //Ignoring error for DashPay Contract not found
                createProfile(userRegTxId, userLastSubTx)
                //createContract(userRegTxId, userLastSubTx, ctx)
            }
        })
    }

    private fun createContract(userRegTxId: Sha256Hash, hashPrevSubTx: Sha256Hash, ctx: Context) {
        val dapSchema = JSONObject(ctx.readFromFile("dashpay_contract.json"))
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
        val userObj = Create.createDapObject("profile")
        userObj.put("bio", "Hey, I am $currentUsername, a DapiDemo User :D")
        userObj.put("bUserName", currentUsername)
        userObj.put("displayName", "I'm the real $currentUsername")

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

    fun createContactRequest(username: String, accept: Boolean, cb: DapiRequestCallback<String>) {
        val currentUser = this@MainViewModel.currentUser.value!!
        val contactObj = Create.createDapObject("contact")
        contactObj.put("action", if (accept) "accept" else "request")
        contactObj.put("from", currentUser.uname)
        //stub pub key
        contactObj.put("content", "tpubDDWsgxTGAT4Byi8KqV9QhnUwtS6DtkZooSpPkSi25LchNeFQYL58RYYePvBcXeaE7eH4KwP8BGYYCGL5DaYQTz3BcqXGvzk1PQTwe6ffT98")
        contactObj.put("relation", username)

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

    fun loadContacts() {
        if (currentUser.value != null) {
            dapiClient.fetchDapObjects(DAPI_ID, "contact", object : DapiRequestCallback<List<DapiDemoContact>> {
                override fun onSuccess(data: JsonRPCResponse<List<DapiDemoContact>>) {
                    val contacts = arrayListOf<DapiDemoContact>()
                    contacts.addAll(data.result!!)
                    val acceptedContacts = arrayListOf<String>()
                    acceptedContacts.addAll(contacts.map { it.from })
                    this@MainViewModel.contacts.postValue(contacts)

                    dapiClient.fetchDapObjects(DAPI_ID, "contact", object : DapiRequestCallback<List<DapiDemoContact>> {
                        override fun onSuccess(data: JsonRPCResponse<List<DapiDemoContact>>) {
                            contacts.addAll(data.result!!)
                            acceptedContacts.addAll(data.result!!.map { it.relation })

                            dapiClient.fetchDapObjects(DAPI_ID, "contact", object : DapiRequestCallback<List<DapiDemoContact>> {
                                override fun onSuccess(data: JsonRPCResponse<List<DapiDemoContact>>) {
                                    val pendingContacts = data.result!!.filter {
                                        it.relation !in acceptedContacts
                                    }

                                    this@MainViewModel.pendingContacts.postValue(pendingContacts)
                                    dapiClient.fetchDapObjects(DAPI_ID, "contact", object : DapiRequestCallback<List<DapiDemoContact>> {
                                        override fun onSuccess(data: JsonRPCResponse<List<DapiDemoContact>>) {
                                            val contactRequests = data.result!!.filter {
                                                it.from !in acceptedContacts
                                            }
                                            this@MainViewModel.contactRequests.postValue(contactRequests)
                                        }

                                        override fun onError(errorMessage: String) {

                                        }
                                    }, mapOf("data.relation" to currentUser.value!!.uname, "data.action" to "request"))
                                }

                                override fun onError(errorMessage: String) {

                                }
                            }, mapOf("data.from" to currentUser.value!!.uname, "data.action" to "request"))
                        }

                        override fun onError(errorMessage: String) {

                        }
                    }, mapOf("data.from" to currentUser.value!!.uname, "data.action" to "accept"))
                }

                override fun onError(errorMessage: String) {

                }
            }, mapOf("data.relation" to currentUser.value!!.uname, "data.action" to "accept"))
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
