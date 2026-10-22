# Traspaso de datos entre el escritorio y el móvil

Este documento es el plan acordado para pasar los datos de la app de un dispositivo a otro
(escritorio ⇄ Android). **Todavía no está implementado**: está aquí para decidir el enfoque
antes de tocar la base de datos.

Lo que queremos poder hacer:

- Pasar **todo** (copia completa) y pasar **una sola mascota**.
- En **los dos sentidos**.
- Que importar sea **actualizar, no sustituir**: nada de duplicados y nada de borrar.

---

## 1. Por qué no vale copiar la base de datos

Es la opción que parece obvia (es un único fichero SQLite) y es la que rompe los datos. Cuatro
razones, todas del esquema actual:

1. **Los `id` son locales.** En el móvil la mascota `1` es Luna; en el escritorio la `1` es
   Rocky. Si al importar se copian las filas con su `id`, o chocan (duplicados) o se mezclan
   (las vacunas de una acaban en la otra). Y copiar el fichero entero no es una fusión:
   sustituye todo, así que no sirve ni para "solo una mascota" ni para conservar lo que haya.
2. **Las fotos y los adjuntos son rutas.** `mascotas.foto` e `informes.rutaArchivo` guardan una
   ruta del dispositivo. En el otro dispositivo esa ruta no existe: la mascota llegaría con la
   foto rota.
3. **Los hijos cuelgan de la mascota con `ON DELETE CASCADE`.** La tentación de "borro los hijos
   y los vuelvo a insertar" para fusionar una mascota **destruye** lo que hubiera de ella en el
   destino, incluido lo que no venga en el paquete.
4. **Nadie guarda cuándo se cambió una fila.** Sin una marca de tiempo no hay forma de saber si
   la vacuna del paquete es más nueva que la que ya está.

> El síntoma de hacerlo mal no es un error: son filas duplicadas que el usuario ve en la lista y
> no sabe de dónde salen.

---

## 2. La idea: identidad que viaja + fusión

Dos campos nuevos en las tablas de datos:

| Campo | Para qué |
|---|---|
| `uuid TEXT` (único) | **Reconocer la misma fila en los dos dispositivos**, sin depender del `id` local. No cambia nunca, ni al editar. |
| `actualizadoEn INTEGER` | Cuándo se tocó por última vez (milisegundos). Decide quién gana si la misma fila cambió en los dos sitios. |

E importar deja de ser "copiar" para ser **fusionar**:

- Si el `uuid` **no está** en el destino → se inserta (es nuevo).
- Si el `uuid` **ya está** y lo que llega es **más reciente** → se actualiza.
- Si el `uuid` **ya está** y lo local es **más reciente** → se queda lo local y se avisa.
- Lo que está en el destino y **no viene** en el paquete → **no se toca nunca**. Importar no
  borra.

Todo dentro de **una sola transacción**: si algo falla a mitad, no se queda medio importado.

---

## 3. Formato del paquete: un `.zip`

Un ZIP con el JSON de los datos y los ficheros al lado:

```
virtualpet-2026-10-14.zip
├── datos.json
└── fotos/
    ├── 3f2c8a1e….jpg
    └── 9b7d4c02….jpg
```

`datos.json` (con las claves en camelCase, las mismas que usa la app):

```json
{
  "formato": 1,
  "generadoEn": "2026-10-14T18:30:00Z",
  "origen": "escritorio",
  "mascotas": [
    {
      "uuid": "3f2c8a1e-…",
      "actualizadoEn": 1760461800000,
      "nombre": "Luna",
      "especie": "perro",
      "raza": "Labrador Retriever",
      "fechaNacimiento": "2020-03-15",
      "sexo": "hembra",
      "color": "Marrón",
      "microchip": "123456789",
      "pesoIdeal": 27.0,
      "foto": "fotos/3f2c8a1e-….jpg",
      "vacunas": [
        {
          "uuid": "a71f…",
          "actualizadoEn": 1760461800000,
          "nombre": "Rabia",
          "fechaAplicacion": "2025-06-01",
          "fechaProximaDosis": "2026-06-01",
          "veterinario": "Dra. Ruiz",
          "lote": "L-778"
        }
      ],
      "preventivos": [], "revisiones": [], "tratamientos": [],
      "pesos": [], "notas": [], "informes": []
    }
  ],
  "veterinarios": []
}
```

Por qué ZIP+JSON y no otras opciones:

- **Frente a copiar la base de datos**: permite fusionar, permite una sola mascota y permite
  versionar el formato.
- **Frente a JSON suelto**: las fotos y los adjuntos tienen que viajar **dentro**, o llegan
  rotos (punto 2 de arriba).

