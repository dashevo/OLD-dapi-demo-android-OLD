package org.dashevo.dapidemo.adapter

import org.dashevo.dapidemo.model.DapiDemoContact

interface ContactsAdapter {
    var contacts: ArrayList<DapiDemoContact>
    fun remove(contact: DapiDemoContact)
}