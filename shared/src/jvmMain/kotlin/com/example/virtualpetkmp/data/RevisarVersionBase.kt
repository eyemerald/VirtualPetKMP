package com.example.virtualpetkmp.data

import app.cash.sqldelight.db.QueryResult
import com.example.virtualpetkmp.db.VirtualPetDatabase
import java.io.File
import javax.swing.JOptionPane
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.base_mas_nueva_mensaje
import virtualpetkmp.shared.generated.resources.base_mas_nueva_titulo

/**
 * Revisa, **sin abrir la app**, si el fichero de la base es de una versión más nueva que esta.
 *
 * Es lo que usa el arranque de escritorio: si la base es más nueva, se enseña un aviso del sistema
 * y no se abre la ventana. Comprobarlo antes es necesario porque, si la comprobación se hace dentro
 * de la composición y se corta el arranque, la ventana se queda en blanco (no pinta nada).
 *
 * Abre el fichero y lo cierra: no deja ninguna conexión abierta ni toca nada.
 *
 * @param archivo base de datos a revisar; por defecto la de la app en el equipo.
 * @return la excepción que describe el problema, o `null` si la base se puede abrir.
 */
fun revisarVersionDeLaBaseDeDatos(
    archivo: File = File(System.getProperty("user.home"), ".virtualpet/virtualpet.db")
): BaseDeDatosMasNuevaException? {
    if (!archivo.exists() || archivo.length() == 0L) return null

    val driver = crearDriverJdbc(archivo)
    return try {
        val version = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version;",
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0) ?: 0L)
            },
            parameters = 0
        ).value

        if (version > VirtualPetDatabase.Schema.version) {
            BaseDeDatosMasNuevaException(
                versionFichero = version,
                versionApp = VirtualPetDatabase.Schema.version
            )
        } else {
            null
        }
    } finally {
        driver.close()
    }
}

/**
 * Si la base es más nueva que la app, enseña un aviso del sistema y devuelve `true`.
 *
 * Lo llama el arranque de escritorio **antes** de abrir la ventana. El texto sale de los recursos
 * de la app (los seis idiomas), leídos aquí con `getString` fuera de la composición: por eso esta
 * función vive en `shared` y no en `desktopApp`, donde los accesos a los recursos son `internal`.
 *
 * Se usa un diálogo del sistema y no una pantalla de Compose porque, al cortar el arranque antes de
 * montar la interfaz, la ventana se quedaba en blanco (comprobado).
 */
fun avisarSiLaBaseEsMasNueva(): Boolean {
    val problema = revisarVersionDeLaBaseDeDatos() ?: return false

    val titulo = runBlocking { getString(Res.string.base_mas_nueva_titulo) }
    val mensaje = runBlocking {
        getString(
            Res.string.base_mas_nueva_mensaje,
            problema.versionFichero,
            problema.versionApp
        )
    }
    JOptionPane.showMessageDialog(null, mensaje, titulo, JOptionPane.ERROR_MESSAGE)
    return true
}
