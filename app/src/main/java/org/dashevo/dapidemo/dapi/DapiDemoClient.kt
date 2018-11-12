package org.dashevo.dapidemo.dapi

import android.util.Log
import com.google.gson.Gson
import org.dashevo.dapiclient.DapiClient
import org.dashevo.dapiclient.callback.BaseCallback
import org.dashevo.dapiclient.callback.BaseTxCallback
import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.dapiclient.model.BlockchainUserContainer
import org.dashevo.dapiclient.model.DapContext
import org.dashevo.dapiclient.model.DapSpace
import org.dashevo.dapidemo.model.Contact
import org.dashevo.schema.Create
import org.dashevo.schema.Object
import org.dashevo.schema.util.HashUtils
import org.jsonorg.JSONObject
import java.util.*

object DapiDemoClient : DapiClient("127.0.0.1", "8080") {

    const val CONTACT = "contact"
    private const val DAPI_DEMO_DAP_ID = "c78a05c06876a61a3942c2e5618ceec0a51e301b2b708f908165a2c00ca32cb8"

    val gson = Gson()

    var dapSchema: JSONObject = JSONObject()
    var contacts =  arrayListOf<Contact>()
    var contactRequests = arrayListOf<Contact>()
    var pendingContacts = arrayListOf<Contact>()
    var onContactsUpdated: OnContactsUpdated? = null

    fun initDap(cb: BaseCallback<String>) {
        getDap(DAPI_DEMO_DAP_ID, object : BaseCallback<String> {
            override fun onSuccess(dapId: String) {
                cb.onSuccess(dapId)
            }

            override fun onError(errorMessage: String) {
                createDap(dapSchema, currentUser!!.buid, cb)
            }
        })
    }

    fun loginOrCreateUser(username: String, cb: BaseCallback<BlockchainUser>) {
        login(username, object : BaseCallback<BlockchainUser> {
            override fun onSuccess(blockchainUser: BlockchainUser) {
                cb.onSuccess(blockchainUser)
            }

            override fun onError(errorMessage: String) {
                val fakePubKey = HashUtils.toHash(JSONObject(mapOf("username" to username)))
                createUser(username, fakePubKey, object : BaseTxCallback<String, String> {
                    override fun onSuccess(userId: String, txId: String) {
                        loginOrCreateUser(username, cb)
                    }

                    override fun onError(errorMessage: String) {
                        Log.e("DAP", "Failed to create blockchain user: $errorMessage")
                    }

                })
            }
        })
    }

    fun getDapSpaceOrSignUp(aboutMe: String, cb: BaseCallback<DapSpace>) {
        getDapSpace(object : BaseCallback<DapSpace> {
            override fun onSuccess(dapSpace: DapSpace) {
                cb.onSuccess(dapSpace)
            }

            override fun onError(errorMessage: String) {
                val obj = Create.createDapObject("user")
                obj.put("aboutme", aboutMe)
                commitSingleObject(obj, object : BaseTxCallback<String, String> {
                    override fun onSuccess(dapId: String, txId: String) {
                        getDapSpaceOrSignUp(aboutMe, cb)
                    }

                    override fun onError(errorMessage: String) {
                        cb.onError(errorMessage)
                    }
                })
            }
        })
    }

    override fun getDapContext(cb: BaseCallback<DapContext>) {
        super.getDapContext(object : BaseCallback<DapContext> {
            override fun onSuccess(dapContext: DapContext) {
                contacts.clear()
                pendingContacts.clear()
                contactRequests.clear()

                val userIds = arrayListOf<String>()

                dapContext.objects?.forEach {
                    if (it[Object.OBJTYPE] == CONTACT) {
                        val contact: Contact = gson.fromJson(JSONObject(it).toString(), Contact::class.java)
                        userIds.add(contact.user.userId)
                        pendingContacts.add(contact)
                    }
                }

                dapContext.related?.forEach {
                    if (it[Object.OBJTYPE] == CONTACT) {
                        val contact: Contact = gson.fromJson(JSONObject(it).toString(), Contact::class.java)
                        userIds.add(contact.user.userId)
                        contactRequests.add(contact)
                    }
                }

                contactRequests.removeAll(contacts)
                pendingContacts.removeAll(contacts)

                getContactUsers(userIds, dapContext, cb)
            }

            override fun onError(errorMessage: String) {
                cb.onError(errorMessage)
            }
        })
    }

