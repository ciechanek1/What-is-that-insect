package com.ciechu.whatisthatinsect.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.ciechu.whatisthatinsect.R

class InsectDialogFragment: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_dialog_first_insect, null)

        builder.setView(view)
            .setTitle("Congratulations")
            .setPositiveButton("OK"){dialogInterface, i ->  }

        return builder.create()
    }
}