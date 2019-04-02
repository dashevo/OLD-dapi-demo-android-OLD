package org.dashevo.dapidemo.adapter

import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.dashevo.dapidemo.R
import org.dashevo.dapidemo.model.DapiDemoContact
import org.dashevo.dapidemo.model.MainViewModel

class ContactsAdapterImpl(val viewModel: MainViewModel) : RecyclerView.Adapter<ContactsAdapterImpl.ContactViewHolder>(), ContactsAdapter {
    var itemClickListener: OnItemClickListener? = null

    override var contacts: ArrayList<DapiDemoContact> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.contact_item, parent, false)
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
        val removeBtn: AppCompatImageButton by lazy { itemView.findViewById<AppCompatImageButton>(R.id.removeBtn) }

        fun bind(user: DapiDemoContact) {
            username.text = if (user.from == viewModel.currentUser.value?.uname) {
                user.relation
            } else {
                user.from
            }
            removeBtn.setOnClickListener { itemClickListener?.onRemoveClicked(user) }
        }

    }

    override fun remove(contact: DapiDemoContact) {
        //TODO
        notifyDataSetChanged()
    }

    interface OnItemClickListener {
        fun onRemoveClicked(contact: DapiDemoContact)
    }

}