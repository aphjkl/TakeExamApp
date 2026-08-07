package edu.ap.takeexamapp.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.concurrent.atomic.AtomicBoolean

class LocationRepository(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun getCurrentLocation(
        onSuccess: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            onError("Location permission is required.")
            return
        }

        val cancellation = CancellationTokenSource()
        val completed = AtomicBoolean(false)
        val handler = android.os.Handler(context.mainLooper)
        val timeout = Runnable {
            if (completed.compareAndSet(false, true)) {
                cancellation.cancel()
                onError("Location timed out. Check that location is enabled and try again.")
            }
        }
        handler.postDelayed(timeout, 20_000)
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
            .addOnSuccessListener { location ->
                if (completed.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeout)
                    if (location == null) onError("No location was available. Turn on location and try again.")
                    else onSuccess(location)
                }
            }
            .addOnFailureListener { error ->
                if (completed.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeout)
                    onError(error.localizedMessage ?: "Unable to determine the current location.")
                }
            }
    }
}
