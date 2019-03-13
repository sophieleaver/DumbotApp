package com.example.sophieleaver.dumbotapp

import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_add_dumbbell.view.*
import kotlinx.android.synthetic.main.item_storage_location.view.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import java.util.*


class NewWeightDialog : DialogFragment() {

    private val database: DatabaseReference =
        FirebaseDatabase.getInstance().reference.child("demo2")

    private val newWeight: Dumbbell = Dumbbell()
    private var storageAreas: List<String> = emptyList()

    private lateinit var toolbar: Toolbar
    private lateinit var weightValueText: TextInputEditText
    private lateinit var totalStockText: TextInputEditText
    private lateinit var storageAreaList: RecyclerView
    private lateinit var saveNewWeightButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }

    override fun onStart() {
        super.onStart()

        dialog?.let {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            it.window?.setLayout(width, height)
            it.window?.setWindowAnimations(R.style.AppTheme_Slide)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_dumbbell, container, false).apply {
            toolbar = toolbar_add_new_dumbbell
            storageAreaList = storage_location_list
            saveNewWeightButton = btn_confirm_new_weight
            weightValueText = text_weight_value
            totalStockText = text_total_stock
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(toolbar) {
            setNavigationOnClickListener { dismiss() }
            title = "Add New Weight"
//            inflateMenu(R.menu.menu_add_dumbbell)
//            setOnMenuItemClickListener {
//                saveNewWeight()
//                return@setOnMenuItemClickListener true
//            }
        }

        saveNewWeightButton.setOnClickListener {

            val weightValue = weightValueText.text
            val totalStock = totalStockText.text

            val invalidForm = (newWeight.storageLocation.isNullOrEmpty())
                    || (totalStock.isNullOrBlank())
                    || (weightValue.isNullOrBlank())

            if (invalidForm) {
                requireActivity().toast("Must fill in all parts of the form to add a new weight type to the inventory")
            } else {
                AlertDialog.Builder(this.requireContext()).apply {
                    setTitle("Save new weight to Weight Inventory?")
                    setMessage(
                        getString(
                            R.string.text_confirm_add_new_dumbbell,
                            weightValueText.text!!,
                            totalStockText.text!!
                        )
                    )
                    setPositiveButton("Save New Weight") { _, _ ->
                        saveNewWeight(weightValue.toString().toInt(), totalStock.toString().toInt())
                    }
                    setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
                    create()
                    show()
                }

            }
        }

        getStorageAreas()

    }

    private fun getStorageAreas() {
        database
            .child("layout/nodes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val nodeString: String? = dataSnapshot.value as String?
                    storageAreas = nodeString
                        ?.removeSurrounding("[", "]")
                        ?.replace("\'", "")
                        ?.split(",")
                        ?.filter { it.startsWith("SA", true) }
                        ?: emptyList()

                    setupRecyclerView()

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    requireActivity().longToast("There was a problem loading the storage areas. Please try again")
                    dismiss()
                }

            })
    }

    private fun setupRecyclerView() {
        storageAreaList.layoutManager = GridLayoutManager(requireContext(), 2)
        storageAreaList.adapter = StorageLocationAdapter()

        arguments?.let {

            weightValueText.setText(it.getInt("weightValue").toString())
            totalStockText.setText(it.getInt("totalStock").toString())
            newWeight.storageLocation.addAll(it.getStringArrayList("storageLocation")!!)
            storageAreaList.adapter!!.notifyDataSetChanged()

            setUpForEditingWeight()


        }
    }

    private fun setUpForEditingWeight() {
        saveNewWeightButton.text = getString(R.string.confirm_edited_weight)
        weightValueText.isEnabled = false

        with(toolbar) {
            title = "Edit Weight"
            inflateMenu(R.menu.edit_dumbbell)
            setOnMenuItemClickListener {
                AlertDialog.Builder(requireContext()).apply {
                    setTitle("Remove Weight from Inventory")
                    setMessage(
                        getString(
                            R.string.detail_delete_weight,
                            weightValueText.text.toString().toInt()
                        )
                    )
                    setPositiveButton("Remove Weight") { _, _ -> removeWeight(weightValueText.text.toString().toInt()) }
                    setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
                    create()
                    show()
                }
                return@setOnMenuItemClickListener true
            }
        }

        saveNewWeightButton.setOnClickListener {
            val weightValue = weightValueText.text
            val totalStock = totalStockText.text

            val invalidForm = (newWeight.storageLocation.isNullOrEmpty())
                    || (totalStock.isNullOrBlank())
                    || (weightValue.isNullOrBlank())

            if (invalidForm) {
                requireActivity().toast("Must fill in all parts of the form to edit a weight")
            } else {
                AlertDialog.Builder(requireContext()).apply {
                    setTitle("Confirm Edit Weight")
                    setMessage(
                        getString(
                            R.string.detail_edit_weight,
                            totalStock.toString().toInt(),
                            weightValue.toString().toInt(),
                            newWeight.storageLocation.joinToString()
                        )
                    )
                    setPositiveButton("Save Edited Weight") { _, _ ->
                        editWeight(weightValue.toString().toInt(), totalStock.toString().toInt())
                    }
                    setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
                    create()
                    show()
                }
            }
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun removeWeight(weightValue: Int) {
        database.child("weights/$weightValue")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    dataSnapshot.getValue(Dumbbell::class.java)?.let {
                        if ((it.activeRequests.isNullOrEmpty()) and (it.waitQueue.isNullOrEmpty())) {
                            database.child("weights/$weightValue").removeValue()
                                .addOnCompleteListener { task ->
                                    val resultText = if (task.isSuccessful) {
                                        "Successfully removed " +
                                                "$weightValue kg weight from weight inventory."
                                    } else {
                                        "Error while attempting to remove " +
                                                "$weightValue kg weight from weight inventory. " +
                                                "Please try again later."
                                    }
                                    requireActivity().toast(resultText)
                                    dismiss()
                                }

                        } else {
                            val currentRequests = it.activeRequests.size + it.waitQueue.size
                            requireActivity().toast(
                                "$currentRequests people are still using " +
                                        "$weightValue kg dumbbells or are in the wait queue. " +
                                        "Cannot remove from weight inventory until all requests are cleared."
                            )
                        }
                    }

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w(TAG, "Failed to get dumbbell from database", databaseError.toException())
                    requireActivity().toast(
                        "Error while attempting to remove " +
                                "$weightValue kg weight from weight inventory. " +
                                "Please try again later."
                    )
                }

            })
    }

    private fun editWeight(weightValue: Int, newTotalStock: Int) {
        database.child("weights/$weightValue")
            .updateChildren(mapOf("totalStock" to newTotalStock, "storageLocation" to newWeight.storageLocation))
            .addOnSuccessListener {
                requireActivity().toast("Successfully edited $weightValue kg weight")
                dismiss()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Failed saving edits to $weightValue kg weight", exception)
                requireActivity().toast("Error while saving edits to $weightValue kg weight. Please try again later")
                dismiss()
            }
    }

    private fun saveNewWeight(weightValue: Int, totalStock: Int) {

        database.child("weights/$weightValue").setValue(newWeight.apply {
            this.weightValue = weightValue
            this.totalStock = totalStock
        }).addOnSuccessListener {
            requireActivity().toast("Successfully added $weightValue kg weight to Weight Inventory")
            dismiss()
        }.addOnFailureListener { exception ->
            Log.w(TAG, "Failed adding $weightValue kg to Weight Inventory", exception)
            requireActivity().longToast("Error while trying to add $weightValue kg weight to Weight Inventory. Please try again later")
            dismiss()
        }

    }

    companion object {
        private const val TAG = "NewWeightDialog"

        fun display(fragmentManager: FragmentManager, weightToEdit: Dumbbell? = null): NewWeightDialog {
            val exampleDialog = NewWeightDialog().apply {
                weightToEdit?.let {
                    arguments = Bundle().apply {
                        putInt("weightValue", it.weightValue)
                        putInt("totalStock", it.totalStock)
                        putStringArrayList(
                            "storageLocation",
                            it.storageLocation as ArrayList<String>
                        )
                    }
                }


            }
            exampleDialog.show(fragmentManager, TAG)
            return exampleDialog
//        return NewWeightDialog().also { show(fragmentManager, TAG) }
        }
    }

    inner class StorageLocationAdapter : RecyclerView.Adapter<StorageLocationAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(this@NewWeightDialog.requireContext())
                    .inflate(R.layout.item_storage_location, parent, false)
            )

        override fun getItemCount(): Int = storageAreas.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            with(holder) {
                storageLocation.text =
                    getString(R.string.item_storage_location, storageAreas[position])
                itemView.apply {
                    setOnClickListener {
                        with(storageAreas[adapterPosition]) {
                            val selected = newWeight.storageLocation.contains(this)
                            if (selected) {
                                setBackgroundResource(0)
                                newWeight.storageLocation.remove(this)
                            } else {
                                setBackgroundResource(R.color.colorAccent)
                                newWeight.storageLocation.add(this)
                            }
                        }

                    }
                    setBackgroundResource(
                        if (newWeight.storageLocation.contains(storageAreas[adapterPosition])) R.color.colorAccent
                        else 0
                    )
                }
            }


        }


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val storageLocation: TextView = view.select_storage_location!!
        }
    }

}