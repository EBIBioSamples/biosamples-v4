function graph_search(base_url) {
    let attributeL1 = $("#attributeL1").val();
    let valueL1 = $("#valueL1").val();
    let referenceL1 = $("#referenceL1").val();
    let relationship1 = $("#relationship1").val();
    let attributeR1 = $("#attributeR1").val();
    let valueR1 = $("#valueR1").val();
    let referenceR1 = $("#referenceR1").val();

    let nodes = [];
    let links = [];

    if (attributeL1 && valueL1) {
        nodes.push({
            id: "a1",
            type: "Sample",
            attributes: {[attributeL1]: valueL1}
        })
    }

    if (attributeR1 && valueR1) {
        nodes.push({
            id: "a2",
            type: "Sample",
            attributes: {[attributeR1]: valueR1}
        });
    }

    if (relationship1) {
        links.push({
            type: relationship1,
            startNode: "a1",
            endNode: "a2"
        });

        if (!(attributeL1 && valueL1)) {
            nodes.push({
                id: "a1",
                type: "Sample"
            })
        }

        if (!(attributeR1 && valueR1)) {
            nodes.push({
                id: "a2",
                type: "Sample"
            })
        }
    } else {
        if (((attributeL1 && valueL1) || referenceL1) && ((attributeR1 && valueR1) || referenceR1)) {
            links.push({
                type: "ANY",
                startNode: "a1",
                endNode: "a2"
            });
        }
    }

    if (referenceL1) {
        nodes.push({
            id: "a3",
            type: "ExternalEntity",
            attributes: {archive: referenceL1}
        });
        links.push({
            type: "EXTERNAL_REFERENCE",
            startNode: "a1",
            endNode: "a3"
        });
        //Need empty left node if left external reference is existing
        if (!(attributeL1 && valueL1) && !relationship1) {
            nodes.push({
                id: "a1",
                type: "Sample"
            })
        }
    }

    if (referenceR1) {
        nodes.push({
            id: "a4",
            type: "ExternalEntity",
            attributes: {archive: referenceR1}
        });
        links.push({
            type: "EXTERNAL_REFERENCE",
            startNode: "a2",
            endNode: "a4"
        });
        //Need empty right node if right external reference is existing
        if (!(attributeL1 && valueL1) && !relationship1) {
            nodes.push({
                id: "a2",
                type: "Sample"
            })
        }
    }

    let request = {
        nodes: nodes,
        links: links
    };

    $.ajax({
        type: 'post',
        url: base_url + 'graph/search',
        data: JSON.stringify(request),
        contentType: "application/json; charset=utf-8",
        traditional: true,
        success: function (data) {
            samples_url = base_url + 'samples/';
            let samples = [];
            console.log(data);

            const links = data["links"].filter(function (link) {
                return link.type !== "EXTERNAL_REFERENCE"
            });
            const externalLinks = data["links"].filter(function (link) {
                return link.type === "EXTERNAL_REFERENCE"
            });
            // const page = data["nodes"].reduce((map, node) => (map[node.id] = node, map), {});
            let nodes = new Map(data["nodes"].map(i => [i.id, i]));
            const page = data["page"];
            const size = data["size"];
            const total = data["total"];

            console.log(links);
            console.log(nodes);

            // var graphSearchFacet = $("#graph-search-facet").empty();
            var graphSearchResults = $("#graph-search-results").empty();

            if (links.length !== 0) {
                links.forEach(function (link, index) {
                    let linkType = link.type.replace("_", " ");
                    let startNode = nodes.get(link.startNode);
                    let endNode = nodes.get(link.endNode);

                    let parentDiv = $("<div/>").attr("id", "link-" + index).addClass("graph-search-record medium-12");
                    let sourceDiv = $("<div/>").addClass("card columns medium-4 graph-search-record-sample");
                    let typeDiv = $("<div/>").addClass("graph-search-record-link-type columns medium-4");
                    let targetDiv = $("<div/>").addClass("card columns medium-4 graph-search-record-sample");

                    var accessionSpan = $("<span/>").addClass("lead graph-search-record-sample-span").html(startNode["attributes"]["accession"]);
                    var accessionLink = $("<a/>").attr("href", samples_url + startNode["attributes"]["accession"]).append(accessionSpan);
                    var nameSpan = $("<span/>").addClass("graph-search-record-sample-span").html(startNode["attributes"]["name"]);
                    sourceDiv.append(accessionLink).append(nameSpan);

                    let typeSpan = $("<span/>").addClass("lead text-center").html(linkType);
                    typeDiv.append(typeSpan);

                    accessionSpan = $("<span/>").addClass("lead graph-search-record-sample-span").html(endNode["attributes"]["accession"]);
                    accessionLink = $("<a/>").attr("href", samples_url + endNode["attributes"]["accession"]).append(accessionSpan);
                    nameSpan = $("<span/>").addClass("graph-search-record-sample-span").html(endNode["attributes"]["name"]);
                    targetDiv.append(accessionLink).append(nameSpan);

                    parentDiv.append(sourceDiv);
                    parentDiv.append(typeDiv);
                    parentDiv.append(targetDiv);
                    graphSearchResults.append(parentDiv);
                });
            } else {
                data["nodes"].forEach(function (node, index) {
                    if (node["type"] !== "ExternalEntity") {
                        var parentDiv = $("<div/>").attr("id", "sample-" + index).addClass("card columns medium-12");
                        var backgroundColor = index % 2 === 0 ? "background: #f0f0f2;" : "background: #ffffff;";
                        parentDiv.attr("style", backgroundColor);

                        var nameSpan = $("<span/>").addClass("lead text-left").html(node["attributes"]["name"]);
                        var nameLink = $("<a/>").attr("href", samples_url + node["attributes"]["accession"]).append(nameSpan);
                        var nameParentSpan = $("<span/>").addClass("columns medium-9").append(nameLink);
                        parentDiv.append(nameParentSpan);

                        var accessionSpan = $("<span/>").addClass("text-right float-right").html(node["attributes"]["accession"]);
                        var accessionLink = $("<a/>").attr("href", samples_url + node["attributes"]["accession"]).append(accessionSpan);
                        var accParentSpan = $("<span/>").addClass("columns medium-3").append(accessionLink);
                        parentDiv.append(accParentSpan);

                        graphSearchResults.append(parentDiv);
                    }
                })
            }
        }
    });
}

function execute_example_query(base_url, attributeL1, valueL1, referenceL1, relationship1, attributeR1, valueR1, referenceR1) {
    $("#attributeL1").val(attributeL1);
    $("#valueL1").val(valueL1);
    $("#referenceL1").val(referenceL1);
    $("#relationship1").val(relationship1);
    $("#attributeR1").val(attributeR1);
    $("#valueR1").val(valueR1);
    $("#referenceR1").val(referenceR1);

    graph_search(base_url)
}