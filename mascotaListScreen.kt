@Composable
fun MascotaListScreen(
    viewModel: MascotaViewModel,
    onMascotaClick: (Long) -> Unit
) {
    val mascotas by viewModel.mascotas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Mascotas") },
                backgroundColor = MaterialTheme.colors.primary
            )
        },
        content = { padding ->
            if (isLoading) {
                Box(modifier = Modifier.padding(padding)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.padding(padding)) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding)) {
                    items(mascotas) { mascota ->
                        MascotaCard(
                            mascota = mascota,
                            onClick = { onMascotaClick(mascota.id!!) }
                        )
                    }
                }
            }
        }
    )
}

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
