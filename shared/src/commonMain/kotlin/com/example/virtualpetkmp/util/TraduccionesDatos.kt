package com.example.virtualpetkmp.util

/**
 * Traducción de los valores que viven en la base de datos: especies, razas y tipos.
 *
 * **Por qué existe este fichero y no solo `strings.xml`.** La especie y la raza se guardan en
 * la base de datos como texto ("Perro", "Labrador Retriever") y hay código que los compara
 * para rellenar el formulario (`mascota.raza in razasDeEstaEspecie`). Si se tradujeran los
 * valores guardados, las mascotas que ya existen dejarían de coincidir y su raza aparecería en
 * blanco. La solución es no tocar lo guardado y traducir **solo al mostrar**: esto es una capa
 * de presentación, se aplica en la interfaz y nunca al escribir.
 *
 * Aquí no entra ningún texto de interfaz normal: eso va en `composeResources`, que es donde
 * Compose lo resuelve solo según el idioma del sistema.
 *
 * ---------------------------------------------------------------------------
 * TRADUCCIONES PROVISIONALES
 * Las de alemán, francés, portugués e italiano están hechas sin revisión de un hablante nativo.
 * Antes de publicar la app en esos idiomas hay que repasarlas, especialmente los nombres de
 * raza, que en cada país tienen su forma oficial.
 * ---------------------------------------------------------------------------
 */
object TraduccionesDatos {

