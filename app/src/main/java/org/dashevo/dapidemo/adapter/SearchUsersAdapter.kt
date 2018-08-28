package org.dashevo.dapidemo.adapter

import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.dapiclient.model.BlockchainUserContainer
import org.dashevo.dapidemo.R

class SearchUsersAdapter : RecyclerView.Adapter<SearchUsersAdapter.SearchResultViewHolder>() {

    var onItemClickListener: OnItemClickListener? = null

    var contacts: List<BlockchainUserContainer> = arrayListOf()
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
        holder.bind(contacts[position].blockchainuser)
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    inner class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val username: TextView by lazy { itemView.findViewById<TextView>(R.id.username) }
        val addBtn: AppCompatImageButton by lazy { itemView.findViewById<AppCompatImageButton>(R.id.addBtn) }

        fun bind(user: BlockchainUser) {
            username.text = user.uname
            addBtn.setOnClickListener {
                onItemClickListener?.onItemClicked(user)
            }
        }

    }

    interface OnItemClickListener {
        fun onItemClicked(blockchainUserContainer: BlockchainUser)
    }

}