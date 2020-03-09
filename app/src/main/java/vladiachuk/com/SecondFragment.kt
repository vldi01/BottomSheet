package vladiachuk.com

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item.view.*
import kotlinx.android.synthetic.main.second_frame.*

class SecondFragment: Fragment(R.layout.second_frame) {
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private val dataSet = arrayOf(
        "Hello",
        "World",
        "Warsaw",
        "qwerty",
        "omg",
        "waterfall",
        "recyclerView",
        "Vlad Diachuk",
        "Kalabanga",
        "Manufactory",
        "Street",
        "NY",
        "Kyiv"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewManager = LinearLayoutManager(context)
        viewAdapter = MyAdapter(dataSet)
        recycler.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    class MyAdapter(private val myDataset: Array<String>) :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

        class MyViewHolder(val view: LinearLayout) : RecyclerView.ViewHolder(view)


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false) as LinearLayout

            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.view.textView.text = myDataset[position]
            holder.view.setOnClickListener {
                Toast.makeText(holder.view.context, myDataset[position], Toast.LENGTH_SHORT).show()
            }
        }

        override fun getItemCount() = myDataset.size
    }
}