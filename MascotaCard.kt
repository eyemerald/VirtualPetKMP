@Composable
fun MascotaCard(
    mascota: Mascota,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .clickable(onClick = onClick),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = mascota.nombre,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Especie: ${mascota.especie}",
                style = MaterialTheme.typography.body2
            )
            Text(
                text = "Raza: ${mascota.raza}",
                style = MaterialTheme.typography.body2
            )
            Text(
                text = "Fecha de Nacimiento: ${mascota.fechaNacimiento.toFormatoEuropeo()}",
                style = MaterialTheme.typography.body2
            )
        }
    }
}