Detalles que ahorran una tarde de depuración:

- `"formato"` es un número. Si el paquete trae un formato **más nuevo** que el que entiende la
  app, se rechaza con un mensaje claro ("actualiza la app") en vez de importar a medias.
- Los campos desconocidos se **ignoran** (así una versión vieja puede leer un paquete nuevo).
- Al elegir el fichero en Android, el selector tiene que admitir `*/*`: hay proveedores que
  devuelven un ZIP como `application/octet-stream` y con el filtro `application/zip` el fichero
  sale en gris y no se puede elegir.
- El paquete se llama `virtualpet-AAAA-MM-DD.zip` y en la interfaz se explica que **no es una
  copia de seguridad de la base de datos**, sino un paquete para fusionar.

---

## 4. Los `id` locales se reescriben al importar

Al importar una mascota, el `uuid` decide **qué** fila es; el `id` local se resuelve en el
destino:

1. Buscar la mascota por `uuid`.
   - No está → insertar y quedarse con el `id` que le da SQLite (`last_insert_rowid()`).
   - Está → actualizar esa fila y quedarse con **su** `id`.
2. Todos los hijos del paquete (vacunas, pesos…) se insertan o actualizan con **ese** `id`
   local en `mascotaId`, no con el del paquete de origen.

Es el paso donde se rompen las implementaciones descuidadas, así que va cubierto con tests (ver
§8).

---

## 5. Fotos y adjuntos

- La foto de la mascota viaja en `fotos/` con el `uuid` como nombre. Al importar se copia a la
  carpeta local de fotos y se guarda **esa** ruta en `mascotas.foto`.
- Los adjuntos de informes (`informes.rutaArchivo`) van por el mismo camino. Si el fichero no
  está en el paquete (por ejemplo un paquete "solo datos"), el informe se importa con su
  descripción y se avisa en el resumen de que falta el adjunto.
- Aviso de tamaño antes de exportar: con fotos y adjuntos el ZIP puede pasar de 50 MB, y en
  Android eso hay que saberlo antes de darle a compartir por WhatsApp.

---

## 6. Reglas de fusión, en corto

| Caso | Qué se hace |
|---|---|
| El `uuid` no existe en el destino | Se inserta |
| Existe y llega más reciente | Se actualiza |
| Existe y lo local es más reciente | Se queda lo local; el resumen lo dice |
| Existe con los mismos datos | No se toca (reimportar el mismo paquete no cambia nada) |
| Está en el destino y no viene en el paquete | No se toca **nunca** |
| Llega una mascota con el mismo nombre y distinto `uuid` | **Se pregunta**: ¿es la misma (se une a la existente) o es otra (se crea)? |
| El `uuid` choca entre dos mascotas ya existentes | No puede pasar: el `uuid` es único y lo genera la app |

La última fila es la respuesta a "no quiero que se dupliquen cosas": sin identidad compartida,
**dos mascotas creadas por separado en cada dispositivo no son la misma mascota**, se llamen como
se llamen. La única pista que tiene la app es el nombre, así que en ese caso pregunta en vez de
adivinar.

---

## 7. Plan por fases

### Fase 1 — Identidad en el esquema (la base de todo) — **hecha en la 1.4**

- Migración `13.sqm` (versión 13 → **14**): añade `uuid` y `actualizadoEn` a `mascotas`,
  `vacunas`, `preventivos`, `revisiones`, `tratamientos`, `pesos`, `informes`, `notas` y
  `veterinarios`. Es **solo aditiva**: dos `ALTER TABLE … ADD COLUMN` por tabla y un `UPDATE`
  que rellena las filas existentes con `lower(hex(randomblob(16)))` y la hora actual. Ninguna
  fila se borra, se renombra ni se recrea.
- `uuid`: 32 caracteres hexadecimales en minúscula (el mismo formato que genera la app con
  `java.util.UUID.randomUUID()`). `actualizadoEn`: milisegundos desde epoch, en `INTEGER` y no
  como texto, para comparar con `<` sin depender de los decimales que escriba cada plataforma.
- Los nueve repositorios generan ambos valores en cada `insert` y refrescan `actualizadoEn` en
  cada `update`, y los dos se pueden pasar a mano: la importación tendrá que conservar los del
  paquete original.
- Verificado: `MigracionesTest` migra desde **todas** las versiones anteriores y comprueba que
  el esquema migrado es idéntico al de una instalación nueva; una prueba con las bases reales de
  `build/` (v12 y v13, con 34 filas entre las dos) confirma que no se pierde nada; y se migró en
  el emulador una base v13 de verdad, quedando `user_version = 14`, las 34 filas intactas y
  todas con identificador.

