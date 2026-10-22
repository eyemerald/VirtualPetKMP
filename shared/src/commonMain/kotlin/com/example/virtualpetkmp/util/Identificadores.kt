package com.example.virtualpetkmp.util

import kotlinx.datetime.Clock

/**
 * Identificador universal de una fila: 32 caracteres hexadecimales en minúscula, sin guiones.
 *
 * Es el mismo formato que escribe la migración 13 con `randomblob(16)`, para que una fila migrada
 * y una recién creada sean indistinguibles.
 *
 * **No se usa el `id`** porque es un autonumérico local: la vacuna 3 de un móvil y la vacuna 3 de
 * otro son registros distintos, y al exportar e importar hay que poder reconocer los que sí son el
 * mismo. Este valor, en cambio, no cambia nunca.
 *
 * Es `expect`/`actual` y no un cálculo hecho a mano con `Random` porque en la JVM se resuelve con
 * `java.util.UUID.randomUUID()`, que usa un generador criptográfico: dos dispositivos distintos no
 * pueden acabar con el mismo identificador por azar.
 */
expect fun nuevoIdentificadorUniversal(): String

/**
 * Momento actual en milisegundos desde epoch, que es como se guarda `actualizadoEn`.
 *
 * En milisegundos y no como texto ISO a propósito: así se compara con `<` en SQL sin depender de
 * cuántos decimales de segundo haya escrito cada plataforma.
 */
fun ahoraMillis(): Long = Clock.System.now().toEpochMilliseconds()
