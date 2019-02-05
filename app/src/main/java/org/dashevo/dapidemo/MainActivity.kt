package org.dashevo.dapidemo

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.Toast
import org.dashevo.dapiclient.callback.BaseCallback
import org.dashevo.dapiclient.model.DapContext
import org.dashevo.dapiclient.model.DapSpace
import org.dashevo.dapidemo.adapter.ContactsPagerAdapter
import org.dashevo.dapidemo.dapi.DapiDemoClient
import org.dashevo.dapidemo.extensions.hide
import org.dashevo.dapidemo.extensions.show
import org.dashevo.dapidemo.fragment.SearchUsersFragment
import org.dashevo.schema.Schema
import org.dashevo.schema.SchemaLoader
import org.jsonorg.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {


    private val searchFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.searchFragment) as SearchUsersFragment
    }

    private val viewPager by lazy { findViewById<ViewPager>(R.id.pager) }
    private val pagerAdapter = ContactsPagerAdapter(supportFragmentManager)
    private val tabLayout by lazy { findViewById<TabLayout>(R.id.tabs) }
    private val progressBar by lazy { findViewById<ProgressBar>(R.id.progressBar)}

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

        supportFragmentManager.beginTransaction().hide(searchFragment).commit()
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = pagerAdapter.count
        tabLayout.setupWithViewPager(viewPager)

        val dapJson = JSONObject(readFromFile("dapi_demo_dap.json"))
        DapiDemoClient.dapSchema = dapJson
        progressBar.show()
    }

    override fun onResume() {
        super.onResume()
        if (DapiDemoClient.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            fetchDapContext()
        }
    }

    private fun fetchDapContext() {
        progressBar.show()
        if (!DapiDemoClient.checkDap()) {
            DapiDemoClient.initDap(object : BaseCallback<String> {
                override fun onSuccess(dapId: String) {
                    Log.i("DAP", "dap initialized successfully")
                    dapSignUp()
                    progressBar.hide()
                }

                override fun onError(errorMessage: String) {
                    Toast.makeText(this@MainActivity, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    progressBar.hide()
                }
            })
        } else {
            progressBar.show()
            DapiDemoClient.getDapContext(object : BaseCallback<DapContext> {
                override fun onSuccess(dapContext: DapContext) {
                    pagerAdapter.currentFragment?.updateAdapter()
                    progressBar.hide()
                }

                override fun onError(errorMessage: String) {
                    Toast.makeText(this@MainActivity, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    progressBar.hide()
                }

            })
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
        fetchDapContext()
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


    private fun dapSignUp() {
        progressBar.show()
        DapiDemoClient.getDapSpaceOrSignUp("I'm ${DapiDemoClient.currentUser?.uname}",
                object : BaseCallback<DapSpace> {
                    override fun onSuccess(dapSpace: DapSpace) {
                        fetchDapContext()
                        progressBar.hide()
                    }

                    override fun onError(errorMessage: String) {
                        progressBar.hide()
                        Toast.makeText(this@MainActivity,
                                "Failed to signup in DAP $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                })
    }

    fun readFromFile(fileName: String): String {
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

}
