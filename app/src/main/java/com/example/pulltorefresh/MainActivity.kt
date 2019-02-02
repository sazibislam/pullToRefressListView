package com.example.pulltorefresh

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import com.example.testpullrefresh.PullToRefresh
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    private var listView: PullToRefresh? = null
    private var adapter: PullToRefreshListViewSampleAdapter? = null

    // IDs for the context menu actions
    private val idEdit = 1
    private val idDelete = 2


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        listView = findViewById(R.id.pull_to_refresh_listview) as PullToRefresh

        // OPTIONAL: Disable scrolling when list is refreshing
        // listView.setLockScrollWhileRefreshing(false);

        // OPTIONAL: Uncomment this if you want the Pull to Refresh header to show the 'last updated' time
        // listView.setShowLastUpdatedText(true);

        // OPTIONAL: Uncomment this if you want to override the date/time format of the 'last updated' field
        // listView.setLastUpdatedDateFormat(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));

        // OPTIONAL: Uncomment this if you want to override the default strings
        // listView.setTextPullToRefresh("Pull to Refresh");
        // listView.setTextReleaseToRefresh("Release to Refresh");
        // listView.setTextRefreshing("Refreshing");

        // MANDATORY: Set the onRefreshListener on the list. You could also use
        // listView.setOnRefreshListener(this); and let this Activity
        // implement OnRefreshListener.
        listView!!.setOnRefreshListener(object : PullToRefresh.OnRefreshListener {

            override fun onRefresh() {
                // Your code to refresh the list contents goes here

                // for example:
                // If this is a webservice call, it might be asynchronous so
                // you would have to call listView.onRefreshComplete(); when
                // the webservice returns the data
                adapter?.loadData()

                // Make sure you call listView.onRefreshComplete()
                // when the loading is done. This can be done from here or any
                // other place, like on a broadcast receive from your loading
                // service or the onPostExecute of your AsyncTask.

                // For the sake of this sample, the code will pause here to
                // force a delay when invoking the refresh
                listView!!.postDelayed(Runnable { listView!!.onRefreshComplete() }, 2000)
            }
        })

        adapter = object : PullToRefreshListViewSampleAdapter() {

        }
        listView?.setAdapter(adapter)

        // Request the adapter to load the data
        adapter?.loadData()

        // click listener
        listView!!.setOnItemClickListener(AdapterView.OnItemClickListener { arg0, arg1, arg2, arg3 ->
            val viewHolder = arg1.tag as PullToRefreshListViewSampleAdapter.ViewHolder
            if (viewHolder.name != null) {
                //Toast.makeText(MainActivity, viewHolder.name.getText(), Toast.LENGTH_SHORT).show()
            }
        })

        // Register the context menu for actions
        registerForContextMenu(listView)
    }


    /**
     * Create the context menu with the Edit and Delete options
     */
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)

        // Add any actions you need. Implement the logic in onContextItemSelected
        menu.add(Menu.NONE, idEdit, Menu.NONE, R.string.edit)
        menu.add(Menu.NONE, idDelete, Menu.NONE, R.string.delete)
    }

    /**
     * Event called after an option from the context menu is selected
     */
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo

        when (item.itemId) {
            idEdit -> {

                // Put your code here for Edit action
                // just as an example a toast message
                Toast.makeText(
                    this,
                    getString(R.string.edit) + " " + adapter?.getItem(info.position - 1),
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }
            idDelete -> {

                // Put your code here for Delete action
                // just as an example a toast message
                Toast.makeText(
                    this,
                    getString(R.string.delete) + " " + adapter?.getItem(info.position - 1),
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }
            else -> return super.onContextItemSelected(item)
        }

}


    abstract inner class PullToRefreshListViewSampleAdapter : android.widget.BaseAdapter() {

        private var items = ArrayList<String>()

        inner class ViewHolder {
            var id: String? = null
            var name: TextView? = null
        }

        /**
         * Loads the data.
         */
        fun loadData() {

            // Here add your code to load the data for example from a webservice or DB

            items = ArrayList()

            items.add("Ali Hasan")
            items.add("Tista")
            items.add("Tumpa")
            items.add("Chelsea")
            items.add("Real Madrid")
            items.add("Bayern Munchen")
            items.add("Internazionale")
            items.add("Valencia")
            items.add("Arsenal")
            items.add("AS Roma")
            items.add("Tottenham Hotspur")
            items.add("PSV")
            items.add("Olympique Lyon")
            items.add("AC Milan")
            items.add("Dortmund")
            items.add("Schalke 04")
            items.add("Twente")
            items.add("Porto")
            items.add("Juventus")


            // MANDATORY: Notify that the data has changed
            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(position: Int): Any {
            return items[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var rowView = convertView

            val record = getItem(position) as String

            val inflater =getLayoutInflater()

            val viewHolder = ViewHolder()

            if (convertView == null) {
                rowView = inflater.inflate(R.layout.list_item, null)

                viewHolder.name = rowView!!.findViewById(R.id.textView1) as TextView

                rowView.tag = viewHolder
            }

            val holder = rowView!!.tag as ViewHolder

            holder.name?.setText(record)

            return rowView
        }
    }
}
