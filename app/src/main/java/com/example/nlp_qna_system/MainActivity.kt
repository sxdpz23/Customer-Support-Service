package com.example.nlp_qna_system

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import co.intentservice.chatui.ChatView
import co.intentservice.chatui.models.ChatMessage
import com.jakewharton.threetenabp.AndroidThreeTen
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.UnsupportedEncodingException
import java.lang.Exception
import java.net.URL
import java.net.URLConnection
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // Used for Assistant Feature
    var chatview: ChatView? = null
    private val REQ_CODE_SPEECH_INPUT = 100
    private var btnSpeak: Button? = null
    private var txtSpeechInput: String? = null
    var outputText: String? = null
    private var tts: TextToSpeech? = null
    private val API_KEY: String = "446c518b839e46fb939361f21ade706a"
    private var timeStamp : Long? = null
    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AndroidThreeTen.init(this)

        Log.d("Started","Just came on the Listening Activity")
        btnSpeak = findViewById(R.id.btnlistening)
        chatview = findViewById(R.id.chat_view)

        btnSpeak?.isEnabled = false
        tts = TextToSpeech(this, this)
        promptSpeechInput(chatview)

        chatview?.setOnSentMessageListener { chatMessage ->
            count = 1
            txtSpeechInput = chatMessage.message
            val task = RetrieveFeedTask()
            /**Passing the user query to a new class which connects,
             * receives and filtering operations on the data from api.ai
             */
            /**Passing the user query to a new class which connects,
             * receives and filtering operations on the data from api.ai
             */
            task.execute(txtSpeechInput)
            true
        }
    }

    /**
     * Showing google speech input dialog
     */
    fun promptSpeechInput(view: View?) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        // intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(    //Prompting user to say something
            RecognizerIntent.EXTRA_PROMPT,
            "Say Something"
        )
        try {   //"If user said something then proceed" action
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        } catch (a: ActivityNotFoundException) {
            Toast.makeText(
                applicationContext,
                "Sorry! Your device doesn\\'t support speech input",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Receiving speech input
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_CODE_SPEECH_INPUT -> { // if the request code sent is the same request code
                if (resultCode == Activity.RESULT_OK && null != data) {
                    val result: ArrayList<String> = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)!!
                    val userQuery = result[0]
                    txtSpeechInput = userQuery
                    val task = RetrieveFeedTask()
                    /**Passing the user query to a new class which connects,
                     * receives and filtering operations on the data from api.ai
                     */
                    task.execute(userQuery)
                }
            }
        }
    }

    // Create GetText Method to request the user query to api.ai agent
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Throws(UnsupportedEncodingException::class)
    fun getText(query: String?): String? {
        val text: String
        var reader: BufferedReader? = null

        // Send data
        try {
            // Defined URL  where to send data
            val url = URL("https://api.api.ai/v1/query?v=20150910")

            // Send POST data request
            val conn: URLConnection = url.openConnection()
            conn.doOutput = true
            conn.doInput = true
            conn.setRequestProperty(
                "Authorization",
                "Bearer $API_KEY"     //Client Acces Token for the api.ai token
            )
            conn.setRequestProperty("Content-Type", "application/json")

            //Create JSONObject here
            val jsonParam = JSONObject()
            val queryArray = JSONArray()
            queryArray.put(query)
            jsonParam.put("query", queryArray)
            //            jsonParam.put("name", "order a medium pizza");
            jsonParam.put("lang", "en")
            jsonParam.put("sessionId", "1234567890")

            val wr = OutputStreamWriter(conn.getOutputStream())
            Log.d("karma", "after conversion is $jsonParam")
            wr.write(jsonParam.toString())
            wr.flush()
            Log.d("karma", "json is $jsonParam")

            // Get the server response
            reader = BufferedReader(InputStreamReader(conn.getInputStream()))
            val sb = StringBuilder()
            var line: String?

            // Read Server Response
            while (reader.readLine().also { line = it } != null) {
                // Append server response in string
                sb.append("""$line""".trimIndent())
            }
            text = sb.toString() //returned data from the api.ai agent

            val object1 = JSONObject(text)
            val `object` = object1.getJSONObject("result")
            val fulfillment: JSONObject?
            val speech: String?
            //            if (object.has("fulfillment")) {
            fulfillment = `object`.getJSONObject("fulfillment")
            //                if (fulfillment.has("speech")) {
            speech = fulfillment.optString("speech")
            //                }
//            }

            Log.d("karma ", "response is $text")

            speakOut(speech.toString())
            return speech // returning the value needed after filtering from the received data

        } catch (ex: Exception) {
            Log.d("karma", "exception at last $ex")
        } finally {
            try {
                reader?.close()
            } catch (ex: Exception) {
            }
        }
        return null
    }

    internal inner class RetrieveFeedTask :
        AsyncTask<String?, Void?, String?>() {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun doInBackground(vararg voids: String?): String? {
            var s: String? = null
            try {
                s = getText(voids[0])
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                Log.d("karma", "Exception occurred $e")
            }
            return s
        }

        override fun onPostExecute(s: String?) {
            super.onPostExecute(s)
            chatview = findViewById(R.id.chat_view)
            outputText = s
        }
    }
    // speak out the answer we got from the api.ai agent
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // set US English as language for tts
            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language specified is not supported!")
            } else {
                btnSpeak?.isEnabled = true
            }

        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun speakOut(speakText: String) {
        this@MainActivity.runOnUiThread { //as every time it was giving error because we were performing actions on different thread
            timeStamp = System.currentTimeMillis()
            if (count != 1) {
                chatview = findViewById(R.id.chat_view)
                val userMessage = ChatMessage(txtSpeechInput, timeStamp!!, ChatMessage.Type.SENT)
                chatview?.addMessage(userMessage)
            }
            tts!!.speak(speakText, TextToSpeech.QUEUE_FLUSH, null,"")  //when the input was using the voice
            val arcaMessage = ChatMessage(speakText, timeStamp!!, ChatMessage.Type.RECEIVED)
            chatview?.addMessage(arcaMessage)
        }
    }

    public override fun onDestroy() {
        // Shutdown TTS
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }
}
