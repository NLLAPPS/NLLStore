package com.nll.store.ui

import android.view.View
import com.google.android.material.snackbar.Snackbar

object SnackProvider {

    fun provideDefaultSnack(root: View, anchorView: View? = null, snackText: String, snackActionText: String? = null, durationMilliSeconds: Int = Snackbar.LENGTH_INDEFINITE, snackClickListener: ClickListener): Snackbar {
        return getBaseSnack(root, anchorView, snackText, snackActionText, durationMilliSeconds, snackClickListener)

    }

    private fun getBaseSnack(root: View, anchorView: View?, snackText: String, snackActionText: String?, duration: Int, snackClickListener: ClickListener): Snackbar {
        val snack = Snackbar.make(root, snackText, duration).apply {
            view.setOnClickListener {
                snackClickListener.onSnackViewClick()
                dismiss()
            }
            snackActionText?.let {
                setAction(snackActionText) {
                    snackClickListener.onActionClick()
                }
            }
        }
        anchorView?.let {
            snack.setAnchorView(it)
        }
        return snack
    }


    interface ClickListener {
        fun onActionClick()
        fun onSnackViewClick()
    }

    //When we don't need to handle Action Click
    fun interface ViewClickListener : ClickListener {
        override fun onActionClick() {
            //unused
        }

    }

    //When we don't need to handle View Click
    fun interface ActionClickListener : ClickListener {
        override fun onSnackViewClick() {
            //unused
        }

    }
}