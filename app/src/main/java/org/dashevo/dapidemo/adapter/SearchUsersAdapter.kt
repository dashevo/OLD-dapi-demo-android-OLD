package org.dashevo.dapidemo.adapter

import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.dashevo.dapidemo.R
import org.dashevo.dapidemo.model.DapiDemoUser

class SearchUsersAdapter : RecyclerView.Adapter<SearchUsersAdapter.SearchResultViewHolder>() {

    var onItemClickListener: OnItemClickListener? = null

    var contacts: List<DapiDemoUser> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.search_result_item, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    inner class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val username: TextView by lazy { itemView.findViewById<TextView>(R.id.username) }
        val addBtn: AppCompatImageButton by lazy { itemView.findViewById<AppCompatImageButton>(R.id.addBtn) }

        fun bind(user: DapiDemoUser) {
            username.text = user.bUserName
            addBtn.setOnClickListener {
                onItemClickListener?.onItemClicked(user)
            }
        }

    }

    interface OnItemClickListener {
        fun onItemClicked(dapiDemoUser: DapiDemoUser)
    }

}