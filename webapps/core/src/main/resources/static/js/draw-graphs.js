function drawGraphs() {
    console.log("Drawing graphs");
    drawCurationGraph();
    drawSampleTypeChart();
}

function drawCurationGraph() {
    var curationData = [
        {pipeline: "curami", count: 100},
        {pipeline: "curation", count: 50},
        {pipeline: "zooma", count: 200},
        {pipeline: "copydown", count: 70}
    ];

    var width = 600,
        height = 400,
        margin = {top: 20, right: 20, bottom: 30, left: 50};
    let svg = d3.select("#sample-curation-graph").append("svg")
        .attr("width", width)
        .attr("height", height);
    let g = svg.append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    // let svg = d3.select("#sample-curation-graph").append("svg"),
    //     margin = {top: 20, right: 20, bottom: 30, left: 50},
    //     width = +svg.attr("width") - margin.left - margin.right,
    //     height = +svg.attr("height") - margin.top - margin.bottom,
    //     g = svg.append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    g.append("g")
        .attr("class", "x axis");

    g.append("g")
        .attr("class", "y axis");

    let x = d3.scaleBand()
        .padding(0.2)
        .range([0, width]);

    let y = d3.scaleLinear()
        .range([height, 0]);

    function update(curationData) {
        x.domain(curationData.map(d => d.pipeline));
        y.domain([0, d3.max(curationData, d => d.count)]);

        let points = g.selectAll(".point")
            .data(curationData); //update

        pointsEnter = points
            .enter()
            .append("rect")
            .attr("class", "point");

        points.merge(pointsEnter) //Enter + Update
            .attr("x", d => x(d.pipeline))
            .attr("y", d => y(d.count))
            .attr("width", d => x.bandwidth())
            .attr("height", d => height - y(d.count))

            .style("fill", "steelblue");

        points.exit()
            .remove();


        g.select(".x.axis")
            .call(d3.axisBottom(x))
            .attr("transform",
                "translate(0, " + height + ")");

        g.select(".y.axis")
            .call(d3.axisLeft(y));
    }


    update(curationData);

    console.log("w", width, " h", height);
}

function drawSampleTypeChart() {
    var data = [10, 20, 100];

    var width = 400,
        height = 400,
        radius = Math.min(width, height) / 2;

    var color = d3.scaleOrdinal()
        .range(["#98abc5", "#8a89a6", "#7b6888"]);

    var arc = d3.arc()
        .outerRadius(radius - 10)
        .innerRadius(0);

    var labelArc = d3.arc()
        .outerRadius(radius - 40)
        .innerRadius(radius - 40);

    var pie = d3.pie()
        .sort(null)
        .value(function(d) { return d; });

    var svg = d3.select("#sample-type-chart").append("svg")
        .attr("width", width)
        .attr("height", height)
        .append("g")
        .attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");

    var g = svg.selectAll(".arc")
        .data(pie(data))
        .enter().append("g")
        .attr("class", "arc");

    g.append("path")
        .attr("d", arc)
        .style("fill", function(d) { return color(d.data); });

    g.append("text")
        .attr("transform", function(d) { return "translate(" + labelArc.centroid(d) + ")"; })
        .attr("dy", ".35em")
        .text(function(d) { return d.data; });
}