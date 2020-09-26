package com.ciechu.whatisthatinsect.base

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.ciechu.whatisthatinsect.R
import kotlinx.android.synthetic.main.fragment_congrats_dialog_insect.view.*

abstract class BaseDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_congrats_dialog_insect, null)

        changeText(view)

        builder.setView(view)
            .setTitle("Congratulations!")
            .setPositiveButton("OK"){dialogInterface, i ->  }


        return builder.create()

    }

    open fun changeText(view: View) {}
}