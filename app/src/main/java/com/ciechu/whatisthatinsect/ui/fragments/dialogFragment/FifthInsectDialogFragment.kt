package com.ciechu.whatisthatinsect.ui.fragments.dialogFragment

import android.annotation.SuppressLint
import android.view.View
import com.ciechu.whatisthatinsect.base.BaseDialogFragment
import kotlinx.android.synthetic.main.fragment_congrats_dialog_insect.view.*

class FifthInsectDialogFragment : BaseDialogFragment() {

    @SuppressLint("SetTextI18n")
    override fun changeText(view: View) {
        view.dialog_fragment_insect_TV.text = "You found your fifth insect"
        super.changeText(view)
    }
}