    private val ETIQUETAS: Map<Idioma, Map<String, String>> = mapOf(
        Idioma.ESPANOL to emptyMap(), // El español es el idioma de origen: no necesita traducción.

        Idioma.INGLES to mapOf(
            "Perro" to "Dog",
            "Gato" to "Cat",
            "Ave" to "Bird",
            "Conejo" to "Rabbit",
            "Roedor" to "Rodent",
            "Reptil" to "Reptile",
            "Otro" to "Other",
            "Otra" to "Other",
            "Mestizo" to "Mixed breed",

            "Labrador Retriever" to "Labrador Retriever",
            "Pastor Alemán" to "German Shepherd",
            "Bulldog Francés" to "French Bulldog",
            "Bulldog Inglés" to "English Bulldog",
            "Golden Retriever" to "Golden Retriever",
            "Chihuahua" to "Chihuahua",
            "Yorkshire Terrier" to "Yorkshire Terrier",
            "Husky Siberiano" to "Siberian Husky",
            "Pastor Belga" to "Belgian Shepherd",
            "Gran Danés" to "Great Dane",
            "Cocker Spaniel" to "Cocker Spaniel",
            "San Bernardo" to "Saint Bernard",
            "Basset Hound" to "Basset Hound",
            "Dachshund" to "Dachshund",
            "Pomerania" to "Pomeranian",
            "Shih Tzu" to "Shih Tzu",
            "Doberman" to "Doberman",
            "Border Collie" to "Border Collie",
            "Rottweiler" to "Rottweiler",
            "Beagle" to "Beagle",
            "Poodle" to "Poodle",
            "Boxer" to "Boxer",

            "Siamés" to "Siamese",
            "Persa" to "Persian",
            "Común Europeo" to "European Shorthair",
            "Maine Coon" to "Maine Coon",
            "Bengalí" to "Bengal",
            "Burmés" to "Burmese",
            "Angora Turco" to "Turkish Angora",
            "Ruso Azul" to "Russian Blue",
            "British Shorthair" to "British Shorthair",
            "Scottish Fold" to "Scottish Fold",
            "Sphynx" to "Sphynx",
            "Ragdoll" to "Ragdoll",
            "Savannah" to "Savannah",

            "Canario" to "Canary",
            "Periquito" to "Budgerigar",
            "Loro" to "Parrot",
            "Agaporni" to "Lovebird",
            "Cabeza de León" to "Lionhead",
            "Holandés" to "Dutch",
            "Mini Lop" to "Mini Lop",
            "Hámster" to "Hamster",
            "Cobaya" to "Guinea pig",
            "Ratón" to "Mouse",
            "Iguana" to "Iguana",
            "Tortuga" to "Turtle",
            "Serpiente" to "Snake",

            // Tipos de preventivo
            "Pipeta" to "Spot-on treatment",
            "Desparasitación interna" to "Deworming",
            "Desparasitación" to "Deworming",

            // Sexo
            "Macho" to "Male",
            "Hembra" to "Female"
        ),

        Idioma.PORTUGUES to mapOf(
            "Perro" to "Cão",
            "Gato" to "Gato",   // En portugués también es "gato".
            "Ave" to "Ave",     // Y "ave", igual que en español.
            "Conejo" to "Coelho",
            "Roedor" to "Roedor",
            "Reptil" to "Réptil",
            "Otro" to "Outro",
            "Otra" to "Outra",
            "Mestizo" to "Mestiço",
            "Pastor Alemán" to "Pastor Alemão",
            "Bulldog Francés" to "Buldogue Francês",
            "Bulldog Inglés" to "Buldogue Inglês",
            "Husky Siberiano" to "Husky Siberiano",
            "Pastor Belga" to "Pastor Belga",
            "Gran Danés" to "Dogue Alemão",
            "San Bernardo" to "São Bernardo",
            "Siamés" to "Siamês",
            "Común Europeo" to "Europeu Comum",
            "Bengalí" to "Bengali",
            "Burmés" to "Birmanês",
            "Angora Turco" to "Angorá Turco",
            "Ruso Azul" to "Azul Russo",
            "Pomerania" to "Pomerânia",
            "Canario" to "Canário",
            "Periquito" to "Periquito",
            "Cabeza de León" to "Cabeça de Leão",
            "Holandés" to "Holandês",
            "Hámster" to "Hamster",
            "Ratón" to "Rato",
            "Tortuga" to "Tartaruga",
            "Serpiente" to "Serpente",
            "Pipeta" to "Pipeta",
            "Desparasitación interna" to "Desparasitação interna",
            "Desparasitación" to "Desparasitação",
            "Macho" to "Macho",
            "Hembra" to "Fêmea"
        ),

        Idioma.FRANCES to mapOf(
            "Perro" to "Chien",
            "Gato" to "Chat",
            "Ave" to "Oiseau",
            "Conejo" to "Lapin",
            "Roedor" to "Rongeur",
            "Reptil" to "Reptile",
            "Otro" to "Autre",
            "Otra" to "Autre",
            "Mestizo" to "Croisé",
            "Pastor Alemán" to "Berger allemand",
            "Bulldog Francés" to "Bouledogue français",
            "Bulldog Inglés" to "Bouledogue anglais",
            "Husky Siberiano" to "Husky sibérien",
            "Pastor Belga" to "Berger belge",
            "Gran Danés" to "Dogue allemand",
            "San Bernardo" to "Saint-Bernard",
            "Siamés" to "Siamois",
            "Común Europeo" to "Européen commun",
            "Bengalí" to "Bengal",
            "Burmés" to "Burmèse",
            "Angora Turco" to "Angora turc",
            "Ruso Azul" to "Bleu russe",
            "Pomerania" to "Poméranien",
            "Canario" to "Canari",
            "Periquito" to "Perruche",
            "Cabeza de León" to "Tête de lion",
            "Holandés" to "Hollandais",
            "Hámster" to "Hamster",
            "Ratón" to "Souris",
            "Tortuga" to "Tortue",
            "Serpiente" to "Serpent",
            "Pipeta" to "Pipette",
            "Desparasitación interna" to "Vermifuge",
            "Desparasitación" to "Vermifuge",
            "Macho" to "Mâle",
            "Hembra" to "Femelle"
        ),

        Idioma.ALEMAN to mapOf(
            "Perro" to "Hund",
            "Gato" to "Katze",
            "Ave" to "Vogel",
            "Conejo" to "Kaninchen",
            "Roedor" to "Nagetier",
            "Reptil" to "Reptil",
            "Otro" to "Sonstiges",
            "Otra" to "Sonstige",
            "Mestizo" to "Mischling",
            "Pastor Alemán" to "Deutscher Schäferhund",
            "Bulldog Francés" to "Französische Bulldogge",
            "Bulldog Inglés" to "Englische Bulldogge",
            "Husky Siberiano" to "Sibirischer Husky",
            "Pastor Belga" to "Belgischer Schäferhund",
            "Gran Danés" to "Deutsche Dogge",
            "San Bernardo" to "Bernhardiner",
            "Siamés" to "Siamkatze",
            "Común Europeo" to "Europäisch Kurzhaar",
            "Bengalí" to "Bengal",
            "Burmés" to "Burma-Katze",
            "Angora Turco" to "Türkisch Angora",
            "Ruso Azul" to "Russisch Blau",
            "Pomerania" to "Zwergspitz",
            "Canario" to "Kanarienvogel",
            "Periquito" to "Wellensittich",
            "Cabeza de León" to "Löwenkopf",
            "Holandés" to "Holländer",
            "Hámster" to "Hamster",
            "Ratón" to "Maus",
            "Tortuga" to "Schildkröte",
            "Serpiente" to "Schlange",
            "Pipeta" to "Spot-on-Präparat",
            "Desparasitación interna" to "Entwurmung",
            "Desparasitación" to "Entwurmung",
            "Macho" to "Männlich",
            "Hembra" to "Weiblich"
        ),

        Idioma.ITALIANO to mapOf(
            "Perro" to "Cane",
            "Gato" to "Gatto",
            "Ave" to "Uccello",
            "Conejo" to "Coniglio",
            "Roedor" to "Roditore",
            "Reptil" to "Rettile",
            "Otro" to "Altro",
            "Otra" to "Altra",
            "Mestizo" to "Meticcio",
            "Pastor Alemán" to "Pastore tedesco",
            "Bulldog Francés" to "Bulldog francese",
            "Bulldog Inglés" to "Bulldog inglese",
            "Husky Siberiano" to "Husky siberiano",
            "Pastor Belga" to "Pastore belga",
            "Gran Danés" to "Alano tedesco",
            "San Bernardo" to "San Bernardo",
            "Siamés" to "Siamese",
            "Común Europeo" to "Europeo comune",
            "Bengalí" to "Bengala",
            "Burmés" to "Burmese",
            "Angora Turco" to "Angora turco",
            "Ruso Azul" to "Blu di Russia",
            "Pomerania" to "Pomerania",
            "Canario" to "Canarino",
            "Periquito" to "Pappagallino",
            "Cabeza de León" to "Testa di leone",
            "Holandés" to "Olandese",
            "Hámster" to "Criceto",
            "Ratón" to "Topo",
            "Tortuga" to "Tartaruga",
            "Serpiente" to "Serpente",
            "Pipeta" to "Pipetta",
            "Desparasitación interna" to "Sverminamento",
            "Desparasitación" to "Sverminamento",
            "Macho" to "Maschio",
            "Hembra" to "Femmina"
        )
    )

