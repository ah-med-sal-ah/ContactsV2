package com.example.contactsv2

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.example.contactsv2.databinding.ActivityDetailBinding
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Patterns
class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONTACT = "extra_contact"
    }

    private lateinit var binding: ActivityDetailBinding
    private lateinit var contact: Contact
    // Photo currently shown (can differ from contact.photoUri until we save)
    private var currentPhotoUri: String? = null

    // Gallery launcher: must be declared as a property, not inside onCreate
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) {
                // User closed the picker without choosing
                Toast.makeText(this, "Aucune photo sélectionnée", Toast.LENGTH_SHORT).show()
            } else {
                val local = copierDansStockageInterne(uri)
                if (local == null) {
                    Toast.makeText(this, "Impossible de charger la photo", Toast.LENGTH_SHORT).show()
                } else {
                    currentPhotoUri = local.toString()
                    binding.imgPhoto.setImageURI(local)
                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Read the Contact sent by MainActivity
        val received = IntentCompat.getParcelableExtra(intent, EXTRA_CONTACT, Contact::class.java)
        if (received == null) {
            finish()   // nothing to show, go back
            return
        }
        contact = received
        // After a rotation, restore the photo chosen before it
        currentPhotoUri = savedInstanceState?.getString("photo") ?: contact.photoUri
        pendingCameraUri = savedInstanceState?.getString("pending_camera")?.let { Uri.parse(it) }
        showContact()

        // Annuler: go back without saving (the result stays RESULT_CANCELED)
        binding.btnAnnuler.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        binding.btnEnregistrer.setOnClickListener { enregistrer() }
        binding.btnAppeler.setOnClickListener { appeler() }
        binding.btnEmail.setOnClickListener { envoyerEmail() }
        binding.btnCamera.setOnClickListener { ouvrirCamera() }
        binding.btnGalerie.setOnClickListener {
            try {
                pickImage.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Aucune galerie disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("photo", currentPhotoUri)
        outState.putString("pending_camera", pendingCameraUri?.toString())
    }

    // Put the contact's data into the form
    private fun showContact() {
        binding.etNom.setText(contact.nom)
        binding.etPrenom.setText(contact.prenom)
        binding.etTelephone.setText(contact.telephone)
        binding.etEmail.setText(contact.email)

        if (currentPhotoUri != null) {
            binding.imgPhoto.setImageURI(Uri.parse(currentPhotoUri))
        } else {
            binding.imgPhoto.setImageResource(R.drawable.ic_person)
        }
    }
    // Open the dialer with the number currently in the field
    private fun appeler() {
        val numero = binding.etTelephone.text.toString().trim()
        if (numero.isEmpty()) {
            binding.etTelephone.error = "Numéro requis"
            return
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", numero, null))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Aucune application d'appel disponible", Toast.LENGTH_SHORT).show()
        }
    }

    // Open an e-mail app with the address pre-filled
    private fun envoyerEmail() {
        val adresse = binding.etEmail.text.toString().trim()
        if (adresse.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(adresse).matches()) {
            binding.etEmail.error = "E-mail valide requis"
            return
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", adresse, null))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Aucune application de messagerie disponible", Toast.LENGTH_SHORT).show()
        }
    }
    // Validate the form, then send the updated contact back to MainActivity
    private fun enregistrer() {
        val nom = binding.etNom.text.toString().trim()
        val prenom = binding.etPrenom.text.toString().trim()
        val telephone = binding.etTelephone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        var valide = true
        if (nom.isEmpty()) { binding.etNom.error = "Nom requis"; valide = false }
        if (prenom.isEmpty()) { binding.etPrenom.error = "Prénom requis"; valide = false }
        if (telephone.isEmpty()) { binding.etTelephone.error = "Téléphone requis"; valide = false }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "E-mail invalide"
            valide = false
        }
        if (!valide) return


        // copy() keeps the same id and replaces only the modified fields
        val updated = contact.copy(
            nom = nom,
            prenom = prenom,
            telephone = telephone,
            email = email,
            photoUri = currentPhotoUri
        )

        val result = Intent().putExtra(EXTRA_CONTACT, updated)
        setResult(RESULT_OK, result)
        finish()
    }
    // Check camera availability and permission, then open the camera
    private fun ouvrirCamera() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, "Caméra indisponible sur cet appareil", Toast.LENGTH_SHORT).show()
            return
        }
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            lancerCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    // Create the target file and launch the camera app
    private fun lancerCamera() {
        val dir = File(filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "contact_${contact.id}_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        pendingCameraUri = uri
        try {
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Aucune application caméra disponible", Toast.LENGTH_SHORT).show()
        }
    }
    // URI of the file the camera is about to write into
    private var pendingCameraUri: Uri? = null

    // Result of the camera: true if a photo was taken
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = pendingCameraUri
            if (success && uri != null) {
                currentPhotoUri = uri.toString()
                binding.imgPhoto.setImageURI(uri)
            } else {
                Toast.makeText(this, "Prise de photo annulée", Toast.LENGTH_SHORT).show()
            }
        }

    // Result of the permission dialog
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                lancerCamera()
            } else {
                Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show()
            }
        }

    // Copy a gallery image into the app's private storage and return its new URI
    private fun copierDansStockageInterne(uri: Uri): Uri? {
        return try {
            val dir = File(filesDir, "photos").apply { mkdirs() }
            val file = File(dir, "contact_${contact.id}_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }
}