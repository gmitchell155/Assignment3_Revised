package com.example.assignment3

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.assignment3.databinding.ActivityArtObjectBinding
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL

class ArtObjectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityArtObjectBinding

    private var objectSearch = ""
    //this value will hold the Accession Number got from searching the object ID
    private var accessionNum = ""
    //this value will hold the department got from searching the object ID
    private var department =""
    //this value will hold the object name got from searching the object ID
    private var objectName =""
    //this value will hold the title got from searching the object ID
    private var title =""
    //this value will hold the medium got from searching the object ID
    private var medium =""
    //this value will hold the object date got from searching the object ID
    private var objectDate =""
    //this value will hold the artist display name got from searching the object ID
    private var artistName = ""
    //this value will hold the artist display bio got from searching the object ID
    private var artistBio = ""
    //this value will hold the credit line got from searching the object ID
    private var credit = ""
    ////this value will hold the link to the object got from searching the object ID
    private var urlString = ""
    //this value will hold the primaryImageSmall url got from searching the object ID
    private var primaryImageSmall = ""
    //sets global url variable
    private lateinit var url: URL
    //sets global connection variable
    private lateinit var connection: HttpURLConnection
    //sets global url variable for image search
    private lateinit var url2: URL
    //sets global connection variable for image search
    private lateinit var connection2: HttpURLConnection
    //sets global bitmap variable for image
    private var bitmap : Bitmap? = null
    //sets global variable for JSONObject
    private var json3 = JSONObject()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtObjectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.metButton.setOnClickListener(OpenWebsite())
        val intent = getIntent()

        val objectInfo = intent.getStringExtra(
            getString(R.string.object_intent_key)
        )
        //sets objectSearch to objectID passed from intent
        objectSearch = objectInfo.toString()

        Log.i("what is in in here", objectInfo.toString())
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

    private fun startDownload() {
        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            //makes new url based on the objectID of the item clicked in the RecyclerView
            val builder = Uri.Builder()
                .scheme("https")
                .authority("collectionapi.metmuseum.org")
            builder.path("/public/collection/v1/objects/" + objectSearch)
            val path2 = builder.build().toString()
            url = URL(path2)
            //Log.i("URL", path2)
            connection = url.openConnection() as HttpURLConnection
            var jsonStr3 = ""
                try {
                    jsonStr3 = connection.getInputStream()
                        .bufferedReader().use(BufferedReader::readText)
                }catch(e: FileNotFoundException){
                    //File not found
                } finally {
                    connection.disconnect()
                }
                //new JSONObject
                 try{
                     json3 = JSONObject(jsonStr3)
                } catch(e: JSONException){
                     //JSON Exception
                 }

                try{
                    //gets accession number from the JSONObject
                    accessionNum = json3.getString("accessionNumber")
                    //gets accession number from the JSONObject
                    department = json3.getString("department")
                    //gets accession number from the JSONObject
                    objectName = json3.getString("objectName")
                    //gets accession number from the JSONObject
                    title = json3.getString("title")
                    //gets accession number from the JSONObject
                    medium = json3.getString("medium")
                    //gets accession number from the JSONObject
                    objectDate = json3.getString("objectDate")
                    //gets accession number from the JSONObject
                    artistName = json3.getString("artistDisplayName")
                    //gets accession number from the JSONObject
                    artistBio = json3.getString("artistDisplayBio")
                    //gets accession number from the JSONObject
                    credit = json3.getString("creditLine")
                    //gets accession number from the JSONObject
                    urlString = json3.getString("objectURL")
                }//catches JSONException
                catch(e: JSONException){
                    //JSON Exception
                }

                //checks if there is a url in primaryImageSmall. IF so it makes a connection using image url
                try{
                    if(json3.getString("primaryImageSmall") != "") {
                        primaryImageSmall = json3.getString("primaryImageSmall")
                        //gets accession number from the JSONObject
                        url2 = URL(primaryImageSmall)
                        connection2 = url2.openConnection() as HttpURLConnection
                        //Log.i("url", url2.toString())
                        try{

                            connection2.getInputStream().use { stream ->
                                bitmap = BitmapFactory.decodeStream(stream)
                            }
                        }catch(e: UninitializedPropertyAccessException){
                            //UninitializedPropertyAccessException
                        }finally{
                            connection2.disconnect()
                        }
                    }//catches JSONException
                }catch(e: JSONException){
                    //JSONException
                }



                //Log.i("Is the image URL stored?", primaryImageSmall)
                checkValues()

            withContext(Dispatchers.Main){
                //displays accession number in the corresponding textview
                binding.accessionNumTextView.setText(accessionNum)
                //displays department in the corresponding textview
                binding.departmentTextView.setText(department)
                //displays object name in the corresponding textview
                binding.objectNameTextView.setText(objectName)
                //displays title in the corresponding textview
                binding.titleTextView.setText(title)
                //displays medium number in the corresponding textview
                binding.mediumTextView.setText(medium)
                //displays object date number in the corresponding textview
                binding.objectDateTextView.setText(objectDate)
                //displays artist display name in the corresponding textview
                binding.artistNameTextView.setText(artistName)
                //displays artist display bio in the corresponding textview
                binding.artistBioTextView.setText(artistBio)
                //displays credit line in the corresponding textview
                binding.creditLineTextView.setText(credit)

                //checks if the url in primaryImageSmall is empty
                if(primaryImageSmall != ""){
                    //displays the image from the url
                    binding.objectImageView.setImageBitmap(bitmap)
                }else{
                    //displays a default broken image if there is no url
                    binding.objectImageView.setImageResource(R.drawable.ic_baseline_image_not_supported_24)
                }

                //Log.i("What is the Image URL?", binding.objectNameTextView.getText().toString())

            }
        }


    }

    inner class OpenWebsite : View.OnClickListener{
        override fun onClick(view: View?){
            //displays a message if the objectURL has no value
            if(urlString == ""){
                val builder = AlertDialog.Builder(binding.root.context)
                    .setTitle("Page requested is unavailable")
                    .setMessage(R.string.no_information)
                    .setPositiveButton("ok", null)
                    .show()
            }else{
                //sets uri to the objectURL
                val uri = Uri.parse(urlString)
                //creates new intent
                val intent = Intent(Intent.ACTION_VIEW, uri)
                //sends user to webpage of the corresponding object
                startActivity(intent)
            }

        }
    }

    //helper function that checks values and sets them to UNKNOWN if nothing is returned
    fun checkValues(){
        if(accessionNum == ""){
            accessionNum = "UNKNOWN"
        }

        if(department == ""){
            department = "UNKNOWN"
        }

        if(objectName == ""){
            objectName = "UNKNOWN"
        }
        if(title == ""){
            title = "UNKNOWN"
        }

        if(medium == ""){
            medium = "UNKNOWN"
            Log.i("val", medium)
        }

        if(objectDate == ""){
            objectDate = "UNKNOWN"
        }

        if(artistName == ""){
            artistName = "UNKNOWN"
        }
        if(artistBio == ""){
            artistBio = "UNKNOWN"
        }

        if(credit == ""){
            credit = "UNKNOWN"
        }
    }

}