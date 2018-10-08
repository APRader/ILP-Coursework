package com.example.s1611382.ilp

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.DrawableWrapper
import android.location.Location
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBar
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.example.s1611382.ilp.MainActivity.DownloadCompleteRunner.result
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core. permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity(), PermissionsListener, LocationEngineListener, OnMapReadyCallback {

    private val tag = "MainActivity"
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var permissionManager: PermissionsManager
    //stores current location at all times
    private lateinit var originLocation: Location
    //contains GeoJson features
    private lateinit var coinCollection: FeatureCollection
    private lateinit var features: List<Feature>

    //gives user location
    private lateinit var locationEngine: LocationEngine
    //for UI: icon representing user location
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //need toolbar for app nav drawer button
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        Mapbox.getInstance(this, getString(R.string.access_token))
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        // this makes map variable usable in the rest of the class
        mapView?.getMapAsync(this)

        mDrawerLayout = findViewById(R.id.drawer_layout)

        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            // set item as selected to persist highlight
            menuItem.isChecked = true
            // close drawer when item is tapped
            mDrawerLayout.closeDrawers()

            // Add code here to update the UI based on the item selected
            // For example, swap UI fragments here
            if (menuItem.itemId == R.id.nav_wallet) {
                openWallet()
            }

            true
        }

        val fab: View = findViewById(R.id.fab)
        fab.setOnClickListener {
            setCameraPosition(originLocation)
        }

    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            Log.d(tag, "[onMapReady] mapBoxMap is null")
        } else {
            map = mapboxMap
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true

            // Make location information available
            enableLocation()

            val task = DownloadFileTask(DownloadCompleteRunner)
            val result = task.execute("http://homepages.inf.ed.ac.uk/stg/coinz/2018/10/03/coinzmap.geojson").get()
            drawCoinLocations(result)
        }
    }

    //get user's permission for location
    private fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine()
            initializeLocationLayer()
        } else {
            Log.d(tag, "Permissions are not granted")
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(this)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initializeLocationEngine() {
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine.addLocationEngineListener(this)
        locationEngine.apply {
            interval = 5000
            fastestInterval = 1000
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }

        val lastLocation = locationEngine.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } //else { locationEngine.addLocationEngineListener(this) }
    }

    private fun initializeLocationLayer() {
        if (mapView == null) { Log.d(tag, "mapView is null") }
        else {
            if (map == null) { Log.d(tag, "map is null") }
            else {
                locationLayerPlugin = LocationLayerPlugin(mapView!!, map!!, locationEngine)
                //for camera tracking location
                locationLayerPlugin.apply {
                    setLocationLayerEnabled(true)
                    cameraMode = CameraMode.TRACKING
                    renderMode = RenderMode.NORMAL
                }
            }
        }
    }

    private fun setCameraPosition(location: Location) {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude), 14.5))
    }

    private fun openWallet() {
        val intent = Intent(this, Wallet::class.java)
        startActivity(intent)
    }

    private fun drawCoinLocations(JsonFile : String?) {

        coinCollection = FeatureCollection.fromJson(JsonFile.toString())

        features = coinCollection.features() as List<Feature>

        for (f: Feature in features) {
            if (f.geometry() is Point) {
                val coordinates = (f.geometry() as Point).coordinates()
                map?.addMarker(
                        MarkerOptions().position(LatLng(coordinates[1], coordinates[0]))
                )
            }

        }
    }

    interface DownloadCompleteListener {
        fun downloadComplete(result: String)
    }

    object DownloadCompleteRunner : DownloadCompleteListener {
        var result : String? = null
        override fun downloadComplete(result: String) {
            this.result = result

        }
    }

    class DownloadFileTask(private val caller : DownloadCompleteListener):
            AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg urls: String): String = try {
            loadFileFromNetwork(urls[0])
        } catch (e: IOException) {
            "Unable to load content. check your network connection"
        }

        private fun loadFileFromNetwork(urlString: String): String {
            val stream: InputStream = downloadUrl(urlString)
            // Read input from stream, build result as a string
            result = stream.bufferedReader().use { it.readText() }
            return result.toString()
        }


        //Given a string representation of a URL, sets up a connection and gets an input stream.
        @Throws(IOException::class)
        private fun downloadUrl(urlString: String): InputStream {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 10000 // milliseconds
            conn.connectTimeout = 15000 // milliseconds
            conn.requestMethod = "GET"
            conn.doInput = true
            conn.connect() // Starts the query
            return conn.inputStream
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)

            caller.downloadComplete(result)
        }
    }


    //open the drawer when nav button is tapped
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                mDrawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else ->super.onOptionsItemSelected(item)
        }
    }

    //-- from PermissionsListener --//
    //when user denies permission
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        /*seems to trigger when location previously not granted when app opens
        but doesn't trigger when first denied.
        Hence warning message built into onPermissionResult function*/
    }
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocation()
        } else {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Location permission not granted")
            builder.setMessage("You denied permission to use location. " +
                    "Without that information you cannot collect new coins. " +
                    "Please accept access to location when you reopen the app " +
                    "so you can enjoy Coinz to the fullest extent!")
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //-- from LocationEngineListener --//
    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            Log.d(tag, "[onLocationChanged] location is null")
        } else {
            originLocation = location
            //setCameraPosition(originLocation)

            //check if player is near a coin
            for (f: Feature in features) {
                if (f.geometry() is Point) {
                    val coordinates = (f.geometry() as Point).coordinates()
                    val coinLocation = Location("")
                    coinLocation.latitude = coordinates[1]
                    coinLocation.longitude = coordinates[0]
                    //player is within 25 metres of the coin
                    if (location.distanceTo(coinLocation) <= 25) {
                        val id = f.properties()?.get("id")

                        val builder = AlertDialog.Builder(this@MainActivity)
                        builder.setTitle("Near a coin")
                        builder.setMessage("You're near coin $id")
                        val dialog: AlertDialog = builder.create()
                        dialog.show()
                    }
                }

            }

        }
    }
    @SuppressLint("MissingPermission")
    override fun onConnected() {
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine.requestLocationUpdates()
    }


    // lifecycle methods for MapBox override standard funs
    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }
    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }
    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }
    override fun onStop() {
        super.onStop()
        locationEngine.removeLocationUpdates()
        locationLayerPlugin.onStop()
        mapView?.onStop()
    }
    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
        locationEngine.deactivate()
    }
    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mapView?.onSaveInstanceState(outState)
        }
    }
    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }
}
