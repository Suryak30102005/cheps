package com.example.udyam.Seller.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.udyam.databinding.FragmentSellerHomeBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

/**
 * A fragment that displays the seller's home screen.
 *
 * This fragment shows a dashboard with key metrics like total sales, pending orders,
 * and delivered orders. It also displays a line chart showing the sales trend
 * for the week.
 */
class SellerHomeFragment : Fragment() {

    private var _binding: FragmentSellerHomeBinding? = null
    private val binding get() = _binding!!

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called immediately after [.onCreateView] has returned, but before any
     * saved state has been restored in to the view.
     *
     * @param view The View returned by [.onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Update dummy card values
        binding.textTotalSalesValue.text = "₹12,500"
        binding.textOrdersPendingValue.text = "30"
        binding.textOrdersDeliveredValue.text = "27"

        // Set up the graph
        setupSalesGraph()
    }

    /**
     * Sets up the sales graph with sample data.
     */
    private fun setupSalesGraph() {
        // Sample data for each day of the week (Mon to Sun)
        val entries = listOf(
            Entry(0f, 12f),  // Monday
            Entry(1f, 18f),  // Tuesday
            Entry(2f, 7f),   // Wednesday
            Entry(3f, 14f),  // Thursday
            Entry(4f, 20f),  // Friday
            Entry(5f, 10f),  // Saturday
            Entry(6f, 16f)   // Sunday
        )

        val lineDataSet = LineDataSet(entries, "Orders This Week").apply {
            color = Color.parseColor("#3461FD")
            valueTextColor = Color.BLACK
            lineWidth = 2f
            circleRadius = 5f
            setCircleColor(Color.parseColor("#3461FD"))
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(true)
        }

        val lineData = LineData(lineDataSet)
        binding.chartSalesGraph.apply {
            data = lineData
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            animateX(1000)

            // X Axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                valueFormatter = IndexAxisValueFormatter(
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                )
            }

            // Y Axis
            axisRight.isEnabled = false
            axisLeft.granularity = 1f

            invalidate()
        }
    }

    /**
     * Called when the view previously created by [.onCreateView] has
     * been detached from the fragment.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
