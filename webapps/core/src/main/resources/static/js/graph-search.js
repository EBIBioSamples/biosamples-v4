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
        })
    }

    if (relationship1) {
        links.push({
            type: relationship1,
            startNode: "a1",
            endNode: "a2"
        });
        if (!(attributeR1 && valueR1)) {
            nodes.push({
                id: "a2",
                type: "Sample"
            })
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
    }


    let request = {
        nodes : nodes,
        links: links
    };

    /*let request = {
        nodes: [
            {
                id: "a1",
                type: "Sample",
                attributes: {[attributeL1]: valueL1}
            }, {
                id: "a2",
                type: "Sample",
                attributes: {[attributeR1]: valueR1}
            }, {
                id: "a3",
                type: "ExternalEntity",
                attributes: {archive: referenceL1}
            }, {
                id: "a4",
                type: "ExternalEntity",
                attributes: {archive: referenceR1}
            }
        ],
        links: [{
            type: relationship1,
            startNode: "a1",
            endNode: "a2"
        }, {
            type: "EXTERNAL_REFERENCE",
            startNode: "a1",
            endNode: "a3"
        }, {
            type: "EXTERNAL_REFERENCE",
            startNode: "a2",
            endNode: "a4"
        }]
    };*/

    $.ajax({
        type: 'post',
        url: base_url + 'graph/search',
        data: JSON.stringify(request),
        contentType: "application/json; charset=utf-8",
        traditional: true,
        success: function (data) {
            samples_url = base_url + 'samples/';
            let samples = [];
            if ("_embedded" in data && "samples" in data["_embedded"]) {
                samples = data["_embedded"]["samples"];
            }
            let page = data["page"];

            var graphSearchFacet = $("#graph-search-facet").empty();
            var graphSearchResults = $("#graph-search-results").empty();

            samples.forEach(function (item, index) {
                var parentDiv = $("<div/>").attr("id", "sample-" + index).addClass("card columns medium-12");
                var backgroundColor = index % 2 === 0 ? "background: #f0f0f2;" : "background: #ffffff;";
                parentDiv.attr("style", backgroundColor);

                var nameSpan = $("<span/>").addClass("lead text-left").html(item["name"]);
                var nameLink = $("<a/>").attr("href", samples_url + item["accession"]).append(nameSpan);
                var nameParentSpan = $("<span/>").addClass("columns medium-9").append(nameLink);
                parentDiv.append(nameParentSpan);

                var accessionSpan = $("<span/>").addClass("text-right float-right").html(item["accession"]);
                var accessionLink = $("<a/>").attr("href", samples_url + item["accession"]).append(accessionSpan);
                var accParentSpan = $("<span/>").addClass("columns medium-3").append(accessionLink);
                parentDiv.append(accParentSpan);

                var updateSpan = $("<span/>").addClass("text-right float-left").html("Updated On: " + item["update"]);
                // var updateP = $("<p/>").addClass("small").html().append(updateSpan);
                var updateParentSpan = $("<span/>").addClass("small column medium-12").append(updateSpan);
                parentDiv.append(updateParentSpan);

                graphSearchResults.append(parentDiv);
            });

            var facetCarddiv =  $("<div/>").addClass("card column graph-search-facet")
                .html("<h5>Results summary (this is fake data)</h5> Total Samples: 123<br> Total Relationships: 123<br>");
            graphSearchFacet.append(facetCarddiv);

            facetCarddiv =  $("<div/>").addClass("card column graph-search-facet")
                .html("<h5>Relationships</h5>SAME AS: 100<br>DERIVED FROM: 300<br>EXTERNAL REF: 300<br>");
            graphSearchFacet.append(facetCarddiv);

            facetCarddiv =  $("<div/>").addClass("card column graph-search-facet")
                .html("Page: " + page["number"]);
            graphSearchFacet.append(facetCarddiv);
        }
    });



}