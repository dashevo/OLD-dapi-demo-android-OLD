package org.dashevo.dapidemo.fragment

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
import org.dashevo.dapiclient.callback.CommitDapObjectCallback
import org.dashevo.dapiclient.callback.UsersCallback
import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.dapiclient.model.BlockchainUserContainer
import org.dashevo.dapidemo.R
import org.dashevo.dapidemo.adapter.SearchUsersAdapter
import org.dashevo.dapidemo.dapi.DapiDemoClient
import org.dashevo.dapidemo.extensions.hide
import org.dashevo.dapidemo.extensions.show
import java.util.*

class SearchUsersFragment : Fragment() {

    private val adapter = SearchUsersAdapter()
    private var searchTimer = Timer()
    private val searchDelay = 500L
    private val searchRv by lazy { view!!.findViewById<RecyclerView>(R.id.searchRv) }
    private val progressBar: ProgressBar by lazy { view!!.findViewById<ProgressBar>(R.id.progressBar) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchRv.layoutManager = LinearLayoutManager(activity)
        searchRv.adapter = adapter

        adapter.onItemClickListener = object : SearchUsersAdapter.OnItemClickListener {
            override fun onItemClicked(blockchainUserContainer: BlockchainUser) {
                addContact(blockchainUserContainer)
            }
        }
    }

    fun search(query: String) {
        progressBar.show()
        if (query.isNotEmpty()) {
            searchTimer.cancel()
            searchTimer = Timer()
            searchTimer.schedule(object : TimerTask() {
                override fun run() {
                    DapiDemoClient.searchUsers(query, object : UsersCallback {
                        override fun onSuccess(users: List<BlockchainUserContainer>) {
                            adapter.contacts = users
                            progressBar.hide()
                        }

                        override fun onError(errorMessage: String) {
                            Log.d("Error", "Error $errorMessage")
                            progressBar.hide()
                        }
                    })
                }
            }, searchDelay)
        } else {
            adapter.contacts = listOf()
        }
    }

    private fun addContact(user: BlockchainUser) {
        progressBar.show()
        DapiDemoClient.addContact(user, object : CommitDapObjectCallback {
            override fun onSuccess(dapId: String, txId: String) {
                Toast.makeText(activity, "contact successfully added", Toast.LENGTH_SHORT).show()
                progressBar.hide()
            }

            override fun onError(errorMessage: String) {
                Toast.makeText(activity, "failed to add contact", Toast.LENGTH_SHORT).show()
                progressBar.hide()
            }
        })
    }
}
