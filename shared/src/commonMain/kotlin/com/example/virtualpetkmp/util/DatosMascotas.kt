package com.example.virtualpetkmp.util

val ESPECIES = listOf(
    "Perro", "Gato", "Ave", "Conejo", "Roedor", "Reptil", "Otro"
)

val RAZAS_POR_ESPECIE: Map<String, List<String>> = mapOf(
    "Perro" to listOf(
        "Labrador Retriever", "Pastor Alemán", "Bulldog Francés", "Golden Retriever",
        "Chihuahua", "Poodle", "Beagle", "Rottweiler", "Yorkshire Terrier",
        "Boxer", "Dachshund", "Pastor Belga", "Husky Siberiano", "Border Collie",
        "Bulldog Inglés", "Shih Tzu", "Pomerania", "Doberman", "Gran Danés",
        "Cocker Spaniel", "San Bernardo", "Basset Hound", "Mestizo", "Otra"
    ),
    "Gato" to listOf(
        "Siamés", "Persa", "Común Europeo", "Maine Coon", "Bengalí",
        "Ragdoll", "Burmés", "Sphynx", "British Shorthair", "Scottish Fold",
        "Angora Turco", "Ruso Azul", "Savannah", "Mestizo", "Otra"
    ),
    "Ave" to listOf("Canario", "Periquito", "Loro", "Agaporni", "Otra"),
    "Conejo" to listOf("Cabeza de León", "Holandés", "Mini Lop", "Otra"),
    "Roedor" to listOf("Hámster", "Cobaya", "Ratón", "Otra"),
    "Reptil" to listOf("Iguana", "Tortuga", "Serpiente", "Otra"),
    "Otro" to listOf("Otra")
)
