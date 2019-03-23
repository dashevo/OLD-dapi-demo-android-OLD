package org.dashevo.dapidemo

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.Toast
import org.dashevo.dapiclient.callback.DapiRequestCallback
import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.dapiclient.model.JsonRPCResponse
import org.dashevo.dapidemo.adapter.ContactsPagerAdapter
import org.dashevo.dapidemo.extensions.hide
import org.dashevo.dapidemo.fragment.SearchUsersFragment
import org.dashevo.dapidemo.model.MainViewModel
import org.dashevo.dapidemo.model.readFromFile
import org.dashevo.schema.Schema
import org.dashevo.schema.SchemaLoader
import org.jsonorg.JSONObject

class MainActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("dapi-demo", Context.MODE_PRIVATE) }

    private val searchFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.searchFragment) as SearchUsersFragment
    }

    private val viewPager by lazy { findViewById<ViewPager>(R.id.pager) }
    private val pagerAdapter = ContactsPagerAdapter(supportFragmentManager)
    private val tabLayout by lazy { findViewById<TabLayout>(R.id.tabs) }
    private val progressBar by lazy { findViewById<ProgressBar>(R.id.progressBar) }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Schema.schemaLoader = object : SchemaLoader {
            override fun loadJsonSchema(): JSONObject {
                return JSONObject(readFromFile("schema_v7.json"))
            }

            override fun loadDashSystemSchema(): JSONObject {
                return JSONObject(readFromFile("dash_system_schema.json"))
            }
        }

        progressBar.hide()

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel.djService.observe(this, Observer { djServiceLiveData ->
            Toast.makeText(this, if (djServiceLiveData != null) "Connected" else "Disconnected", Toast.LENGTH_LONG).show()
        })
        viewModel.djService.toString()

        supportFragmentManager.beginTransaction().hide(searchFragment).commit()
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = pagerAdapter.count
        tabLayout.setupWithViewPager(viewPager)
    }

    override fun onResume() {
        super.onResume()
        val username = prefs.getString("username", "") ?: ""
        if (username.isNotEmpty()) {
            title = "Hello, $username"
            MainApplication.instance.dapiClient.getUser(username, object : DapiRequestCallback<BlockchainUser> {
                override fun onSuccess(data: JsonRPCResponse<BlockchainUser>) {
                    viewModel.currentUser.value = data.result
                    viewModel.loadContacts()
                }

                override fun onError(errorMessage: String) {
                    println(errorMessage)
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                }
            })
        } else {
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.contacts_menu, menu)
        val searchMenuItem = menu.findItem(R.id.search)
        val searchView = searchMenuItem.actionView as SearchView

        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                showSearchFragment()
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                hideSearchFragment()
                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchForUsers(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                searchForUsers(newText)
                return true
            }
        })

        return true
    }

    private fun hideSearchFragment(): Boolean {
        supportFragmentManager.beginTransaction().hide(searchFragment).commit()
        return false
    }

    private fun showSearchFragment() {
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_up, R.anim.silde_down)
                .show(searchFragment).commit()
    }

    fun searchForUsers(query: String) {
        searchFragment.search(query)
    }

}