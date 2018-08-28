package org.dashevo.dapidemo.adapter

import org.dashevo.dapidemo.model.Contact

interface ContactsAdapter {
    var contacts: ArrayList<Contact>
    fun remove(contact: Contact)
}