    private fun getContactUsers(ids: ArrayList<String>, dapContext: DapContext, cb: BaseCallback<DapContext>) {
        if (ids.isEmpty()) {
            val iterator = pendingContacts.iterator()
            while (iterator.hasNext()) {
                val pending = iterator.next()
                contactRequests.forEach { request ->
                    if (pending.blockchainUser?.buid == request.blockchainUser?.buid) {
                        contacts.add(pending)
                    }
                }
            }

            contacts.forEach { contact ->
                contactRequests.remove(
                        contactRequests.firstOrNull {
                            it.blockchainUser?.buid == contact.blockchainUser?.buid
                        }
                )
                pendingContacts.remove(
                        pendingContacts.firstOrNull {
                            it.blockchainUser?.buid == contact.blockchainUser?.buid
                        }
                )
            }

            cb.onSuccess(dapContext)
            onContactsUpdated?.onContactsUpdated()
        } else {
            val userId = ids[ids.size - 1]
            ids.remove(userId)
            getUser(userId, object : BaseCallback<BlockchainUserContainer> {
                override fun onSuccess(blockchainUserContainer: BlockchainUserContainer) {
                    val contact: Contact? = contacts.firstOrNull { it.meta?.buid ?: it.user.userId == userId }
                    val pendingContact: Contact? = pendingContacts.firstOrNull { it.meta?.buid ?: it.user.userId == userId }
                    val contactRequest: Contact? = contactRequests.firstOrNull { it.meta?.buid ?: it.user.userId == userId }

                    contact?.blockchainUser = blockchainUserContainer.blockchainuser
                    pendingContact?.blockchainUser = blockchainUserContainer.blockchainuser
                    contactRequest?.blockchainUser = blockchainUserContainer.blockchainuser

                    getContactUsers(ids, dapContext, cb)
                }

                override fun onError(errorMessage: String) {
                    cb.onError(errorMessage)
                }
            })
        }
    }

    fun addContact(user: BlockchainUser, cb: BaseTxCallback<String, String>) {
        val obj = Create.createDapObject("contact")
        val userObj = JSONObject(mapOf(
                "userId" to user.buid,
                "type" to 0
        ))
        //Fake signature, just to avoid two packets having the same id
        val sig = HashUtils.toHash(JSONObject(gson.toJson(currentUser)))
        Object.setMeta(obj, "sig", sig)

        obj.put("user", userObj)
        addObject(obj, cb)
    }

    fun addContact(userId: String, cb: BaseTxCallback<String, String>) {
        getUser(userId, object : BaseCallback<BlockchainUserContainer> {
            override fun onSuccess(blockchainUserContainer: BlockchainUserContainer) {
                addContact(blockchainUserContainer.blockchainuser, cb)
            }

            override fun onError(errorMessage: String) {
                cb.onError(errorMessage)
            }
        })
    }

    fun removeContact(userId: String, cb: BaseTxCallback<String, String>) {
        val contactsMerge = ArrayList<Contact>(contacts)
        contactsMerge.addAll(pendingContacts)

        val contactToRemove = contactsMerge.firstOrNull {
            it.user.userId == userId
        }

        if (contactToRemove != null) {
            removeObject(JSONObject(gson.toJson(contactToRemove)), cb)
        } else {
            cb.onError("Contact not found")
        }
    }

    interface OnContactsUpdated {
        fun onContactsUpdated()
    }

}
