package org.dashevo.dapidemo.dapi

import android.util.Log
import com.google.gson.Gson
import org.dashevo.dapiclient.DapiClient
import org.dashevo.dapiclient.callback.*
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

object DapiDemoClient : DapiClient("192.168.0.5", "8080") {

    const val CONTACT = "contact"
    private const val DAPI_DEMO_DAP_ID = "8a0e3721aef3b07607642099de6d55894fe49e3b6c48ddb26a7cbc6c2ca947ac"

    val gson = Gson()

    var dapSchema: JSONObject = JSONObject()
    var contacts =  arrayListOf<Contact>()
    var contactRequests = arrayListOf<Contact>()
    var pendingContacts = arrayListOf<Contact>()
    var onContactsUpdated: OnContactsUpdated? = null

    fun initDap(cb: DapCallback) {
        getDap(DAPI_DEMO_DAP_ID, object : DapCallback {
            override fun onSuccess(dapId: String) {
                cb.onSuccess(dapId)
            }

            override fun onError(errorMessage: String) {
                createDap(dapSchema, currentUser!!.buid, cb)
            }
        })
    }

    fun loginOrCreateUser(username: String, cb: LoginCallback) {
        login(username, object : LoginCallback {
            override fun onSuccess(blockchainUser: BlockchainUser) {
                cb.onSuccess(blockchainUser)
            }

            override fun onError(errorMessage: String) {
                val fakePubKey = HashUtils.toHash(JSONObject(mapOf("username" to username)))
                createUser(username, fakePubKey, object : CreateUserCallback {
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

    //TODO: DapiClient separate DapSpace/DapContext callbacks
    fun getDapSpaceOrSignUp(aboutMe: String, cb: GetDapSpaceCallback) {
        getDapSpace(object : GetDapSpaceCallback{
            override fun onSuccess(dapSpace: DapSpace) {
                cb.onSuccess(dapSpace)
            }

            override fun onError(errorMessage: String) {
                val obj = Create.createDapObject("user")
                obj.put("aboutme", aboutMe)
                obj.put("avatar", "b") //TODO
                commitSingleObject(obj, object : CommitDapObjectCallback {
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

    override fun getDapContext(cb: GetDapContextCallback) {
        super.getDapContext(object : GetDapContextCallback {
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

    private fun getContactUsers(ids: ArrayList<String>, dapContext: DapContext, cb: GetDapContextCallback) {
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
            getUser(userId, object : GetUserCallback {
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

    fun addContact(user: BlockchainUser, cb: CommitDapObjectCallback) {
        val obj= Create.createDapObject("contact")
        obj.put("hdextpubkey", user.pubkey)
        val userObj = JSONObject(mapOf(
                "userId" to user.buid,
                "type" to 0
        ))
        obj.put("me", currentUser!!.buid)
        obj.put("user", userObj)
        addObject(obj, cb)
    }

    fun addContact(userId: String, cb: CommitDapObjectCallback) {
        getUser(userId, object : GetUserCallback {
            override fun onSuccess(blockchainUserContainer: BlockchainUserContainer) {
                addContact(blockchainUserContainer.blockchainuser, cb)
            }

            override fun onError(errorMessage: String) {
                cb.onError(errorMessage)
            }
        })
    }

    fun removeContact(userId: String, cb: CommitDapObjectCallback) {
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
