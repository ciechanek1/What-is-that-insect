package com.ciechu.whatisthatinsect

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.ciechu.whatisthatinsect.viewmodels.InsectViewModel
import org.koin.android.ext.android.inject

class FirstInsectDialogFragment: DialogFragment() {

    private val insectViewModel: InsectViewModel by inject()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_dialog_first_insect, null)

        builder.setView(view)
            .setTitle("Popopopop")
            .setPositiveButton("OK"){dialogInterface, i ->  }

        return builder.create()
    }
}