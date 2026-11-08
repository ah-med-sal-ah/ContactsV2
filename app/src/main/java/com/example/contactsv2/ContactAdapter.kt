package com.example.contactsv2

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.example.contactsv2.databinding.ItemContactBinding

class ContactAdapter(private val contacts: MutableList<Contact>) : BaseAdapter() {

    override fun getCount(): Int = contacts.size

    override fun getItem(position: Int): Contact = contacts[position]

    override fun getItemId(position: Int): Long = contacts[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Reuse the row if Android gives us one, otherwise inflate a new one
        val binding = if (convertView == null) {
            ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                .also { it.root.tag = it }
        } else {
            convertView.tag as ItemContactBinding
        }

        val contact = getItem(position)
        binding.tvNomComplet.text = "${contact.prenom} ${contact.nom.uppercase()}"
        binding.tvTelephone.text = contact.telephone

        if (contact.photoUri != null) {
            binding.imgPhoto.setImageURI(Uri.parse(contact.photoUri))
        } else {
            binding.imgPhoto.setImageResource(R.drawable.ic_person)
        }
        return binding.root
    }
}