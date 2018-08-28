package org.dashevo.dapidemo.adapter

import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.dashevo.dapiclient.model.BlockchainUserContainer
import org.dashevo.dapidemo.R
import org.dashevo.dapidemo.model.Contact

class ContactRequestsAdapter : RecyclerView.Adapter<ContactRequestsAdapter.ContactViewHolder>(), ContactsAdapter {

    var itemClickListener: OnItemClickListener? = null

    override var contacts: ArrayList<Contact> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.contact_request_item, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val username: TextView by lazy { itemView.findViewById<TextView>(R.id.username) }
        val acceptBtn: AppCompatImageButton by lazy { itemView.findViewById<AppCompatImageButton>(R.id.acceptBtn) }

        fun bind(user: Contact) {
            username.text = user.meta?.uname ?: user.blockchainUser?.uname ?: ""
            acceptBtn.setOnClickListener {
                if (user.meta != null) {
                    itemClickListener?.onAcceptClicked(user.meta.buid)
                }
            }
        }

    }

    override fun remove(contact: Contact) {
        contacts.remove(contact)
        notifyDataSetChanged()
    }

    interface OnItemClickListener {
        fun onAcceptClicked(userId: String)
    }

}