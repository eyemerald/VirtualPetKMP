package com.example.virtualpetkmp.util

/**
 * Nombre de archivo sugerido para el PDF de la ficha de una mascota.
 *
 * Se usa tanto al compartir (Android necesita un nombre porque el archivo va a la caché) como
 * al guardar (es el nombre que aparece preseleccionado en el diálogo del sistema).
 *
 * Empieza por "VirtualPet-" y no por el nombre de la mascota: el archivo puede acabar en el correo
 * del veterinario o en una carpeta compartida, y así se sabe de qué app sale sin abrirlo.
 *
 * El nombre de una mascota lo escribe una persona, así que lleva acentos, eñes y espacios. Un
 * nombre de archivo, en cambio, tiene que valer en cualquier sistema: aquí los acentos se
 * convierten a su letra base ("Ñuño" → "Nuno") en lugar de tirarlos, y todo lo que no sea una
 * letra o un número ASCII se convierte en un guion.
 */
fun nombreArchivoPdf(nombreMascota: String): String {
    val base = buildString {
        nombreMascota.forEach { letra ->
            // Se conserva la mayúscula o minúscula que escribió el usuario: "Luna" y no "luna".
            val sinAcento = LETRAS_CON_ACENTO[letra.lowercaseChar()]
            append(
                when {
                    sinAcento != null -> if (letra.isUpperCase()) sinAcento.uppercaseChar() else sinAcento
                    letra in 'a'..'z' || letra in 'A'..'Z' || letra in '0'..'9' -> letra
                    else -> '-'
                }
            )
        }
    }
        .trim('-')
        .replace(Regex("-{2,}"), "-")
        // 33 y no 40: con el prefijo "VirtualPet-" y la extensión, el nombre entero queda por
        // debajo de 50 caracteres, que es el límite que se prueba.
        .take(33)
        .trim('-')
        .ifEmpty { "mascota" }

    return "VirtualPet-$base.pdf"
}

/**
 * Letras acentuadas de los idiomas que habla la app, con la letra a la que equivalen.
 *
 * Se hace a mano porque en `commonMain` no hay `java.text.Normalizer` ni nada parecido: cada
 * plataforma tendría su propia forma de quitar acentos, y esto es lo mismo en todas.
 */
private val LETRAS_CON_ACENTO: Map<Char, Char> = mapOf(
    'á' to 'a', 'à' to 'a', 'â' to 'a', 'ä' to 'a', 'ã' to 'a', 'å' to 'a',
    'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
    'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
    'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'ö' to 'o', 'õ' to 'o', 'ø' to 'o',
    'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
    'ñ' to 'n', 'ç' to 'c', 'ý' to 'y', 'ÿ' to 'y',
    'ß' to 's', 'æ' to 'a', 'œ' to 'o'
)
