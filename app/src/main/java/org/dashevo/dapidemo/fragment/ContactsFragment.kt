package org.dashevo.dapidemo.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import org.dashevo.dapiclient.callback.DapiRequestCallback
import org.dashevo.dapiclient.model.JsonRPCResponse
import org.dashevo.dapidemo.R
import org.dashevo.dapidemo.adapter.ContactRequestsAdapter
import org.dashevo.dapidemo.adapter.ContactsAdapter
import org.dashevo.dapidemo.adapter.ContactsAdapterImpl
import org.dashevo.dapidemo.extensions.hide
import org.dashevo.dapidemo.extensions.show
import org.dashevo.dapidemo.model.DapiDemoContact
import org.dashevo.dapidemo.model.MainViewModel

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

    private lateinit var viewModel: MainViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)

        contactsRv.layoutManager = LinearLayoutManager(activity)
        contactsRv.adapter = when (fragmentType) {
            Type.CONTACTS -> {
                val result = ContactsAdapterImpl(viewModel)
                result.itemClickListener = contactsItemClickListener
                adapter = result
                result
            }
            Type.PENDING -> {
                val result = ContactsAdapterImpl(viewModel)
                result.itemClickListener = contactsItemClickListener
                adapter = result
                result
            }
            Type.REQUESTS -> {
                val result = ContactRequestsAdapter(viewModel)
                result.itemClickListener = contactRequestsItemClickListener
                adapter = result
                result
            }
        }

        when (fragmentType) {
            Type.CONTACTS -> {
                viewModel.contacts.observe(this, Observer { updateAdapter() })
            }
            Type.PENDING -> {
                viewModel.pendingContacts.observe(this, Observer { updateAdapter() })
            }
            Type.REQUESTS -> {
                viewModel.contactRequests.observe(this, Observer { updateAdapter() })
            }
        }

        //TODO: Can probably do it internally
        viewModel.currentUser.observe(this, Observer {
            viewModel.loadContacts()
        })
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            updateAdapter()
        }
    }

    fun updateAdapter() {
        adapter?.contacts = when (fragmentType) {
            Type.CONTACTS -> {
                val list = arrayListOf<DapiDemoContact>()
                val contacts = viewModel.contacts.value
                if (contacts != null) {
                    list.addAll(contacts)
                }
                list
            }
            Type.PENDING -> {
                val list = arrayListOf<DapiDemoContact>()
                val pendingContacts = viewModel.pendingContacts.value
                if (pendingContacts != null) {
                    list.addAll(pendingContacts)
                }
                list
            }
            Type.REQUESTS -> {
                val list = arrayListOf<DapiDemoContact>()
                val contactRequests = viewModel.contactRequests.value
                if (contactRequests != null) {
                    list.addAll(contactRequests)
                }
                list
            }
        }
    }

    private val contactsItemClickListener = object : ContactsAdapterImpl.OnItemClickListener {
        override fun onRemoveClicked(contact: DapiDemoContact) {
            progressBar.show()
        }
    }

    private val contactRequestsItemClickListener = object : ContactRequestsAdapter.OnItemClickListener {
        override fun onAcceptClicked(username: String) {
            progressBar.show()
            viewModel.createContactRequest(username, true, object : DapiRequestCallback<String> {
                override fun onSuccess(data: JsonRPCResponse<String>) {
                    Toast.makeText(activity, "Success", Toast.LENGTH_SHORT).show()
                    progressBar.hide()
                }

                override fun onError(errorMessage: String) {
                    Toast.makeText(activity, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    progressBar.hide()
                }

            })
        }
    }

}
