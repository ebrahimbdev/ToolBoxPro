package com.toolbox.pro.qr.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ContactItem(
    val id: String,
    val name: String,
    val numbers: List<String>
)

@Singleton
class ContactsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    suspend fun loadContacts(): List<ContactItem> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val grouped = LinkedHashMap<String, ContactItem>()
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val id = if (idIndex >= 0) cursor.getString(idIndex) ?: continue else continue
                    val name = (if (nameIndex >= 0) cursor.getString(nameIndex) else null)?.trim().orEmpty()
                        .ifBlank { "?" }
                    val number = (if (numberIndex >= 0) cursor.getString(numberIndex) else null)
                        ?.filterNot { it.isWhitespace() }
                        ?.takeIf { it.isNotBlank() }
                    if (name.isEmpty() && number == null) continue

                    val existing = grouped[id]
                    if (existing == null) {
                        grouped[id] = ContactItem(id = id, name = name, numbers = listOfNotNull(number))
                    } else if (number != null && existing.numbers.size < 3 && !existing.numbers.contains(number)) {
                        grouped[id] = existing.copy(numbers = existing.numbers + number)
                    }
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        } catch (_: Throwable) {
            return@withContext grouped.values.sortedBy { it.name.lowercase() }
        }

        grouped.values.sortedBy { it.name.lowercase() }
    }

    /**
     * vCard 3.0 payloads. The hard byte cap keeps the payload inside a QR
     * version 40 code (level M = 2331 bytes, we stay well below it).
     */
    fun buildVCards(contacts: List<ContactItem>): String {
        return contacts.joinToString("\r\n") { contact ->
            buildString {
                append("BEGIN:VCARD\r\n")
                append("VERSION:3.0\r\n")
                append("FN:").append(escape(contact.name)).append("\r\n")
                append("N:").append(escape(contact.name)).append(";;;;\r\n")
                contact.numbers.forEach { number ->
                    append("TEL;TYPE=CELL:").append(escape(number)).append("\r\n")
                }
                append("END:VCARD")
            }
        }
    }

    fun payloadBytes(contacts: List<ContactItem>): Int =
        buildVCards(contacts).toByteArray(Charsets.UTF_8).size

    private fun escape(value: String): String = buildString(value.length + 4) {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                ';' -> append("\\;")
                ',' -> append("\\,")
                '\n' -> append("\\n")
                '\r' -> Unit
                else -> append(ch)
            }
        }
    }

    companion object {
        const val MAX_CONTACTS = 10
        const val MAX_BYTES = 1100
    }
}
