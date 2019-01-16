package org.dashevo.dapidemo.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import org.dashevo.dapiclient.callback.CommitDapObjectCallback
import org.dashevo.dapiclient.callback.GetDapContextCallback
import org.dashevo.dapiclient.model.DapContext
import org.dashevo.dapidemo.R
import org.dashevo.dapidemo.adapter.ContactRequestsAdapter
import org.dashevo.dapidemo.adapter.ContactsAdapter
import org.dashevo.dapidemo.adapter.ContactsAdapterImpl
import org.dashevo.dapidemo.dapi.DapiDemoClient
import org.dashevo.dapidemo.extensions.hide
import org.dashevo.dapidemo.extensions.show
import org.dashevo.dapidemo.model.Contact

class ContactsFragment : Fragment() {

    enum class Type(val title: String) {
        CONTACTS("Contacts"),
        PENDING("Pending"),
        REQUESTS("Requests")
    }

    companion object {

        private const val FRAGMENT_TYPE = "fragmentType"

        fun newInstance(type: Type): ContactsFragment {
            val args = Bundle()
            args.putSerializable(FRAGMENT_TYPE, type)

            val fragment = ContactsFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private val fragmentType by lazy { arguments?.getSerializable(FRAGMENT_TYPE) as Type }
    private val contactsRv by lazy { view!!.findViewById<RecyclerView>(R.id.contactsRv) }
    private val progressBar by lazy { view!!.findViewById<ProgressBar>(R.id.progressBar) }
    private var adapter: ContactsAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contactsRv.layoutManager = LinearLayoutManager(activity)
        contactsRv.adapter = when (fragmentType) {
            Type.CONTACTS -> {
                val result = ContactsAdapterImpl()
                result.itemClickListener = contactsItemClickListener
                adapter = result
                result
            }
            Type.PENDING -> {
                val result = ContactsAdapterImpl()
                result.itemClickListener = contactsItemClickListener
                adapter = result
                result
            }
            Type.REQUESTS -> {
                val result = ContactRequestsAdapter()
                result.itemClickListener = contactRequestsItemClickListener
                adapter = result
                result
            }
        }

        DapiDemoClient.onContactsUpdated = object : DapiDemoClient.OnContactsUpdated {
            override fun onContactsUpdated() {
                updateAdapter()
            }
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            updateAdapter()
        }
    }

    fun updateAdapter() {
        adapter?.contacts = when (fragmentType) {
            Type.CONTACTS -> DapiDemoClient.contacts
            Type.PENDING -> DapiDemoClient.pendingContacts
            Type.REQUESTS -> DapiDemoClient.contactRequests
        }
    }

    private val contactsItemClickListener = object : ContactsAdapterImpl.OnItemClickListener {
        override fun onRemoveClicked(contact: Contact) {
            progressBar.show()
            DapiDemoClient.removeContact(contact.user.userId, object : CommitDapObjectCallback {
                override fun onSuccess(dapId: String, txId: String) {
                    adapter?.remove(contact)
                    progressBar.hide()
                }

                override fun onError(errorMessage: String) {
                    Toast.makeText(activity, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    progressBar.hide()
                }
            })
        }
    }

    private val contactRequestsItemClickListener = object : ContactRequestsAdapter.OnItemClickListener {
        override fun onAcceptClicked(userId: String) {
            progressBar.show()
            DapiDemoClient.addContact(userId, object : CommitDapObjectCallback {
                override fun onSuccess(dapId: String, txId: String) {
                    DapiDemoClient.getDapContext(object : GetDapContextCallback {
                        override fun onSuccess(dapContext: DapContext) {
                            updateAdapter()
                            progressBar.hide()
                        }

                        override fun onError(errorMessage: String) {
                            Toast.makeText(activity,
                                    "Add success, failed to load Dap Context: $errorMessage",
                                    Toast.LENGTH_SHORT).show()
                            progressBar.hide()
                        }
                    })
                }

                override fun onError(errorMessage: String) {
                    Toast.makeText(activity, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    progressBar.hide()
                }
            })
        }
    }

}
