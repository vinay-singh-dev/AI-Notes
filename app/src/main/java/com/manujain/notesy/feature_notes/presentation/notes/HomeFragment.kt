package com.manujain.notesy.feature_notes.presentation.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.manujain.notesy.R
import com.manujain.notesy.core.adjustPaddingWithStatusBar
import com.manujain.notesy.core.getNotesOrderFromStr
import com.manujain.notesy.core.launchCoroutineOnStart
import com.manujain.notesy.core.selected
import com.manujain.notesy.databinding.FragmentHomeBinding
import com.manujain.notesy.feature_notes.domain.model.Note
import com.manujain.notesy.feature_notes.domain.utils.NotesUiEvent
import com.manujain.notesy.feature_notes.domain.utils.OrderType
import com.manujain.notesy.feature_notes.presentation.background_chooser.SpacingItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(), OnNoteItemUserActivityListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val notesViewModel by viewModels<NotesViewModel>()
    private val notesAdapter: NotesAdapter by lazy { NotesAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        adjustPaddingWithStatusBar(binding.homeParent)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.nestedScrollView.post(
            Runnable {
                binding.nestedScrollView.scrollTo(0, notesViewModel.nestedScrollViewPos)
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initNotesUi()
        initUiControls()
        initObservers()
    }

    override fun onPause() {
        super.onPause()
        notesViewModel.nestedScrollViewPos = binding.nestedScrollView.scrollY
    }

    private fun initNotesUi() {
        // Notes List
        binding.notesRV.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = notesAdapter
            addItemDecoration(SpacingItemDecoration(18, 18))
            ItemTouchHelper(
                NoteSwipeHandler { position ->
                    notesAdapter.deleteNote(position)
                }
            ).attachToRecyclerView(this)
        }
    }

    private fun initUiControls() {
        // Notes Order Switching
        ArrayAdapter.createFromResource(
            requireActivity(),
            R.array.notes_order_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.switchNotesOrder.adapter = adapter
        }
    }

    private fun initObservers() {
        // Fetch Notes
        launchCoroutineOnStart {
            lifecycleScope.launch {
                notesViewModel.notes.collectLatest {
                    notesAdapter.submitList(it)
                }
            }
        }

        binding.apply {
            // Add Notes
            addNotes.setOnClickListener {
                navigateToAddEditNoteFragment()
            }

            // On Notes Order Switched
            switchNotesOrder.selected { selected ->
                val order = getNotesOrderFromStr(selected, OrderType.DESCENDING)
                notesViewModel.onEvent(NotesUiEvent.Order(order))
            }
        }
    }

    override fun onNoteItemClicked(note: Note) {
        navigateToAddEditNoteFragment(note.id)
    }

    override fun onNoteItemDeleted(note: Note) {
        notesViewModel.onEvent(NotesUiEvent.DeleteNote(note))
        showSnackbarNoteDeleted()
    }

    private fun showSnackbarNoteDeleted() {
        Snackbar.make(
            binding.parent,
            R.string.note_deleted,
            BaseTransientBottomBar.LENGTH_LONG
        ).setAction(R.string.restored) {
            notesViewModel.onEvent(NotesUiEvent.RestoreNote)
            showSnackbarNoteRestored()
        }.show()
    }

    private fun showSnackbarNoteRestored() {
        Snackbar.make(
            binding.parent,
            R.string.restored,
            BaseTransientBottomBar.LENGTH_LONG
        ).show()
    }

    private fun navigateToAddEditNoteFragment(noteId: String? = null) {
        val action = HomeFragmentDirections.actionHomeFragmentToComposeNoteFragment(noteId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
