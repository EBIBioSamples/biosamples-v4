var executed = false; // avoid multiple executions

function getFacets(path, load) {
    if (!executed) {
        executed = true;
        if (load) {
            urlParam = window.location.href.slice(window.location.href.indexOf('?') + 1);
            var url = path + "?" + urlParam;
            $("#facet_div").load(url, enableFacetButtons);
        } else {
            $("#facets-ajax-loading-img").remove();
        }
    }
}

function enableFacetButtons() {
    $('#local-searchbox').autocomplete({
        serviceUrl: '[(@{/samples/autocomplete})]',
        paramName: 'text',
        onSelect: function (suggestion) {
            //location.href = '[(@{/samples})]?'+$.param({text:suggestion.value});
        }
    });
    let $filterApplyButton = $("#filter-apply");
    let $filterClearButton = $("#filter-clear");
    let $filtersCheckboxes = $(".facet-checkbox");
    let $originalState = [];
    let $changeState = [];

    $(".facet").each(function(index, elem) {
        $originalState[index] = $(elem).hasClass("secondary");
        $changeState[index] = $originalState[index];
    });

    $filtersCheckboxes.each(function(index,elem){
        $(elem).on("click", function(){
            $changeState[index] = !$changeState[index];
            if (!equals($originalState, $changeState)) {
                $filterApplyButton.prop('disabled', false);
            } else {
                $filterApplyButton.prop('disabled', true);
            }
        })
    });

    function equals(array1, array2) {
        return array1.toString() === array2.toString();
    }

}