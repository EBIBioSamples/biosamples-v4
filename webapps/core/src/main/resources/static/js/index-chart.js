function drawCharts(stats, yearlyGrowth) {
  console.log(stats);
  console.log(yearlyGrowth);

  let growthLabels = [];
  let grwothData = [];
  for (const k in yearlyGrowth) {
    growthLabels.push(k);
    grwothData.push(yearlyGrowth[k]);
  }
  drawYearlySamples(growthLabels, grwothData);

  let organismLabels = [];
  let organimsData = [];
  let organismFacets = stats['sampleAnalytics']['facets']['organism'];
  for (const k in organismFacets) {
    organismLabels.push(k);
    organimsData.push(organismFacets[k]);
  }
  drawOrganismPieChart(organismLabels, organimsData);

}

function drawYearlySamples(labels, data) {
  const ctx = document.getElementById('yearly-growth-chart').getContext('2d');
  const yearlyGrowthChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: labels,
      datasets: [{
        label: 'Number of samples (millions) ',
        data: data,
        backgroundColor: [
          '#007c8229'
        ],
        borderColor: [
          '#007c82'
        ],
        borderWidth: 1
      }]
    },
    options: {
      responsive: false,
      scales: {
        y: {
          beginAtZero: true
        }
      }
    }
  });
}

function drawOrganismPieChart(chartLabels, chartData) {
  let canvas = document.getElementById("organism-distribution-chart");
  let ctx = canvas.getContext('2d');

  let data = {
    labels: chartLabels,
    datasets: [
      {
        fill: true,
        backgroundColor: [
          '#007c8229',
          '#123c8229',
          '#86864f29',
          '#327926',
          '#41193b',
          '#733232',
          '#737373'],
        data: chartData,
        borderColor:	['black'],
        borderWidth: [1,1]
      }
    ]
  };

  // Notice the rotation from the documentation.
  let options = {
    responsive: false,
    title: {
      display: true,
      text: 'BioSamples Organims Distribution',
      position: 'bottom'
    },
    rotation: -0.7 * Math.PI
  };

  let organismChart = new Chart(ctx, {
    type: 'pie',
    data: data,
    options: options
  });
}