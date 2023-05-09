package com.shist.ui.fragments

import android.app.Dialog
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.shist.ui.databinding.MapBinding
import com.shist.ui.viewModels.MapViewModel
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.locationcomponent.location
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.appcompat.content.res.AppCompatResources
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.shist.domain.BuildingItem
import com.shist.ui.MainActivity
import com.shist.ui.R
import com.shist.ui.adapters.DepartmentsListAdapter
import com.shist.ui.databinding.HistBuildInfoBinding
import com.shist.ui.databinding.ModernBuildInfoBinding
import com.shist.ui.mapbox.LocationPermissionHelper
import com.shist.ui.viewModels.LoadState
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.style.expressions.dsl.generated.color
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import androidx.appcompat.app.AlertDialog
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.style.expressions.Expression.raw
import com.mapbox.maps.plugin.annotation.generated.*

class MapFragment : Fragment(), KoinComponent {

    private val viewModel: MapViewModel by inject()

    // View of whole map
    private var _binding: MapBinding? = null
    private val binding get() = _binding!!

    // View of dialog window emerging while clicking on icon of modern building
    private var _modernIconBinding: ModernBuildInfoBinding? = null
    private val modernIconBinding get() = _modernIconBinding!!

    // View of dialog window emerging while clicking on icon of historical building
    private var _histIconBinding: HistBuildInfoBinding? = null
    private val histIconBinding get() = _histIconBinding!!

    private lateinit var mapView: MapView

    private lateinit var locationPermissionHelper: LocationPermissionHelper

    private lateinit var annotationApi : AnnotationPlugin
    private lateinit var pointAnnotationManager : PointAnnotationManager
    private lateinit var viewAnnotationManager : ViewAnnotationManager

    private val asyncInflater by lazy { AsyncLayoutInflater(requireContext()) }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MapBinding.inflate(inflater, container, false)
        _modernIconBinding = ModernBuildInfoBinding.inflate(inflater, container, false)
        _histIconBinding = HistBuildInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = binding.mapView

