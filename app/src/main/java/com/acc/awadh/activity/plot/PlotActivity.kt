package com.acc.awadh.activity.plot

import android.bluetooth.le.ScanResult
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.acc.awadh.R
import com.acc.awadh.databinding.ActivityPlotBinding
import com.acc.awadh.ble.BLEManager.registerScanResultListener
import com.acc.awadh.ble.BLEManager.unregisterScanResultListener
import com.acc.awadh.ble.ScanResultListener


class PlotActivity : AppCompatActivity(), ScanResultListener {

    private lateinit var binding: ActivityPlotBinding
    private lateinit var xChart: LineChart
    private lateinit var yChart: LineChart
    private lateinit var zChart: LineChart

    private var deviceAddress: String? = null

    private val xEntries = ArrayList<Entry>()
    private val yEntries = ArrayList<Entry>()
    private val zEntries = ArrayList<Entry>()
    private var time = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_plot)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        xChart = binding.xChart
        yChart = binding.yChart
        zChart = binding.zChart

        // Initialize charts
        initChart(xChart)
        initChart(yChart)
        initChart(zChart)

        // Set custom marker
        val markerX = CustomMarkerView(this, R.layout.marker_view_humid)
        xChart.marker = markerX
        val markerY = CustomMarkerView(this, R.layout.marker_view_temp)
        yChart.marker = markerY

        zChart.marker = markerY

        deviceAddress = intent.getStringExtra("DEVICE_ADDRESS")

        // Register this activity as a listener
        registerScanResultListener(this)

        // Get data from the intent
        val xData = intent.getFloatArrayExtra("X_DATA")
        val yData = intent.getFloatArrayExtra("Y_DATA")
        val zData = intent.getFloatArrayExtra("Z_DATA")

        xData?.let {
            for (i in it.indices) {
                addEntryToChart(xChart, xEntries, time + i , it[i], "#FF0000") // Red for X
            }
        }

        yData?.let {
            for (i in it.indices) {
                addEntryToChart(yChart, yEntries, time + i , it[i], "#FFFF00") // Yellow for Y
            }
        }

        zData?.let {
            for (i in it.indices) {
                addEntryToChart(zChart, zEntries, time + i , it[i], "#0000FF") // Blue for Z
            }
        }
    }

    override fun onScanResultUpdated(result: ScanResult){
        if (result.device.address == deviceAddress) {

            // Access the byte array from the scan record
            val bytes = result.scanRecord?.bytes ?: byteArrayOf()

            // Extract the float values from the byte array
            val X = bytes.getOrNull(3)?.toFloat()
            val Y = bytes.getOrNull(4)?.toFloat()
            val Z = bytes.getOrNull(5)?.toFloat()

            if (X != null && Y != null && Z != null) {
                addNewData(X, Y, Z)
            }
        }
    }
    private fun initChart(chart: LineChart) {
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)

        // Disable grid lines and axis labels
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.setDrawGridLines(false)

        // Disable legend
        chart.legend.isEnabled = false


    }

    private fun addEntryToChart(chart: LineChart, entries: ArrayList<Entry>, x: Float, y: Float, color: String) {
        entries.add(Entry(x, y))

        // Check if the number of entries exceeds 30
        if (entries.size > 30) {
            entries.removeAt(0) // Remove the oldest entry
        }

        val dataSet = LineDataSet(entries, when (chart) {
            xChart -> "X"
            yChart -> "Y"
            else -> "Z"
        })

        dataSet.setDrawValues(false)  // Disable values on data points

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM

        dataSet.color = Color.parseColor(color) // Line color
        dataSet.setCircleColor(Color.parseColor(color)) // Point color
        dataSet.circleRadius = 5f // Point radius
        dataSet.setDrawCircleHole(false) // Filled circles
        dataSet.highLightColor = Color.parseColor(color) // Highlight color
        dataSet.setDrawHighlightIndicators(true) // Draw highlight indicators
        dataSet.lineWidth = 2f

        // Enable fill and set fill color
        dataSet.setDrawFilled(true)
        dataSet.fillColor = when (chart) {
            xChart -> resources.getColor(R.color.x_fill, null)
            yChart -> resources.getColor(R.color.y_fill, null)
            else -> resources.getColor(R.color.z_fill, null)
        }

        dataSet.valueTextSize = 0f // Hide value text

        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.invalidate() // Refresh the chart
    }

    override fun onSupportNavigateUp(): Boolean {
        // Unregister the listener to avoid memory leaks
        unregisterScanResultListener(this)
        onBackPressed()
        return true
    }

    fun addNewData(x: Float, y: Float, z: Float) {
        time += 1
        addEntryToChart(xChart, xEntries, time, x, "#FF0000") // Red for X
        addEntryToChart(yChart, yEntries, time, y, "#FFFF00") // Yellow for Y
        addEntryToChart(zChart, zEntries, time, z, "#0000FF") // Blue for Z
    }


}
