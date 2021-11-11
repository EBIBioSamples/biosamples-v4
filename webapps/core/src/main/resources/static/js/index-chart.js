function drawCharts() {
  const labels = ['2016', '2017', '2018', '2019', '2020', '2021'];
  const data = [6, 7, 9, 11, 15, 20];
  drawYearlySamples(labels, data);

  const chartLables = ["Homo Sapiens", "Severe acute respiratory syndrome coronavirus 2", "Mus musculus", "human gut metagenome", "soil metagenome", "metagenome", "other"];
  // const chartData = [30, 15, 10, 8, 5, 2, 30];
  const chartData = [7318661, 2326920, 2326920, 383267, 362051, 246320, 8000000];
  drawOrganismPieChart(chartLables, chartData);
}

function drawYearlySamples(labels, data) {
  const ctx = document.getElementById('myChart').getContext('2d');
  const myChart = new Chart(ctx, {
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
  var canvas = document.getElementById("myChart2");
  var ctx2 = canvas.getContext('2d');

  var data = {
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
// Notice the borderColor
        borderColor:	['black'],
        borderWidth: [1,1]
      }
    ]
  };

  // Notice the rotation from the documentation.
  var options = {
    responsive: false,
    title: {
      display: true,
      text: 'What happens when you lend your favorite t-shirt to a girl ?',
      position: 'bottom'
    },
    rotation: -0.7 * Math.PI
  };

  // Chart declaration:
  var myBarChart = new Chart(ctx2, {
    type: 'pie',
    data: data,
    options: options
  });
}