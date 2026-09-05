package org.biglau.phone.probe

import android.app.Activity
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

/**
 * places the probe call. debug build only, see [ProbeCall].
 *
 *     adb shell am start -n org.biglau.debug/org.biglau.phone.probe.ProbeActivity
 *
 * incoming only, on purpose: an outgoing probe would go through `TelecomManager.placeCall`,
 * where an extra field decides whether telecom takes the probe or the radio. if that field
 * ever falls away, the device really dials. `addNewIncomingCall` cannot do that: it names the
 * `ConnectionService` and no other one comes into question.
 */
class ProbeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val telecom = getSystemService(TelecomManager::class.java)
        val account = PhoneAccountHandle(
            ComponentName(this, ProbeConnectionService::class.java),
            ProbeCall.ACCOUNT,
        )

        val registered = runCatching {
            telecom.registerPhoneAccount(
                PhoneAccount.builder(account, "BigLau probe call")
                    .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                    .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                    .setShortDescription("a call nobody makes")
                    .build(),
            )
        }
        if (registered.isFailure) {
            report("account cannot be registered: ${registered.exceptionOrNull()}")
            finish()
            return
        }

        val placed = runCatching {
            telecom.addNewIncomingCall(
                account,
                Bundle().apply {
                    putParcelable(
                        TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                        Uri.fromParts("tel", ProbeCall.NUMBER, null),
                    )
                    // adb ... -e klingeln ja - the extra name and its value lie on the device
                    // and stay german, PLAN.md 11.4.
                    putBoolean(ProbeCall.EXTRA_RINGING, intent?.getStringExtra("klingeln") == "ja")
                    putBoolean(ProbeCall.EXTRA_DO_NOT_LOG, true)
                },
            )
        }
        // pick up here and not in the `ConnectionService`. three tries went wrong there:
        // `setDialing()`, `setActive()` at creation, `setActive()` shortly after. telecom
        // holds a call placed through `addNewIncomingCall` on RINGING until somebody
        // **accepts** it - and this is the way meant for that.
        if (placed.isSuccess && intent?.getStringExtra("klingeln") != "ja") {
            android.os.Handler(mainLooper).postDelayed(
                { runCatching { telecom.acceptRingingCall() } },
                600L,
            )
        }
        report(
            if (placed.isSuccess) {
                "probe call placed"
            } else {
                // the most common reason: the account is registered but not enabled. then
                // telecom says nothing, only nothing happens.
                "probe call not placed (enable the account: telecom set-phone-account-enabled)"
            },
        )
        finish()
    }

    /**
     * to the log only, not to the screen.
     *
     * the first version showed a toast - and on a ringing probe it lay exactly over the
     * reject button. a probe that covers one of the two buttons it is about does not check
     * the screen, it obstructs it.
     */
    private fun report(text: String) {
        android.util.Log.i("BigLau", text)
    }
}