        annotationApi = mapView.annotations
        pointAnnotationManager = annotationApi.createPointAnnotationManager()
        viewAnnotationManager = mapView.viewAnnotationManager

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)

        mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) { // After the style is loaded, initialize the Location component.
            mapView.location.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
        }






        // Инициализация списка маркеров
        val markerInfoList = listOf(
            MarkerInfo(Point.fromLngLat(36.197699, 51.732999), R.mipmap.ic_launcher_sciencepath_icon_round, "1", getString(R.string.desc_1), R.mipmap.ic_launcher_1_foreground),
            MarkerInfo(Point.fromLngLat(36.229785, 49.990134), R.mipmap.ic_launcher_sciencepath_icon_round, "2", getString(R.string.desc_2), R.mipmap.ic_launcher_2_foreground),
            MarkerInfo(Point.fromLngLat(37.531905, 55.702853), R.mipmap.ic_launcher_sciencepath_icon_round, "3", getString(R.string.desc_3), R.mipmap.ic_launcher_3_foreground),
            MarkerInfo(Point.fromLngLat(32.046175, 54.784286), R.mipmap.ic_launcher_sciencepath_icon_round, "4", getString(R.string.desc_4), R.mipmap.ic_launcher_4_foreground),
            MarkerInfo(Point.fromLngLat(27.545617, 53.893074), R.mipmap.ic_launcher_sciencepath_icon_round, "5", getString(R.string.desc_5), R.mipmap.ic_launcher_5_foreground),
            MarkerInfo(Point.fromLngLat(30.216813, 55.186595), R.mipmap.ic_launcher_sciencepath_icon_round, "6", getString(R.string.desc_6), R.mipmap.ic_launcher_6_foreground),
            MarkerInfo(Point.fromLngLat(37.617892, 55.755255), R.mipmap.ic_launcher_sciencepath_icon_round, "7", getString(R.string.desc_7), R.mipmap.ic_launcher_7_foreground),
            MarkerInfo(Point.fromLngLat(27.545257, 53.894619), R.mipmap.ic_launcher_sciencepath_icon_round, "8", getString(R.string.desc_8), R.mipmap.ic_launcher_8_foreground),
            MarkerInfo(Point.fromLngLat(27.548418, 53.893214), R.mipmap.ic_launcher_sciencepath_icon_round, "9", getString(R.string.desc_9), R.mipmap.ic_launcher_9_foreground),
            MarkerInfo(Point.fromLngLat(37.617626, 55.761749), R.mipmap.ic_launcher_sciencepath_icon_round, "10", getString(R.string.desc_10), R.mipmap.ic_launcher_10_foreground),
            MarkerInfo(Point.fromLngLat(27.548613, 53.896553), R.mipmap.ic_launcher_sciencepath_icon_round, "11", getString(R.string.desc_11), R.mipmap.ic_launcher_11_foreground),
            MarkerInfo(Point.fromLngLat(36.191618, 51.735182), R.mipmap.ic_launcher_sciencepath_icon_round, "12", getString(R.string.desc_12), R.mipmap.ic_launcher_12_foreground),
            MarkerInfo(Point.fromLngLat(53.219791, 56.848936), R.mipmap.ic_launcher_sciencepath_icon_round, "13", getString(R.string.desc_13), R.mipmap.ic_launcher_13_foreground),
            MarkerInfo(Point.fromLngLat(49.110196, 55.794575), R.mipmap.ic_launcher_sciencepath_icon_round, "14", getString(R.string.desc_14), R.mipmap.ic_launcher_14_foreground),
            MarkerInfo(Point.fromLngLat(69.286078, 41.294072), R.mipmap.ic_launcher_sciencepath_icon_round, "15", getString(R.string.desc_15), R.mipmap.ic_launcher_15_foreground),
            MarkerInfo(Point.fromLngLat(37.295731, 55.951772), R.mipmap.ic_launcher_sciencepath_icon_round, "16", getString(R.string.desc_16), R.mipmap.ic_launcher_16_foreground),
            MarkerInfo(Point.fromLngLat(27.598632, 53.920631), R.mipmap.ic_launcher_sciencepath_icon_round, "17", getString(R.string.desc_17), R.mipmap.ic_launcher_17_foreground),
            MarkerInfo(Point.fromLngLat(27.566725, 53.899873), R.mipmap.ic_launcher_sciencepath_icon_round, "18", getString(R.string.desc_18), R.mipmap.ic_launcher_18_foreground),
            MarkerInfo(Point.fromLngLat(-122.421307, 37.780097), R.mipmap.ic_launcher_sciencepath_icon_round, "19", getString(R.string.desc_19), R.mipmap.ic_launcher_19_foreground),
            MarkerInfo(Point.fromLngLat(28.991350, 53.835975), R.mipmap.ic_launcher_sciencepath_icon_round, "20", getString(R.string.desc_20), R.mipmap.ic_launcher_20_foreground),
            MarkerInfo(Point.fromLngLat(27.561522, 53.902537), R.mipmap.ic_launcher_sciencepath_icon_round, "21", getString(R.string.desc_21), R.mipmap.ic_launcher_21_foreground),
        )

        for (markerInfo in markerInfoList) {
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(markerInfo.point)
                .withIconImage(
                    ContextCompat.getDrawable(requireContext(), markerInfo.iconResId)?.toBitmap()!!
                )
                .withTextField(markerInfo.title)
                .withTextOffset(listOf<Double>(0.0, 1.5.toDouble()))
                .withTextColor("black")
            val pointAnnotation = pointAnnotationManager.create(pointAnnotationOptions)
            pointAnnotationManager.addClickListener { clickedPoint ->
                if (clickedPoint.featureIdentifier == pointAnnotation.featureIdentifier) {
                    showMarkerInfoWindow(markerInfo)
                    true
                } else {
                    false
                }
            }
        }











        locationPermissionHelper = LocationPermissionHelper(WeakReference(requireActivity()))
        locationPermissionHelper.checkPermissions {
            onMapReady()
        }

        var dataList: List<BuildingItem> = emptyList()

        // Getting data from local database (if there are some)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Periodically check the block below...
                viewModel.dataFlow.collect { // If there are some changes at viewModel.dataFlow, then block below will run...
                    dataList = it
                    setMarkers(dataList)
                }
            }
        }

        pointAnnotationManager.addClickListener { clickedPoint -> // What will map do if user click on some icon's point
            val iconView =
                viewAnnotationManager.getViewAnnotationByFeatureId(clickedPoint.featureIdentifier)
            iconView?.toggleViewVisibility()
            val isSelected =
                viewAnnotationManager.getViewAnnotationOptionsByFeatureId(clickedPoint.featureIdentifier)?.selected
            if (iconView != null) {
                viewAnnotationManager.updateViewAnnotation(
                    iconView,
                    viewAnnotationOptions {
                        if (isSelected == true)
                            selected(false)
                        else
                            selected(true)
                    }
                )
            }
            true
        }

        val timeLength = if (dataList.isEmpty()) // If no data in general (even in database)
            Snackbar.LENGTH_INDEFINITE
        else { // If no NEW data, but we have OLD data in database
            Snackbar.LENGTH_LONG
        }

        // Trying to get actual data about buildings from server
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect {
                    when (it) {
                        LoadState.LOADING -> {
                            createSnackbar(resources.getString(R.string.loading),
                                requireContext().getColor(R.color.black))
                        }
                        LoadState.SUCCESS -> {
                            createSnackbar(resources.getString(R.string.loadingSuccess),
                                requireContext().getColor(R.color.black))
                        }
                        LoadState.INTERNET_ERROR -> {
                            createSnackbarWithReload(timeLength,
                                resources.getString(R.string.errorNetwork))
                        }
                        LoadState.UNKNOWN_ERROR -> {
                            createSnackbarWithReload(timeLength,
                                resources.getString(R.string.errorUnknownNoData))
                        }
                        LoadState.EMPTY_DATA_ERROR -> {
                            createSnackbarWithReload(timeLength,
                                resources.getString(R.string.errorNoNewsOnAPI))
                        }
                        else -> createSnackbar(resources.getString(R.string.launching),
                            requireContext().getColor(R.color.black))
                    }
                }
            }
        }

        if (savedInstanceState == null && viewModel.state.value == LoadState.IDLE) {
            viewModel.loadData() // Loading new data about building from server if we are launching for the first time...
        }
    }

    private fun showMarkerInfoWindow(markerInfo: MarkerInfo) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.marker_info_window)
        dialog.findViewById<TextView>(R.id.title_textview)?.text = markerInfo.title
        dialog.findViewById<TextView>(R.id.description_textview)?.text = markerInfo.description
        dialog.findViewById<ImageView>(R.id.imageview)?.setImageResource(markerInfo.imageResId)
        dialog.findViewById<Button>(R.id.close_button)?.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    data class MarkerInfo(
        val point: Point,
        val iconResId: Int,
        val title: String,
        val description: String,
        val imageResId: Int // новое поле для изображения
    )


    private fun setMarkers(itemsList: List<BuildingItem>) {
        for (item in itemsList) {
            if (item.address == null)
                continue
            // Here (in the future) it is worth adding cases for other types of buildings (default - historical)
            val buildingType = when (item.type) {
                "историческое" -> R.drawable.ic_marker_historical
                "учебное" -> R.drawable.ic_marker_uchebnoye
                "общежитие" -> R.drawable.ic_marker_obsezhitie
                "административное" -> R.drawable.ic_marker_admin
                "многофункциональное" -> R.drawable.ic_marker_mnogofunc
                else -> R.drawable.ic_marker_historical
            }
            val point = Point.fromLngLat(
                item.address?.longitude?.toDouble()!!,
                item.address?.latitude?.toDouble()!!)
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(ContextCompat.getDrawable(requireContext(), buildingType)?.toBitmap()!!)
            if (isPointAlreadyExists(point)) {
                continue // If point with such longitude and latitude already exists then just skip her
            }
            val pointAnnotation = pointAnnotationManager.create(pointAnnotationOptions)
            Log.d("POINT_ANNOTATION_ADDED", "ADDED POINT WITH ID : " + pointAnnotation.featureIdentifier)
            if (viewAnnotationManager.getViewAnnotationByFeatureId(pointAnnotation.featureIdentifier) == null) { // Add annotation of view of the point (if there is no yet)
                if (item.type == "историческое") { // It's better to replace with !isModern, but on our server all isModern are set to false...
                    viewAnnotationManager.addViewAnnotation(
                        resId = R.layout.hist_build_info,
                        options = viewAnnotationOptions {
                            geometry(point)
                            associatedFeatureId(pointAnnotation.featureIdentifier)
                            anchor(ViewAnnotationAnchor.BOTTOM)
                            selected(false)
                            allowOverlap(false)
                            color(requireContext().getColor(R.color.bsu))
                        },
                        asyncInflater = asyncInflater
                    ) { viewAnnotation ->
                        viewAnnotation.visibility = View.GONE
                        // calculate offsetY manually taking into account icon height only because of bottom anchoring
                        viewAnnotationManager.updateViewAnnotation(
                            viewAnnotation,
                            viewAnnotationOptions {
                                //offsetY(*(pointAnnotation.iconImageBitmap?.height!!).toInt())
                            }
                        )
                        HistBuildInfoBinding.bind(viewAnnotation).apply {
                            title.text = item.name
                            btnSeeDetails.setOnClickListener {
                                val myActivity = requireActivity() as MainActivity
                                myActivity.onHistoricalBuildingClick(item, item.imagesList)
                            }
                            btnCreateRoute.setOnClickListener {
                                createSnackbar(resources.getString(R.string.future_feature),
                                    requireContext().getColor(R.color.black))
                                // TODO вызвать onClick() из MainActivity для маршрута
                            }
                            btnSee3dModel.setOnClickListener {
                                createSnackbar(resources.getString(R.string.future_feature),
                                    requireContext().getColor(R.color.black))
                                // TODO вызвать onClick() из MainActivity для 3D модели
                            }
                        }
                    }
                } else {
                    viewAnnotationManager.addViewAnnotation(
                        resId = R.layout.modern_build_info,
                        options = viewAnnotationOptions {
                            geometry(point)
                            associatedFeatureId(pointAnnotation.featureIdentifier)
                            anchor(ViewAnnotationAnchor.BOTTOM)
                            selected(false)
                            allowOverlap(false)
                            color(requireContext().getColor(R.color.bsu))
                        },
                        asyncInflater = asyncInflater
                    ) { viewAnnotation ->
                        viewAnnotation.visibility = View.GONE
                        // calculate offsetY manually taking into account icon height only because of bottom anchoring
                        viewAnnotationManager.updateViewAnnotation(
                            viewAnnotation,
                            viewAnnotationOptions {
                                //offsetY((pointAnnotation.iconImageBitmap?.height!!).toInt())
                            }
                        )
                        ModernBuildInfoBinding.bind(viewAnnotation).apply {
                            title.text = item.name
                            if (item.structuralObjects.isNullOrEmpty()) {
                                emptyDepartmentsLabel.isGone = false
                                recyclerView.isGone = true
                            } else {
                                emptyDepartmentsLabel.isGone = true
                                recyclerView.isGone = false
                                val recyclerView = recyclerView
                                recyclerView.layoutManager = GridLayoutManager(context, 1)
                                val adapter = DepartmentsListAdapter(item.imagesList,
                                    requireActivity() as MainActivity,
                                    binding,
                                    requireContext())
                                recyclerView.adapter = adapter
                                adapter.submitList(item.structuralObjects)
                            }
                        }
                    }
                }
                Log.d("VIEW_ANNOTATION_ADDED", "ADDED VIEW FOR POINT WITH ID : " + pointAnnotation.featureIdentifier)
            }
        }
    }

    private fun isPointAlreadyExists(point: Point) : Boolean {
        // Returns true if point already exists at PointManager and false otherwise
        return pointAnnotationManager.annotations.find {
            it.geometry.longitude() == point.longitude() && it.geometry.latitude() == point.latitude()
        } != null
    }

    private fun View.toggleViewVisibility() {
        visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun onMapReady() {
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(14.0)
                .build()
        )
        mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            initLocationComponent()
            setupGesturesListener()
        }
    }

    private fun setupGesturesListener() {
        mapView.gestures.addOnMoveListener(onMoveListener)
    }

    private fun initLocationComponent() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.mapbox_user_puck_icon,
                ),
                shadowImage = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.mapbox_user_icon_shadow,
                ),
                scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
    }

    private fun onCameraTrackingDismissed() {
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    private fun createSnackbar(message: String, color: Int) {
        var timeLength = BaseTransientBottomBar.LENGTH_LONG
        if (message == getString(R.string.loading))
            timeLength = BaseTransientBottomBar.LENGTH_INDEFINITE
        val snackbar = Snackbar.make(
            binding.mapContainer,
            message,
            timeLength
        )
        snackbar.setTextColor(color)
        snackbar.show()
    }

    private fun createSnackbarWithReload(snackbarTimeLength: Int, messageError: String) {
        val snackbar = Snackbar.make(
            binding.mapContainer,
            messageError,
            snackbarTimeLength
        )
        snackbar.setTextColor(requireContext().getColor(R.color.black))
        snackbar.setActionTextColor(requireContext().getColor(R.color.black))
        snackbar.setAction(R.string.reload) {
            viewModel.loadData()
        }
        snackbar.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @Override
    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    @Override
    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    @Override
    override fun onDestroy() {
        super.onDestroy()
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    @Override
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _modernIconBinding = null
        _histIconBinding = null
    }

    @Override
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

}