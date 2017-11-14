if (window.File && window.FileReader && window.FileList && window.Blob) {
  // Great success! All the File APIs are supported.
} else {
  alert('The File APIs are not fully supported in this browser.');
}

function handleAccessionFileSelect(evt) {
	var file = evt.target.files[0];
	
	var filereader = new FileReader();
	
	//TODO some fancy loader swirly 
	//setup the callback used when loading the file from disk
	filereader.onload = (function(e) {
		//convert the string into a JSON array of arrays
		var sampletabstring = stringToJSON2DArray(e.target.result)
		//retrieve the API key to use
		var apikey = document.getElementById('apikeyinput').value;
		
		//do the ajax call
	    $.ajax({
           type:           'POST',
           url:            'api/v1/json/ac?apikey='+apikey, //TODO do this better
           contentType:    'application/json',
           data:           sampletabstring,
           processData:    false,
           success: function(json) {
        	   //once the ajax call is complete, display the output
        	   //through this callback
        	   doResponse(json.errors, json.sampletab)
           },
           error: function(request, error_type, error_mesg) {
        	   //if the ajax call when awry, tell the user
               alert('Oops! Something went wrong whilst trying to display your results.\n' +
                             'A script on this page said...\n' +
                             '\"' + error_type + ': ' + error_mesg + '\"');
           }
       });
	})
	//now setup is complete, actually read the file
	filereader.readAsText(file, "UTF-8");
}

function handleValidationFileSelect(evt) {
	var file = evt.target.files[0];
	
	var filereader = new FileReader();
	
	//TODO some fancy loader swirly 
	//setup the callback used when loading the file from disk
	filereader.onload = (function(e) {
		//convert the string into a JSON array of arrays
		var sampletabstring = stringToJSON2DArray(e.target.result)
		
		//do the ajax call
	    $.ajax({
           type:           'POST',
           url:            'api/v1/json/va',
           contentType:    'application/json',
           data:           sampletabstring,
           processData:    false,
           success: function(json) {
        	   //once the ajax call is complete, display the output
        	   //through this callback
        	   doResponse(json.errors, json.sampletab)
           },
           error: function(request, error_type, error_mesg) {
        	   //if the ajax call when awry, tell the user
               alert('Oops! Something went wrong whilst trying to display your results.\n' +
                             'A script on this page said...\n' +
                             '\"' + error_type + ': ' + error_mesg + '\"');
           }
       });
	})
	//now setup is complete, actually read the file
	filereader.readAsText(file, "UTF-8");
}

function handleSubmissionFileSelect(evt) {
	var file = evt.target.files[0];
	
	var filereader = new FileReader();
	
	//TODO some fancy loader swirly 
	//setup the callback used when loading the file from disk
	filereader.onload = (function(e) {
		//convert the string into a JSON array of arrays
		var sampletabstring = stringToJSON2DArray(e.target.result)
		//retrieve the API key to use
		var apikey = document.getElementById('apikeyinput').value;
		
		//do the ajax call
	    $.ajax({
           type:           'POST',
           url:            'api/v1/json/sb?apikey='+apikey, //TODO do this better
           contentType:    'application/json',
           data:           sampletabstring,
           processData:    false,
           success: function(json) {
        	   //once the ajax call is complete, display the output
        	   //through this callback
        	   doResponse(json.errors, json.sampletab)
           },
           error: function(request, error_type, error_mesg) {
        	   //if the ajax call when awry, tell the user
               alert('Oops! Something went wrong whilst trying to display your results.\n' +
                             'A script on this page said...\n' +
                             '\"' + error_type + ': ' + error_mesg + '\"');
           }
       });
	})
	//now setup is complete, actually read the file
	filereader.readAsText(file, "UTF-8");
}

function doResponse(errors, sampletab) {
	//TODO process errors nicely
	if (errors.length > 0){
		var errorsdiv = document.getElementById('errorsdiv');
		//clear any previous errors
		errorsdiv.innerHTML = "";
		//create the table
		var errortable = document.createElement('table');
		var tablerow = document.createElement('tr');
		errortable.appendChild(tablerow);
		var tabledata = document.createElement('th');
		tabledata.innerHTML = "Error Message";
		tablerow.appendChild(tabledata)
		tabledata = document.createElement('th');
		tabledata.innerHTML = "Comment";
		tablerow.appendChild(tabledata)
		for (var i = 0; i < errors.length; i++) {
			var error = errors[i];
			tablerow = document.createElement('tr');
			errortable.appendChild(tablerow);
			tabledata = document.createElement('td');
			tabledata.innerHTML = error.message;
			tablerow.appendChild(tabledata);
			tabledata = document.createElement('td');
			tabledata.innerHTML = error.comment;
			tablerow.appendChild(tabledata);
			//DEBUG
			//alert(error);
		}
		errorsdiv.appendChild(errortable);
		
	} else {
	    //convert the JSON array of arrays into a single
	    //string with tabs and newlines
	    var sampletabstring = JSON2DArrayToString(sampletab);
	    
	    //in order to download the sampletab string
	    //it needs to be echoes off the server due to
	    //javascript security restrictions
	    
	    //to do that, we create a invisible form 
	    var myForm = document.createElement("form");
	    myForm.method="post" ;
	    myForm.action = "api/echo" ;
	    
	    //attach download string to form as a multiline textbox
	    var myInput = document.createElement("textarea") ;
	    myInput.setAttribute("cols", 1) ;
	    myInput.setAttribute("rows", 1) ;
	    myInput.setAttribute("name", "input") ;
	    myInput.innerHTML = sampletabstring;
	    
	    myForm.appendChild(myInput) ;
	    document.body.appendChild(myForm) ;
	    //send the form, which should trigger a download
	    myForm.submit() ;
	    //clean up afterwards
	    document.body.removeChild(myForm) ;
	}
    
}

function stringToJSON2DArray(myString) {
    var content = new Array();
    var lines = null;
    //escape doublequote characters
    myString = myString.replace(/\"/g, "\\\"");
    ///split by different line endings depending what are present in source
    if (myString.indexOf("\r\n") != -1) {
    	lines = myString.split("\r\n");
    } else if (myString.indexOf("\r") != -1) {
    	lines = myString.split("\r");
    } else if (myString.indexOf("\n") != -1) {
    	lines = myString.split("\n");
    }
    for (var i = 0; i<lines.length; i++) {
        var line = new Array();
        var cells = lines[i].split("\t");
        for (var j = 0; j < cells.length; j++) {
            line.push("\"" + cells[j] + "\"");
        }
        content.push("[" + line + "]");
    }
    var jsonstring =  "{\"sampletab\" : [" + content + "]}";
    $.parseJSON(jsonstring);
    return jsonstring;
}

function JSON2DArrayToString(array) {
	var response = "";
	for (var i=0; i < array.length; i++){
		var line = array[i];
		for (var j=0; j < line.length; j++){
			var cell = line[j];
			response = response + cell
			response = response + "\t";
		}
		response = response + "\r\n"; //use windows line endings for best safety
	}
	return response;
}

$(document).ready(function() {
	if (document.getElementById('pickfileaccession') != null) {
		document.getElementById('pickfileaccession').addEventListener('change', handleAccessionFileSelect, false);
	}
	});

$(document).ready(function() {
	if (document.getElementById('pickfilevalidation') != null) {
		document.getElementById('pickfilevalidation').addEventListener('change', handleValidationFileSelect, false);
	}
	});

$(document).ready(function() {
	if (document.getElementById('pickfilesubmission') != null) {
		document.getElementById('pickfilesubmission').addEventListener('change', handleSubmissionFileSelect, false);
	}
	});