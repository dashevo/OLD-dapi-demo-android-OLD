package org.dashevo.dapidemo.adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.view.View
import android.view.ViewGroup
import org.dashevo.dapidemo.fragment.ContactsFragment

class ContactsPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    var currentFragment: ContactsFragment? = null

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> ContactsFragment.newInstance(ContactsFragment.Type.CONTACTS)
            1 -> ContactsFragment.newInstance(ContactsFragment.Type.PENDING)
            2 -> ContactsFragment.newInstance(ContactsFragment.Type.REQUESTS)
            else -> Fragment()
        }
    }

    override fun getCount(): Int = 3

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> ContactsFragment.Type.CONTACTS.title
            1 -> ContactsFragment.Type.PENDING.title
            2 -> ContactsFragment.Type.REQUESTS.title
            else -> "Tab"
        }
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, obj: Any) {
        if (currentFragment != obj && obj is ContactsFragment) {
            currentFragment = obj
        }
        super.setPrimaryItem(container, position, obj)
    }

}