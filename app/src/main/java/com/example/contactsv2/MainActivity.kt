package com.example.contactsv2
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.contactsv2.databinding.ActivityMainBinding
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ContactAdapter

    private val contacts = mutableListOf(
        Contact(1, "Ben Ali", "Ahmed", "+216 20 111 222", "ahmed.benali@example.com"),
        Contact(2, "Trabelsi", "Sarra", "+216 21 333 444", "sarra.trabelsi@example.com"),
        Contact(3, "Gharbi", "Mohamed", "+216 22 555 666", "mohamed.gharbi@example.com"),
        Contact(4, "Mansour", "Ines", "+216 23 777 888", "ines.mansour@example.com"),
        Contact(5, "Jlassi", "Karim", "+216 24 999 000", "karim.jlassi@example.com")
    )
    // Receives the result sent back by DetailActivity
    private val editContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val updated = result.data?.let {
                    IntentCompat.getParcelableExtra(it, DetailActivity.EXTRA_CONTACT, Contact::class.java)
                } ?: return@registerForActivityResult

                val index = contacts.indexOfFirst { it.id == updated.id }
                if (index != -1) {
                    contacts[index] = updated
                    adapter.notifyDataSetChanged()   // refresh the list and photo now
                }
            }
            // RESULT_CANCELED: nothing to do, the list keeps its old data
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // After a rotation, restore the edited list instead of the sample data
        savedInstanceState?.let {
            BundleCompat.getParcelableArrayList(it, "contacts", Contact::class.java)?.let { saved ->
                contacts.clear()
                contacts.addAll(saved)
            }
        }
        adapter = ContactAdapter(contacts)
        binding.listViewContacts.adapter = adapter

        binding.listViewContacts.setOnItemClickListener { _, _, position, _ ->
            val selected = adapter.getItem(position)
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra(DetailActivity.EXTRA_CONTACT, selected)
            editContactLauncher.launch(intent)
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("contacts", ArrayList(contacts))
    }
}