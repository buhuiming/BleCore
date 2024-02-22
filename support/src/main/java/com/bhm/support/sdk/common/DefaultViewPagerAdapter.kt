package com.bhm.support.sdk.common

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle

/**
 * viewpager2的adapter
 */
class DefaultViewPagerAdapter : FragmentStateAdapter {
    private var fragmentsList: List<Fragment>? = null

    constructor(fragmentActivity: FragmentActivity, fragmentsList: List<Fragment>?) : super(
        fragmentActivity
    ) {
        this.fragmentsList = fragmentsList
    }

    constructor(fragment: Fragment, fragmentsList: List<Fragment>?) : super(fragment) {
        this.fragmentsList = fragmentsList
    }

    constructor(fragmentManager: FragmentManager, lifecycle: Lifecycle) : super(
        fragmentManager,
        lifecycle
    ) {
    }

    override fun createFragment(position: Int): Fragment {
        return fragmentsList!![position]
    }

    override fun getItemCount(): Int {
        return fragmentsList!!.size
    }
}