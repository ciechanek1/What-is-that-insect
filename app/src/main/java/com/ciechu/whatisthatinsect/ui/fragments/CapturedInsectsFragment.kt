package com.ciechu.whatisthatinsect.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ciechu.whatisthatinsect.adapters.InsectAdapter
import com.ciechu.whatisthatinsect.adapters.OnItemClickListener
import com.ciechu.whatisthatinsect.R
import com.ciechu.whatisthatinsect.data.Insect
import com.ciechu.whatisthatinsect.viewmodels.InsectViewModel
import kotlinx.android.synthetic.main.fragment_captured_insects.*

class CapturedInsectsFragment : Fragment(), OnItemClickListener {

   private lateinit var insectViewModel: InsectViewModel
   private lateinit var insectAdapter: InsectAdapter

    private var optionMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        insectViewModel = ViewModelProvider(requireActivity())[InsectViewModel::class.java]
        requireActivity()
            .onBackPressedDispatcher
            .addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (insectViewModel.multiSelectMode) exitMultiSelectedMode()
                    else {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView_captured_insect.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_captured_insects, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        insectViewModel.allInsects.observe(viewLifecycleOwner, Observer {
            updateInsect(it)
        })

        updateTitle()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_insect, menu)
        optionMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_delete){
            insectViewModel.delete(insectViewModel.selectedInsects.toList())
            exitMultiSelectedMode()
            updateDeleteButton()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemLongClick(insect: Insect, position: Int) {
        if (!insectViewModel.multiSelectMode){
            insectViewModel.multiSelectMode = !insectViewModel.multiSelectMode
            selectInsect(insect, position)
            updateDeleteButton()
            updateTitle()
        }
    }

    private fun updateInsect(list: List<Insect>){
        insectAdapter = InsectAdapter(list, this)
        recyclerView_captured_insect.adapter = insectAdapter
    }

    private fun exitMultiSelectedMode() {
        insectViewModel.multiSelectMode = false
        insectViewModel.selectedInsects.forEach{ it.isSelected = false }
        insectViewModel.selectedInsects.clear()
        updateDeleteButton()
        insectAdapter.notifyDataSetChanged()
       updateTitle()
    }

    private fun updateDeleteButton() {
        optionMenu?.findItem(R.id.menu_delete)?.isVisible = insectViewModel.multiSelectMode
    }

    override fun onItemClick(insect: Insect, position: Int) {
        if (insectViewModel.multiSelectMode){
            if (insectViewModel.selectedInsects.contains(insect)){
                unselectedInsect(insect, position)
            } else {
                selectInsect(insect, position)
            }
        }
    }

    private fun selectInsect(insect: Insect, position: Int) {
        insect.isSelected = true
        insectViewModel.selectedInsects.add(insect)
        insectAdapter.notifyItemChanged(position)
    }

    private fun unselectedInsect(insect: Insect, position: Int){
        insect.isSelected = false
        insectViewModel.selectedInsects.remove(insect)
        insectAdapter.notifyItemChanged(position)
        if (insectViewModel.selectedInsects.isEmpty()) exitMultiSelectedMode()
    }

    private fun updateTitle(){
        if (insectViewModel.multiSelectMode){
            (requireActivity() as AppCompatActivity).supportActionBar?.title = "Multi-select mode"
        } else {
            (requireActivity() as AppCompatActivity).supportActionBar?.title = "My captured insects"
        }
    }
}