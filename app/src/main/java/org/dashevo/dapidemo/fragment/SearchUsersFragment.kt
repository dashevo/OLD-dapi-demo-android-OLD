package org.dashevo.dapidemo.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import org.dashevo.dapiclient.callback.DapiRequestCallback
import org.dashevo.dapiclient.model.JsonRPCResponse
import org.dashevo.dapidemo.MainApplication
import org.dashevo.dapidemo.R
import org.dashevo.dapidemo.adapter.SearchUsersAdapter
import org.dashevo.dapidemo.extensions.hide
import org.dashevo.dapidemo.extensions.show
import org.dashevo.dapidemo.model.DapiDemoUser
import org.dashevo.dapidemo.model.MainViewModel
import org.dashevo.dapidemo.model.MainViewModel.Companion.DAPI_ID
import java.util.*

class SearchUsersFragment : Fragment() {

    private val adapter = SearchUsersAdapter()
    private var searchTimer = Timer()
    private val searchDelay = 500L
    private val searchRv by lazy { view!!.findViewById<RecyclerView>(R.id.searchRv) }
    private val progressBar: ProgressBar by lazy { view!!.findViewById<ProgressBar>(R.id.progressBar) }
    private val dapiClient = MainApplication.instance.dapiClient
    private lateinit var viewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchRv.layoutManager = LinearLayoutManager(activity)
        searchRv.adapter = adapter

        viewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
        viewModel.walletInfoLiveData.observe(this, Observer {
            //workaround to connect to peers
        })
        viewModel.djService.observe(this, Observer { djServiceLiveData ->
            Toast.makeText(context, if (djServiceLiveData != null) "Connected" else "Disconnected", Toast.LENGTH_LONG).show()
        })

        adapter.onItemClickListener = object : SearchUsersAdapter.OnItemClickListener {
            override fun onItemClicked(dapiDemoUser: DapiDemoUser) {
                addContact(dapiDemoUser)
            }
        }
        loadUsers()
    }

    private fun loadUsers() {
        MainApplication.instance.dapiClient
    }

    fun search(query: String) {
        progressBar.show()
        if (query.isNotEmpty()) {
            searchTimer.cancel()
            searchTimer = Timer()
            searchTimer.schedule(object : TimerTask() {
                override fun run() {
                    dapiClient.fetchDapObjects(DAPI_ID, "profile", object : DapiRequestCallback<List<DapiDemoUser>> {
                        override fun onSuccess(data: JsonRPCResponse<List<DapiDemoUser>>) {
                            adapter.contacts = data.result!!
                            progressBar.hide()
                        }

                        override fun onError(errorMessage: String) {
                            Log.d("Error", "Error $errorMessage")
                            progressBar.hide()
                        }
                    }, mapOf("data.bUserName" to mapOf("\$regex" to "^$query")) )
                }
            }, searchDelay)
        } else {
            adapter.contacts = listOf()
        }
    }

    private fun addContact(user: DapiDemoUser) {
        progressBar.show()
        viewModel.createContactRequest(user.bUserName, false, object : DapiRequestCallback<String> {
            override fun onSuccess(data: JsonRPCResponse<String>) {
                Toast.makeText(this@SearchUsersFragment.context, "Success", Toast.LENGTH_SHORT).show()
            }

            override fun onError(errorMessage: String) {
                Toast.makeText(this@SearchUsersFragment.context, "Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
