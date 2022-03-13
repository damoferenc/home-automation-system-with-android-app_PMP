package com.example.pmp_project_app

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*
import java.io.InputStream
import java.io.OutputStream


class MainActivity : AppCompatActivity(){

    private val REQUEST_CODE_ENABLE_BT: Int = 1;
    private val REQUEST_CODE_DISCOVERABLE_BT: Int = 2;
    private val REQUEST_CODE_SPEECH_INPUT: Int = 100;
    private var status: String = "";

    private lateinit var workerThread : Thread;

    //bluetooth streams
    private var outputStream: OutputStream? = null
    private var inStream: InputStream? = null

    //list of paired devices
    private lateinit var pairedDevices: Set<BluetoothDevice>

    //bluetooth adapter
    lateinit var bAdapter: BluetoothAdapter;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //init bluetooth adapter
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bAdapter == null)
            bluetoothStatusTv.text = "Bluetooth is not available";
        else{
            bluetoothStatusTv.text = "Bluetooth is available";
        }
        //set image
        if(bAdapter.isEnabled){
            bluetoothIv.setImageResource(R.drawable.ic_bluetooth_on);
        }
        else{
            bluetoothIv.setImageResource(R.drawable.ic_bluetooth_off);
        }
        //turn on bluetooth
        turnOnBtn.setOnClickListener{
            if(bAdapter.isEnabled){
                //already enabled
                Toast.makeText(this, "Already on", Toast.LENGTH_LONG).show();
            }
            else{
                //turn on bluetooth
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_CODE_ENABLE_BT);
            }
        }
        //turn off bluetooth
        turnOffBtn.setOnClickListener {
            if(!bAdapter.isEnabled){
                Toast.makeText(this, "Already off", Toast.LENGTH_LONG).show();
            }
            else{
                bAdapter.disable();
                bluetoothIv.setImageResource(R.drawable.ic_bluetooth_off)
                Toast.makeText(this, "Bluetooth turned off", Toast.LENGTH_LONG).show();
                outputStream = null;
            }
        }
        //make the bluetooth discoverable
        discoverableBtn.setOnClickListener {
            if(!bAdapter.isDiscovering){
                Toast.makeText(this, "Making your device discoverable", Toast.LENGTH_LONG).show()
                val intent = Intent(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE))
                startActivityForResult(intent, REQUEST_CODE_DISCOVERABLE_BT)
            }
        }
        //connect to device
        pairedBtn.setOnClickListener {
            if(outputStream == null){
                pairedDevices = bAdapter.bondedDevices
                val list : ArrayList<BluetoothDevice> = ArrayList()
                if (pairedDevices.isNotEmpty()) {
                    for (device: BluetoothDevice in pairedDevices) {
                        list.add(device)
                        Log.i("device", ""+device)
                    }
                } else {
                    Toast.makeText(this, "no paired bluetooth devices found", Toast.LENGTH_SHORT).show()
                }

                if(list.isNotEmpty()){
                    val device: BluetoothDevice = list[0]
                    val uuids = device.uuids
                    val socket = device.createRfcommSocketToServiceRecord(uuids[0].uuid)
                    socket.connect()
                    outputStream = socket.outputStream
                    inStream = socket.inputStream
                    Toast.makeText(this, "Connected to: " + device.name, Toast.LENGTH_SHORT).show()
                    listen()
                }
            }

        }
        //set listener for speak button
        speakBtn.setOnClickListener {
            if(outputStream != null){
                speak()
            }
        }
    }

    //voice recognition function
    private fun speak(){
        val mIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        mIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        mIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        mIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hi, speak")
        try{
            startActivityForResult(mIntent, REQUEST_CODE_SPEECH_INPUT)

        }
        catch (e: Exception){
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }

    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        when(requestCode){
            REQUEST_CODE_ENABLE_BT ->
                if(resultCode == Activity.RESULT_OK){
                    bluetoothIv.setImageResource(R.drawable.ic_bluetooth_on)
                    Toast.makeText(this, "Bluetooth is on", Toast.LENGTH_LONG).show()
                }
            else{
                    Toast.makeText(this, "Can't turn on bluetooth", Toast.LENGTH_LONG).show()
                }
            REQUEST_CODE_SPEECH_INPUT ->
                if(resultCode == Activity.RESULT_OK && null != data){
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val message = result?.get(0)
                    if(message.equals("Turn on the lights in the bathroom", true)){
                        status = "01"                    }
                    if(message.equals("Turn off the lights in the bathroom", true)){
                        status = "00"                    }
                    if(message.equals("Turn on the lights in the bedroom", true)){
                        status = "11"                    }
                    if(message.equals("Turn off the lights in the bedroom", true)){
                        status = "10"                    }
                    if(message.equals("Turn on the lights in the kitchen", true)){
                        status = "21"                    }
                    if(message.equals("Turn off the lights in the kitchen", true)){
                        status = "20"                    }
                    if(message.equals("Turn on the lights in the living room", true)){
                        status = "31"                    }
                    if(message.equals("Turn off the lights in the living room", true)){
                        status = "30"                    }
                    if(message.equals("Turn on the lights outside", true)){
                        status = "41"                    }
                    if(message.equals("Turn off the lights outside", true)){
                        status = "40"                    }
                    if(message.equals("Close the window", true)){
                        status = "50"                    }
                    if(message.equals("Open the window", true)){
                        status = "52"                    }
                    if(message.equals("Open the window just a little", true)){
                        status = "51"                    }
                    if(message.equals("Turn on air cooling", true)){
                        status = "61"                    }
                    if(message.equals("Turn off air cooling", true)){
                        status = "60"                    }

                    outputStream!!.write(status.toByteArray())


                }
        }
        super.onActivityResult(requestCode, resultCode, data)

    }

    fun listen() {
        workerThread = Thread {
            val buffer = ByteArray(6) // buffer (our data)

            var bytesCount: Int // amount of read bytes
            var temperature = ""
            var input = ""
            var n = 0;

            while (true) {
                try {
                    if (inStream != null) {
                        bytesCount = inStream!!.read(buffer)
                        if (buffer != null && bytesCount > 0) {
                            input = String(buffer)
                            temperature = temperature.plus(input.take(bytesCount))


                        }
                    }
                } catch (e: IOException) {
                    //error
                }
                if(temperature.length >= 5){
                    val printing = temperature.take(5)
                    runOnUiThread { tempTv.setText("Temperature: " + printing)}
                }

                while (temperature.length >= 5) {
                    temperature = temperature.drop(5)
                }

            }
        }
        workerThread.start()
    }

}