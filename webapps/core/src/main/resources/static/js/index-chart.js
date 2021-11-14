function drawCharts() {
  let url = "http://localhost:8090/";
  let yearlyRecords;

  let yearlyGrowthUrl = url + "/stat/sample/growth";
  fetch(yearlyGrowthUrl, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json'
    }
    // body: JSON.stringify(data)
  })
    .then(response => response.json())
    .then(data =>  {
      yearlyRecords = data;
      console.log(data)
    });

  const labels = ['2016', '2017', '2018', '2019', '2020', '2021'];
  const data = [6, 7, 9, 11, 15, 20];
  drawYearlySamples(labels, data);

  const chartLables = ["Homo Sapiens", "Severe acute respiratory syndrome coronavirus 2", "Mus musculus", "human gut metagenome", "soil metagenome", "metagenome", "other"];
  // const chartData = [30, 15, 10, 8, 5, 2, 30];
  const chartData = [7318661, 2326920, 2326920, 383267, 362051, 246320, 8000000];
  drawOrganismPieChart(chartLables, chartData);
}

function drawYearlySamples(labels, data) {
  const ctx = document.getElementById('yearly-growth-chart').getContext('2d');
  const yearlyGrowthChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: labels,
      datasets: [{
        label: 'Number of samples in the database',
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