@Composable
fun LineChartScreen(
    viewModel: MascotaViewModel,
    onMascotaClick: (Long) -> Unit
) {
    val mascotas by viewModel.mascotas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gráfico de Líneas Suaves") },
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
                        LineChart(mascota)
                    }
                }
            }
        }
    )
}

@Composable
fun LineChart(mascota: Mascota) {
    val context = LocalContext.current
    val entries = remember {
        mutableStateListOf<Entry>()
    }

    // Aquí puedes agregar los datos para el gráfico
    // Por ejemplo:
    entries.add(Entry(0f, 10f))
    entries.add(Entry(1f, 15f))
    entries.add(Entry(2f, 12f))
    entries.add(Entry(3f, 18f))
    entries.add(Entry(4f, 20f))

    val dataSet = LineDataSet(entries, "Datos de ejemplo")
    dataSet.lineWidth = 2f
    dataSet.circleRadius = 4f
    dataSet.setDrawCircleHole(false)
    dataSet.color = Color.BLUE
    dataSet.valueTextColor = Color.BLACK
    dataSet.valueTextSize = 10f

    val data = LineData(dataSet)

    LineChart(context).apply {
        data = data
        description.isEnabled = false
        legend.isEnabled = false
        xAxis.isEnabled = true
        xAxis.labelCount = entries.size
        xAxis.valueFormatter = IndexAxisValueFormatter(entries.map { it.x.toString() })
        yAxis.isEnabled = true
    }
}
