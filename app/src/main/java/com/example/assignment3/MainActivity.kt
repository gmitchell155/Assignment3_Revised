//George Mitchell
//CSCI 4020
//Assignment 3

package com.example.assignment3

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment3.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    //creates a mutableList of Strings to store objectIDs to be displayed in Recyclerview
    private var display_object = mutableListOf<String>()
    //creates a mutableList of Strings to store objectIDs from api results
    private var objectID = mutableListOf<String>()
    private lateinit var binding: ActivityMainBinding
    private lateinit var  adapter: MyAdapter
    //makes a global connection variable
    private lateinit var connection: HttpURLConnection
    //boolean used to determine if a search has already been done
    private var searchStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.searchButton.setOnClickListener(DownloadListener())

        val layoutManager = LinearLayoutManager(this)
        binding.myRecyclerview.setLayoutManager(layoutManager)

        //adds divider between each list item
        val divider = DividerItemDecoration(
            applicationContext, layoutManager.orientation
        )
        binding.myRecyclerview.addItemDecoration(divider)

        adapter = MyAdapter()
        binding.myRecyclerview.setAdapter(adapter)



    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.acknowledge_menu_item){
            //displays acknowledgement message
            val builder = AlertDialog.Builder(binding.root.context)
                .setTitle("Acknowledgements")
                .setMessage(R.string.dialog_message)
                .setPositiveButton("ok", null)
                .show()
        }
        return super.onOptionsItemSelected(item)
    }

    inner class MyViewHolder(val itemView : View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener{

        init{
            itemView.findViewById<View>(R.id.object_constraintLayout)
                .setOnClickListener(this)
        }

        fun setText(text : String){
            itemView.findViewById<TextView>(R.id.object_view)
                .setText(text)
        }

        override fun onClick(view: View?) {
            if(view != null){
                val intent = Intent(view.context, ArtObjectActivity::class.java)
                val objectInfo = objectID[adapterPosition]
                intent.putExtra(
                    getString(R.string.object_intent_key),
                    objectInfo
                )
                startActivity(intent)
            }
        }
    }

    inner class MyAdapter() : RecyclerView.Adapter<MyViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.object_view, parent, false)


            return MyViewHolder(view)

        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.setText(display_object[position])

        }

        override fun getItemCount(): Int {

            return display_object.size

        }

    }


    fun isNetworkAvailable(): Boolean {
        var available = false

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        cm?.run {

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                cm.getNetworkCapabilities(cm.activeNetwork)?.run{
                    if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    ){
                        available = true
                    }
                }
            } else{

                cm.getActiveNetworkInfo()?.run {
                    if(type == ConnectivityManager.TYPE_MOBILE
                        || type == ConnectivityManager.TYPE_WIFI
                        || type == ConnectivityManager.TYPE_VPN
                    ){
                        available = true
                    }

                }
            }
        }

        return available
    }

    private var downloadJob: Job? = null


    inner class DownloadListener : View.OnClickListener{
        override fun onClick(view: View?){
            if(isNetworkAvailable()) {
                if(downloadJob?.isActive != true){
                    startDownload()
                } else{
                    Toast.makeText(
                        applicationContext,
                        R.string.toast_message, Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun startDownload(){
        //increases after button is pushed
        if(!searchStarted){
            //do nothing
        }//below clears out all the objects from the RecyclerView
        else if(searchStarted){
            //functions strangely. The first time you change search another word it clears out in one
            //    press. After that you have to press the button twice to get the new values.
            for (i in display_object.indices) {
                display_object.removeAt(0)
            }
            display_object.clear()
            objectID.clear()

            //Log.i("Are the values gone?", display_object.toString())
            adapter.notifyDataSetChanged()
        }

        downloadJob = CoroutineScope(Dispatchers.IO).launch{
            var terms = binding.termsEditTextText.getText()
            //builds url link based on seacrh term
            val builder = Uri.Builder()
                .scheme("https")
                .authority("collectionapi.metmuseum.org")
                .path("/public/collection/v1/search")
                .appendQueryParameter("q", terms.toString())

            val path = builder.build().toString()

            val builder2 = Uri.Builder()
                .scheme("https")
                .authority("collectionapi.metmuseum.org")
                val url = URL(path)
                connection = url.openConnection() as HttpURLConnection

                var jsonStr = ""
                try {
                    jsonStr = connection.getInputStream()
                        .bufferedReader().use(BufferedReader::readText)
                } finally {
                    connection.disconnect()
                }
                //creates new JSONObject
                val json = JSONObject(jsonStr)
                //gets total from JSONObject
                val total = json.getInt("total")
                if (total == 0){
                    //if the total is 0 then the termsEditTextText is cleared
                    this@MainActivity.runOnUiThread(java.lang.Runnable {
                        binding.termsEditTextText.setText("")
                    })
                }else{
                    //gets array of objectIDs from JSONObject
                    val objectIDs = json.getJSONArray("objectIDs")
                    //Log.i("test2", objectIDs.toString())

                    for (i in 0 until objectIDs.length()) {
                        //adds each objectID to a mutableList
                        objectID += objectIDs.getInt(i).toString()

                        //Log.i("test2", objectID.toString())

                    }
                    //Log.i("test", total.toString())
                    //Log.i("test", path)

                    //Log.i("test2", objectID.toString())

                    //displays all objects to RecyclerView
                    if(total < 20 && total != 0){
                        for(i in 0 until objectIDs.length()) {
                            //new url path
                             builder2.path("/public/collection/v1/objects/" + objectID[i])

                                val path = builder2.build().toString()
                                val url = URL(path)
                                connection = url.openConnection() as HttpURLConnection
                            }

                            var jsonStr2 = ""
                            try {
                                jsonStr2 = connection.getInputStream()
                                    .bufferedReader().use(BufferedReader::readText)
                            }//catches FileNOtFoundException
                            catch(e: FileNotFoundException){

                            } finally {
                                connection.disconnect()
                            }
                            //makes string builder
                            val display = StringBuilder()
                            //new JSONObject
                            var json2 = JSONObject()
                            var objectName = ""
                            var title = ""

                            try{
                                //creates new JSONObject
                                json2 = JSONObject(jsonStr2)
                            }//catches JSONException
                            catch(e: JSONException){
                                //JSON Exception
                            }

                            try{
                                //sets objectName
                                objectName = json2.getString("objectName")
                                //sets title
                                title = json2.getString("title")
                                //formats objectName and title to be displayed to RecyclerView
                                display.append("$objectName - $title")
                                // Log.i("values", display.toString())
                                //adds the value in display to be put into RecyclerVIew
                                display_object.add(display.toString())
                            }//catches JSONException
                            catch(e: JSONException){
                                //JSON Exception
                            }

                            Log.i("testing", path)
                        } else if(total > 20){
                        //adds 20 items to RecyclerView

                        for(i in 0..19){
                            builder2.path("/public/collection/v1/objects/" + objectID[i])
                            val path = builder2.build().toString()
                            val url = URL(path)
                            connection = url.openConnection() as HttpURLConnection
                            }


                            var jsonStr2 = ""
                            try {
                                jsonStr2 = connection.getInputStream()
                                    .bufferedReader().use(BufferedReader::readText)

                            } //catches FileNotFoundException
                            catch (e: FileNotFoundException){

                            } finally {
                                    connection.disconnect()
                                }

                            val display = StringBuilder()
                            //creates a new JSONObject
                            var json2 = JSONObject()
                            //sets objectName to empty string
                            var objectName: String
                            //sets title to empty string
                            var title: String
                            //another try catch here
                            try{
                                json2 = JSONObject(jsonStr2)

                            }//catches JSONException
                            catch(e: JSONException){
                                //JSON Exception
                            }

                            try{
                                //sets objectName
                                objectName = json2.getString("objectName")
                                //sets title
                                title = json2.getString("title")
                                //formats objectName and title to be displayed to RecyclerView
                                display.append("$objectName - $title")
                                // Log.i("values", display.toString())
                                //adds the value in display to be put into RecyclerVIew
                                display_object.add(display.toString())

                            }//catches JSONException
                            catch(e: JSONException){

                            }
                            searchStarted = true


                        }

                    }

            withContext(Dispatchers.Main){
                //updates adapter with values to display to Recyclerview
                adapter.notifyDataSetChanged()
                MyAdapter()
            }
        }

    }

}