package com.example.virtualpetkmp.util

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * La decisión de si un aviso debe sonar.
 *
 * Es la regla que faltaba: hasta la 1.14 el receptor notificaba y reprogramaba sin mirar nada, así
 * que una alarma cuya fila ya no existía seguía sonando todos los días. Los cuatro casos de abajo
 * son los que se vieron en el emulador (seis alarmas programadas de las que cinco eran de datos que
 * ya no estaban).
 */
class VerificacionAvisoTest {

    private val uuidTratamiento = "e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1"

    private fun estado(
        uuid: String = uuidTratamiento,
        horas: List<String> = listOf("08:00", "20:00"),
        vigente: Boolean = true
    ) = EstadoEntidadAviso(uuid = uuid, horasDeAviso = horas, vigente = vigente)

    @Test
    fun `un aviso vigente suena`() {
        val decision = decidirAviso(
            tipo = TipoAviso.TRATAMIENTO,
            uuidDeLaAlarma = uuidTratamiento,
            horaDeLaAlarma = "08:00",
            estado = estado()
        )

        assertEquals(DecisionAviso.Sonar, decision)
    }

    @Test
    fun `si la entidad ya no existe no suena`() {
        // La mascota se borró o la restauración de una copia la sustituyó: no hay fila que avisar.
        val decision = decidirAviso(
            tipo = TipoAviso.TRATAMIENTO,
            uuidDeLaAlarma = uuidTratamiento,
            horaDeLaAlarma = "08:00",
            estado = null
        )

        assertEquals(DecisionAviso.Zombi(MotivoZombi.ENTIDAD_BORRADA), decision)
    }

    @Test
    fun `si le cambiaron la hora a esa toma no suena`() {
        // El tratamiento tenía tres tomas y ahora tiene dos: la alarma de la que sobraba sigue viva.
        val decision = decidirAviso(
            tipo = TipoAviso.TRATAMIENTO,
            uuidDeLaAlarma = uuidTratamiento,
            horaDeLaAlarma = "22:00",
            estado = estado(horas = listOf("08:00", "20:00"))
        )

        assertEquals(DecisionAviso.Zombi(MotivoZombi.HORA_CAMBIADA), decision)
    }

    @Test
    fun `la misma fila con otro id pero otra hora no suena`() {
        // Es el caso del móvil del usuario: la mascota existe, pero su tratamiento pasó de las 07:38
        // a una sola toma a las 20:32 y la alarma vieja seguía saltando a las 07:38.
        val decision = decidirAviso(
            tipo = TipoAviso.TRATAMIENTO,
            uuidDeLaAlarma = uuidTratamiento,
            horaDeLaAlarma = "07:38",
            estado = estado(horas = listOf("20:32"))
        )

        assertEquals(DecisionAviso.Zombi(MotivoZombi.HORA_CAMBIADA), decision)
    }

    @Test
    fun `un tratamiento terminado o sin recordatorio no suena`() {
        val decision = decidirAviso(
            tipo = TipoAviso.TRATAMIENTO,
            uuidDeLaAlarma = uuidTratamiento,
            horaDeLaAlarma = "08:00",
            estado = estado(vigente = false)
        )

        assertEquals(DecisionAviso.Zombi(MotivoZombi.YA_NO_TOCA), decision)
    }

    @Test
    fun `una alarma de la version anterior no suena porque no se puede verificar`() {
        // Las alarmas escritas antes de este cambio no llevan uuid: se tratan como zombi y se
        // cancelan. El arranque vuelve a programar las vigentes con el formato nuevo.
        val decision = decidirAviso(
            tipo = null,
            uuidDeLaAlarma = null,
            horaDeLaAlarma = "08:00",
            estado = estado()
        )

        assertEquals(DecisionAviso.Zombi(MotivoZombi.SIN_IDENTIDAD), decision)
    }

    @Test
    fun `el aviso de una vacuna suena aunque no tenga horas`() {
        val decision = decidirAviso(
            tipo = TipoAviso.VACUNA,
            uuidDeLaAlarma = "c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1",
            horaDeLaAlarma = null,
            estado = estado(uuid = "c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1", horas = emptyList())
        )

        assertEquals(DecisionAviso.Sonar, decision)
    }

    @Test
    fun `si la vacuna ya se aplico (fecha cambiada) no suena`() {
        val decision = decidirAviso(
            tipo = TipoAviso.VACUNA,
            uuidDeLaAlarma = "c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1",
            horaDeLaAlarma = null,
            estado = estado(
                uuid = "c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1",
                horas = emptyList(),
                vigente = false
            )
        )

        assertEquals(DecisionAviso.Zombi(MotivoZombi.YA_NO_TOCA), decision)
    }
}
