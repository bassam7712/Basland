package com.basland.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var statusText: TextView

    // Package name of the app we'll demo freeze/hide on. Change to any
    // package installed on your test device (must exist inside the profile).
    private val demoTargetPackage = "com.example.targetapp"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, BaslandDeviceAdminReceiver::class.java)

        statusText = findViewById(R.id.statusText)
        updateStatus()

        findViewById<Button>(R.id.btnCreateSpace).setOnClickListener { startProvisioning() }
        findViewById<Button>(R.id.btnDestroySpace).setOnClickListener { openRemoveProfileSettings() }
        findViewById<Button>(R.id.btnFreezeApp).setOnClickListener { setAppSuspended(true) }
        findViewById<Button>(R.id.btnUnfreezeApp).setOnClickListener { setAppSuspended(false) }
        findViewById<Button>(R.id.btnHideApp).setOnClickListener { setAppHidden(true) }
        findViewById<Button>(R.id.btnUnhideApp).setOnClickListener { setAppHidden(false) }
    }

    /**
     * Step 1: Kick off Android's built-in managed-profile provisioning wizard.
     * The system handles creating the profile; onProfileProvisioningComplete()
     * in BaslandDeviceAdminReceiver fires when it's done.
     */
    private fun startProvisioning() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Toast.makeText(this, "Requires Android 5.0+", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                adminComponent
            )
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "This device does not support managed profiles", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Step 2 (teardown): There is no single API call an app can use to delete
     * its own managed profile from Android 8+ without being the profile owner
     * running INSIDE that profile. The reliable, documented path is to send
     * the user to Settings so they (or the profile-owner instance of Basland
     * itself, via wipeData()) remove it explicitly.
     */
    private fun openRemoveProfileSettings() {
        Toast.makeText(
            this,
            "To remove the space: Settings > Accounts > Remove work profile",
            Toast.LENGTH_LONG
        ).show()
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    /**
     * "Freeze" an app: suspend it so it cannot launch or run in the background.
     * Must be called by the app running AS the profile owner (i.e. this same
     * app, once installed inside the managed profile after provisioning).
     */
    private fun setAppSuspended(suspend: Boolean) {
        if (!dpm.isAdminActive(adminComponent)) {
            Toast.makeText(this, "Not an active admin in this profile", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            dpm.setPackagesSuspended(adminComponent, arrayOf(demoTargetPackage), suspend)
            Toast.makeText(
                this,
                if (suspend) "Frozen $demoTargetPackage" else "Unfroze $demoTargetPackage",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * "Hide" an app: remove its launcher icon within the managed profile
     * without uninstalling it.
     */
    private fun setAppHidden(hidden: Boolean) {
        if (!dpm.isAdminActive(adminComponent)) {
            Toast.makeText(this, "Not an active admin in this profile", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            dpm.setApplicationHidden(adminComponent, demoTargetPackage, hidden)
            Toast.makeText(
                this,
                if (hidden) "Hid $demoTargetPackage" else "Unhid $demoTargetPackage",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateStatus() {
        val isAdmin = dpm.isAdminActive(adminComponent)
        val isProfileOwner = dpm.isProfileOwnerApp(packageName)
        statusText.text = buildString {
            append("Device admin active: $isAdmin\n")
            append("Running as profile owner: $isProfileOwner")
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}