    /**
     * Texto que se enseña para un valor guardado en la base de datos.
     *
     * Si el valor no está en el diccionario se devuelve tal cual: puede ser una raza o una
     * especie escrita a mano por el usuario ("Otro"), y en ese caso lo correcto es mostrar lo
     * que escribió, no un hueco.
     */
    fun etiqueta(valorGuardado: String, idioma: Idioma): String {
        if (idioma == Idioma.ESPANOL) return valorGuardado
        return ETIQUETAS[idioma]?.get(valorGuardado) ?: valorGuardado
    }

    /**
     * Si hay una traducción definida para ese valor.
     *
     * Es distinto de comprobar que el texto cambie: hay palabras que se escriben igual en
     * varios idiomas ("Gato" en español, portugués e italiano) y no son un olvido.
     */
    fun tieneTraduccion(valorGuardado: String, idioma: Idioma): Boolean =
        idioma == Idioma.ESPANOL || ETIQUETAS[idioma]?.containsKey(valorGuardado) == true

    /** Atajo para componer "Especie - Raza" ya traducidos, omitiendo la raza si está vacía. */
    fun especieYRaza(especie: String, raza: String, idioma: Idioma): String {
        val especieTraducida = etiqueta(especie, idioma)
        val razaTraducida = etiqueta(raza, idioma)
        return if (razaTraducida.isBlank()) especieTraducida else "$especieTraducida - $razaTraducida"
    }
}
