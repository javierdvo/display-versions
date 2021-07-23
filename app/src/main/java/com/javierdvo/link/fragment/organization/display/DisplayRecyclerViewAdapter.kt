package com.javierdvo.link.fragment.organization.display

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.javierdvo.link.R
import com.javierdvo.link.classes.devices.Display


class DisplayRecyclerViewAdapter internal constructor(private val mValues: List<Display>, private val mListener: DisplayListFragment.OnListFragmentInteractionListener?
) : RecyclerView.Adapter<DisplayRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_display, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        holder.mNameView.text = mValues[position].name
        holder.mView.setOnClickListener {
            mListener?.onListFragmentInteraction(holder.mItem!!)
        }
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder internal constructor(internal val mView: View) : RecyclerView.ViewHolder(mView) {
        internal val mNameView: TextView = mView.findViewById(R.id.card_display_name)
        internal var mItem: Display? = null

        init {

        }

        override fun toString(): String {
            return "ViewHolder{" +
                    "mView=" + mView +
                    ", mNameView=" + mNameView +

                    ", mItem=" + mItem +
                    '}'.toString()
        }
    }
}
