package com.nospam.detection

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

/**
 * Normalizza i numeri in E.164 e ne estrae metadati utili (area geografica,
 * validità, tipo di linea) usando libphonenumber di Google — lo stesso motore usato
 * da Android per la formattazione dei numeri.
 */
class NumberNormalizer(private val defaultRegion: String = "IT") {

    private val util = PhoneNumberUtil.getInstance()

    data class Parsed(
        val e164: String?,
        val valid: Boolean,
        val region: String?,
        val description: String?,
        val lineType: String,
    )

    fun parse(raw: String?): Parsed {
        if (raw.isNullOrBlank()) {
            return Parsed(null, false, null, "Numero nascosto/anonimo", "WITHHELD")
        }
        return try {
            val proto = util.parse(raw, defaultRegion)
            val valid = util.isValidNumber(proto)
            val e164 = if (valid) util.format(proto, PhoneNumberUtil.PhoneNumberFormat.E164) else null
            val region = util.getRegionCodeForNumber(proto)
            // Descrizione dell'area dal codice regione (es. IT -> "Italia"). Evitiamo il
            // geocoder di libphonenumber, che vive in un artefatto separato e pesante.
            val desc = region?.let { Locale("", it).getDisplayCountry(Locale.ITALIAN) }
                ?.ifBlank { null }
            Parsed(
                e164 = e164,
                valid = valid,
                region = region,
                description = desc,
                lineType = util.getNumberType(proto).name,
            )
        } catch (e: NumberParseException) {
            Parsed(null, false, null, "Numero non analizzabile", "UNKNOWN")
        }
    }
}
