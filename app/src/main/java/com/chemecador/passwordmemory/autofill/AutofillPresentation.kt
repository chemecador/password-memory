package com.chemecador.passwordmemory.autofill

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.chemecador.passwordmemory.R

/**
 * One row of the autofill dropdown. It is a [RemoteViews] because the row is drawn by the app being
 * filled, in its own process, and only this layout of ours travels there.
 */
internal fun Context.datasetPresentation(
    title: String,
    subtitle: String?,
    requestOrigin: AutofillRequestOrigin
): RemoteViews =
    RemoteViews(packageName, R.layout.autofill_dataset).apply {
        setTextViewText(R.id.autofill_title, title)
        if (subtitle.isNullOrEmpty()) {
            setViewVisibility(R.id.autofill_subtitle, View.GONE)
        } else {
            setTextViewText(R.id.autofill_subtitle, subtitle)
        }
        val missingValue = getString(R.string.autofill_security_missing_value)
        setTextViewText(
            R.id.autofill_domain,
            getString(
                R.string.autofill_security_domain_value,
                requestOrigin.claimedWebDomain ?: missingValue
            )
        )
        setTextViewText(
            R.id.autofill_package,
            getString(
                R.string.autofill_security_package_value,
                requestOrigin.requestingPackage ?: missingValue
            )
        )
    }
