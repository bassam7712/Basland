package com.basland.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Basland's DeviceAdminReceiver.
 *
 * This class becomes the "Profile Owner" once the managed (work) profile
 * is provisioned. Android calls onProfileProvisioningComplete() automatically
 * right after the new profile is created — that's where we finish setup
 * (enable the profile and hand control back to the launcher).
 */
class BaslandDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device admin enabled")
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.d(TAG, "Managed profile provisioning complete")

        val dpm = getManager(context)
        val adminComponent = getComponentName(context)

        // Give the new work profile a friendly name.
        dpm.setProfileName(adminComponent, context.getString(R.string.app_name))

        // Turn the freshly-created profile on so its apps become active.
        dpm.setProfileEnabled(adminComponent)
    }

    companion object {
        private const val TAG = "BaslandDeviceAdmin"
    }
}
