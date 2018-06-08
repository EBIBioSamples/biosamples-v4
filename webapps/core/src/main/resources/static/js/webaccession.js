if (window.File && window.FileReader && window.FileList && window.Blob) {
    // Great success! All the File APIs are supported.
} else {
    alert('The File APIs are not fully supported in this browser.');
}

(function ($) {
    var errorsDiv, apiKeyInput, apiKey,
        sampleTabFilePicker, sampleTabFilePickerLabel,
        form, spinner, submissionUrl,
        checkedTerms;

    $(document).ready(function () {
        errorsDiv = document.getElementById("errorsdiv");
        form = document.getElementById("sampletab_form");
        sampleTabFilePicker = document.getElementById("sampletab_file_picker");
        sampleTabFilePickerLabel = document.querySelector("label[for=sampletab_file_picker]");
        spinner = document.getElementById("processing-spinner");
        checkedTerms = document.getElementById('accept-terms');
        // apiKeyInput = document.getElementById("apikeyinput");

        sampleTabFilePicker.addEventListener('change', function(evt) {
                evt.preventDefault();
                genericFileSubmissionHandler(form)
            },
            {
                capture: false
            }
        );

        checkedTerms.addEventListener('change', function(evt) {
            evt.preventDefault();
            sampleTabFilePickerLabel.classList.toggle('disabled', !this.checked);
            sampleTabFilePicker.disabled = !this.checked;
        }, {
            capture: false
        })


    });


    /**
     * Submit the form using AJAX
     * @param form the HTML form element
     */
    function genericFileSubmissionHandler(form) {
        // errorsDiv = document.getElementById("errorsdiv");
        let formData = new FormData(form);
        let submissionURL = form.getAttribute("action");
        spinner.style.visibility = "visible";
        // let apiKey = formData.get("apikey");
        // if (apiKey !== null) {
        //     submissionURL = submissionURL + "?apikey=" + apiKey;
        // }

        $.ajax({
            type: 'POST',
            url: submissionURL,
            data: formData,
            enctype: 'multipart/form-data',
            contentType: false,
            processData: false,
            success: function (json) {
                //once the ajax call is complete, display the output
                //through this callback
                doResponse(json.errors, json.sampletab)
            },
            error: handleAJAXError,
            complete: function() {
                spinner.style.visibility = "hidden";
            }
        });
    }

    /**
     * Handle AJAX response from the server
     * @param errors the error list
     * @param sampleTab the processed SampleTab
     */
    function doResponse(errors, sampleTab) {
        //TODO process errors nicely
        if (errors.length > 0) {
            clearErrors();
            showErrors(errors);

        } else {
            storeProcessedSampletab(sampleTab);
        }

    }

    /**
     * Generate a table with the provided errors and append that to the page
     * @param errors
     */
    function showErrors(errors) {
        var errorTable = document.createElement('table');
        var tableRow = document.createElement('tr');
        errorTable.appendChild(tableRow);

        var tableData = document.createElement('th');
        tableData.innerHTML = "Error Message";
        tableRow.appendChild(tableData);
        tableData = document.createElement('th');
        tableData.innerHTML = "Comment";
        tableRow.appendChild(tableData);
        for (var i = 0; i < errors.length; i++) {

            var error = errors[i];
            tableRow = document.createElement('tr');
            errorTable.appendChild(tableRow);
            tableData = document.createElement('td');
            tableData.innerHTML = error.message;
            tableRow.appendChild(tableData);
            tableData = document.createElement('td');
            tableData.innerHTML = error.comment;
            tableRow.appendChild(tableData);
            //DEBUG
        }
        errorsDiv.appendChild(errorTable);
    }

    /**
     * Displays a file picker to save the returned SampleTab
     * @param sampleTab: the JSON matrix returned from the server
     */
    function storeProcessedSampletab(sampleTab) {
        var sampleTabString = JSON2DArrayToString(sampleTab);

        //in order to download the sampletab string
        //it needs to be echoes off the server due to
        //javascript security restrictions

        //to do that, we create a invisible form
        var myForm = document.createElement("form");
        myForm.method = "post";
        myForm.action = "api/echo";

        //attach download string to form as a multiline textbox
        var myInput = document.createElement("textarea");
        myInput.setAttribute("cols", 1);
        myInput.setAttribute("rows", 1);
        myInput.setAttribute("name", "input");
        myInput.innerHTML = sampleTabString;

        myForm.appendChild(myInput);
        document.body.appendChild(myForm);
        //send the form, which should trigger a download
        myForm.submit();
        //clean up afterwards
        document.body.removeChild(myForm);

    }

    /**
     * Append an error message to the error div
     * @param msg: the message to append
     */
    function displayError(msg) {
        errorsDiv.appendChild(generateErrorLabel(msg));
    }

    /**
     * Clean error div content
     */
    function clearErrors() {
        errorsDiv.innerHTML = "";
    }

    /**
     * Create an HTML error label with the provided message
     * @param msg: the error label content
     * @returns {HTMLSpanElement} the error label HTML element
     */
    function generateErrorLabel(msg) {
        var message = isValidString(msg) ? msg : "An error occurred while processing your request.";
        var labelSpan = document.createElement("span");
        labelSpan.classList.add("label", "warning");
        labelSpan.style = "padding: 5px; font-size:1.2rem";
        labelSpan.textContent = message;
        return labelSpan;
    }


    /**
     * Check if the provided string is valid
     * @param string: the string to verify
     * @returns {boolean} if the string is a not undefined string
     */
    function isValidString(string) {
        return string !== undefined && typeof string === "string";
    }

    /**
     * Function to handle AJAX errors
     * @param request the request that generated the error
     * @param error_type
     * @param error_mesg
     */
    function handleAJAXError(request, error_type, error_mesg) {
        //if the ajax call when awry, tell the user
        clearErrors();
        displayError("Oops! Something went wrong while processing your request");
        var errorDetails = "" + error_type + ": " + error_mesg;
        if (errorDetails.length > 0) {
            displayError(generateErrorLabel(errorDetails))
        }
    }

    /**
     * Function that convert a JSON matrix back to a single string
     * @param array the JSON matrix
     * @returns {string} the string
     */
    function JSON2DArrayToString(array) {
        var response = "";
        for (var i = 0; i < array.length; i++) {
            var line = array[i];
            for (var j = 0; j < line.length; j++) {
                var cell = line[j];
                response = response + cell
                response = response + "\t";
            }
            response = response + "\r\n"; //use windows line endings for best safety
        }
        return response;
    }

})(jQuery);