**Pendiente para la fase de importación:** sin índice sobre `uuid`, cada búsqueda por
identificador es un recorrido completo de la tabla. Con el tamaño de datos de esta app no se
nota, pero cuando exista el importador conviene añadir un índice por tabla (no se añadió ahora
para no meter un objeto más en la migración sin necesidad).

### Fase 2 — Exportar e importar, con tests (sin interfaz todavía)

- `ExportadorDatos`: lee la base de datos y escribe el ZIP (JSON + ficheros).
- `ImportadorDatos`: lee el ZIP, valida `formato`, fusiona en una transacción y devuelve un
  **resumen** (cuántas mascotas nuevas, cuántas actualizadas, cuántas se quedan como están,
  ficheros copiados, avisos).
- Tests, con dos bases de datos en memoria:
  1. Exportar de A, importar en B vacía → queda igual.
  2. Reimportar el mismo paquete → **no duplica nada**.
  3. Cambiar un peso en A, exportar, importar en B → se actualiza esa fila, no se duplica.
  4. Cambiar la misma fila en A y en B con fechas distintas → gana la más reciente.
  5. Importar un paquete **más viejo** que lo local → no revierte nada.
  6. Importar una mascota de A en B, donde B tiene otra mascota con el mismo nombre → avisa.
  7. Paquete corrupto o `formato` desconocido → error claro y **la base de datos intacta**.

### Fase 3 — Interfaz

- En la lista de mascotas, menú ⋮: **"Pasar datos"** → *Exportar todo* / *Importar*.
- En el menú ⋮ de cada mascota: **"Exportar esta mascota"**.
- Flujo de importar: elegir fichero → **enseñar el resumen** ("2 mascotas nuevas, 1
  actualizada, 3 vacunas nuevas…") → confirmar → aplicar.
- Antes de aplicar, **copia de seguridad automática** de la base de datos local en un fichero
  con fecha, y decir dónde queda.
- Se reutiliza lo que ya existe: `rememberSelectorArchivo` (elegir fichero),
  `rememberGuardarArchivo` (guardar), `rememberCompartidorArchivo` (compartir) y
  `rememberAbridorArchivo` (abrir). El "transporte" del fichero ya está resuelto.

### Fase 4 — Probarlo de verdad

PC → móvil con datos reales y móvil → PC después de tocar algo en el móvil, comprobando que no
aparece nada duplicado.

---

## 8. Decisiones a tomar antes de empezar

1. **Quién gana cuando la misma fila cambió en los dos sitios.**
   *Recomendado: la más reciente* (`actualizadoEn`), y el resumen avisa de lo que se queda
   atrás. La alternativa —"gana siempre lo que importas"— es una línea de código, pero puede
   revertir sin avisar un cambio reciente hecho en este dispositivo.
2. **Los borrados.** *Recomendado: no se propagan en la primera versión* (si borras una mascota
   en el PC, sigue en el móvil). Propagarlos pide "lápidas" (`borradoEn`) y una regla más:
   merece una fase aparte.
3. **Los veterinarios.** *Recomendado: viajan en el traspaso completo, no al pasar una sola
   mascota* (son una agenda común, no cuelgan de ninguna mascota).
4. **Adjuntos grandes en el paquete.** *Recomendado: incluirlos*, avisando del tamaño antes de
   exportar.

---

## 9. Lo que no haría en la primera versión

- **Fusión campo a campo** (que un cambio de teléfono sobreviva a uno de dirección): multiplica
  la complejidad y los casos raros para un problema que casi nunca se da.
- **Resolución de conflictos a mano**, fila por fila: el resumen + "gana la más reciente" cubre
  el 95 % de los casos.
- **Sincronización automática por red**: hace falta servidor o cuenta, y este proyecto es
  offline a propósito. El fichero que se pasa a mano es honesto y suficiente.
- **Usar el `id` local como identidad "porque de momento funciona"**: es exactamente el atajo
  que crea los duplicados que queremos evitar.

---

## 10. Relojes: el único aviso honesto

`actualizadoEn` sale del reloj de cada dispositivo. Si un dispositivo tiene la hora mal, sus
cambios pueden "ganar" o "perder" sin motivo. Con dos dispositivos del mismo dueño el desfase
suele ser de segundos, pero conviene saberlo:

- El resumen **enseña siempre** qué se va a actualizar y qué se queda como está, antes de tocar
  nada.
- Si algún día se sincroniza a menudo, se puede añadir un aviso cuando la fecha de un paquete
  sea **más de un día** posterior a la hora actual ("revisa la hora de este dispositivo").
