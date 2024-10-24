package com.acc.awadh.activity.scan.fragment

import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Bundle
import android.content.Intent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.view.*
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.acc.awadh.R
import com.acc.awadh.activity.plot.PlotActivity
import com.acc.awadh.ble.BLEManager.registerScanResultListener
import com.acc.awadh.ble.BLEManager.unregisterScanResultListener
import com.acc.awadh.ble.ScanResultListener
import com.acc.awadh.databinding.FragmentAdvertisingDataBinding
import timber.log.Timber

class AdvertisingDataFragment: DialogFragment(), ScanResultListener {

    private var deviceAddress: String? = "not find"

    // Variable to store the timestamp of the last update
    private var lastUpdateTime: Long = 0

    companion object {
        private const val ARG_DEVICE_ADDRESS = "device_address"

        fun newInstance(deviceAddress: String): AdvertisingDataFragment {
            val fragment = AdvertisingDataFragment()
            val args = Bundle()
            args.putString(ARG_DEVICE_ADDRESS, deviceAddress)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceAddress = arguments?.getString(ARG_DEVICE_ADDRESS)
    }

    private lateinit var binding: FragmentAdvertisingDataBinding

    private val xData = mutableListOf<Float>()
    private val yData = mutableListOf<Float>()
    private val zData = mutableListOf<Float>()


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentAdvertisingDataBinding>(
            inflater,
            R.layout.fragment_advertising_data,
            container,
            false
        )

        // Register this fragment as a listener
        registerScanResultListener(this)

        // Set up button click listener
        binding.openPlotActivityButton.setOnClickListener {
            openPlotActivity()
        }

        binding.okButton.setOnClickListener {
            dismiss()
        }
        // Return the root view of the binding
        return binding.root
    }


    private fun openPlotActivity() {
        val intent = Intent(requireContext(), PlotActivity::class.java)
        intent.putExtra("X_DATA", xData.toFloatArray())
        intent.putExtra("Z_DATA", zData.toFloatArray())
        intent.putExtra("Y_DATA", yData.toFloatArray())
        intent.putExtra("DEVICE_ADDRESS", deviceAddress)
        startActivity(intent)
    }

    // Implement the interface method
    override fun onScanResultUpdated(result: ScanResult) {
        Timber.d("Update UI request recieved")
        val currentTime = System.currentTimeMillis()
        val timeDifference = if (lastUpdateTime == 0L) 0 else currentTime - lastUpdateTime

        // Update the last update time
        lastUpdateTime = currentTime

        // Update the UI with the new ScanResult
        updateUI(result, timeDifference)
    }

    private fun updateUI(result: ScanResult, timeDifference: Long) {

        if (result.device.address == deviceAddress) {

// Access the byte array from the scan record
            val bytes = result.scanRecord?.bytes ?: byteArrayOf()

// Log the raw byte array for debugging
            Timber.d("Raw byte array: %s", bytes.joinToString())

// Extract values if the array has the required length
            val deviceID = bytes.getOrNull(2)?.toInt() // Assuming device ID is a single byte
            val X = bytes.getOrNull(3)?.toFloat()
            val Y = bytes.getOrNull(4)?.toFloat()
            val Z = bytes.getOrNull(5)?.toFloat()

            // Add the values to the lists if they are not null
            X?.let { xData.add(it) }
            Y?.let { yData.add(it) }
            Z?.let { zData.add(it) }


            // Update the UI elements
            requireActivity().runOnUiThread {
                binding.Byte0Text.text = deviceAddress
                binding.Byte2Text.text = X?.toString() ?: ""
                binding.Byte3Text.text = Y?.toString() ?: ""
                binding.Byte4Text.text = Z?.toString() ?: ""
                binding.Byte1Text.text = deviceID?.toString() ?: ""
                binding.Byte5Text.text = "$timeDifference ms"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister the listener to avoid memory leaks
        unregisterScanResultListener(this)
    }

    override fun onResume() {
        super.onResume()
        // Set Fragment Dimensions
        val width = WindowManager.LayoutParams.MATCH_PARENT
        val height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog?.window?.setLayout(width, height)
    }
}