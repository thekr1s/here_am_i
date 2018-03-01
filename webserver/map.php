<?php
$ourFileName = "latlon.txt";
$fh = fopen($ourFileName, 'r') or die("can't open file");
$d = fgetcsv($fh, 1000, " ");

$lat = $d[0];
$lon = $d[1];
$time = $d[2];
fclose($fh);


?>
<html>
<head>
<meta http-equiv="pragma" content="no-cache" />
 <script src="http://maps.google.com/maps?file=api&amp;v=2&amp;sensor=false&amp;key=ABQIAAAALTReqldhCsDgjFrZF7bsOhScMtBiaEOFzvtHpvCJOrbdX2KJHxT7Eg92mFItEKWpEsB5K0EQLyvXJw" type="text/javascript"></script>
<script type="text/javascript">

//<![CDATA[
var 	map;
var 	baseIcon;
var		polylinePoints;
var		polyline = 0;
var		polylineIdx;
var 	icon;
var		leftMarker = 0;
var		rightMarker = 0;

var fileContent='';


//Gets the browser specific XmlHttpRequest Object
function getXmlHttpRequestObject() {
	if (window.XMLHttpRequest) {
		return new XMLHttpRequest(); //Not IE
	} else if(window.ActiveXObject) {
		return new ActiveXObject("Microsoft.XMLHTTP"); //IE
	} else {
		//Display your error message here.
		//and inform the user they might want to upgrade
		//their browser.
		alert("Your browser doesn't support the XmlHttpRequest object.  Better upgrade to Firefox.");
	}
}
//Get our browser specific XmlHttpRequest object.
var receiveReq = getXmlHttpRequestObject();
//Initiate the asyncronous request.
function resetPosition() {
	var t=setTimeout("resetPosition()",6000);
	//If our XmlHttpRequest object is not in the middle of a request, start the new asyncronous call.
	if (receiveReq.readyState == 4 || receiveReq.readyState == 0) {
		//Setup the connection as a GET call to latlon.html.
		//True explicity sets the request to asyncronous (default).
		var lRand = Math.random();
		receiveReq.open("GET", "get.php", true);
		//Set the function that will be called when the XmlHttpRequest objects state changes.
		receiveReq.onreadystatechange = handleSayHello;
		//Make the actual request.
		receiveReq.send(null);
	}

}
//Called every time our XmlHttpRequest objects state changes.
function handleSayHello() {
	//Check to see if the XmlHttpRequests state is finished.
	if (receiveReq.readyState == 4) {
		//Set the contents of our span element to the result of the asyncronous call.

		var res = receiveReq.responseText.split(' ');
		try{
			SetLeftMarker(res[0], res[1]);
			document.form1.textarea1.value="Positie lat lon " + res[0] + " - " + res[1] + "\n";
			date = new Date(parseInt(res[2]));
			document.form1.textarea1.value+= "Laatste update: " + date.toLocaleString() + "\n";
		} catch (e) {
			document.form1.textarea1.value += "\nhandleSayHello " + e;
		}
	}

}


function load()
{

  if (GBrowserIsCompatible())
  {

	document.getElementById("map").style.height = document.body.offsetHeight - 20;
	map = new GMap2(document.getElementById("map"));
	map.setCenter(new GLatLng(<?=$lat ?>, <?=$lon ?>), 13);
	map.enableScrollWheelZoom();
	map.addControl(new GSmallZoomControl());
	map.addControl(new GMapTypeControl());
	baseIcon = new GIcon();
	baseIcon.shadow = "http://www.google.com/mapfiles/shadow50.png";
	baseIcon.iconSize = new GSize(10, 17);
	baseIcon.shadowSize = new GSize(18, 17);
	baseIcon.iconAnchor = new GPoint(5, 17);
	var t=setTimeout("resetPosition()",100);
  }

}

function SetLeftMarker(lat,lng)
{

  icon = new GIcon(baseIcon);
  icon.image = "http://labs.google.com/ridefinder/images/mm_20_blue.png";
  var options = {
    icon: icon,
    draggable: false,
    title: "Roberts position"
  };

  if (leftMarker != 0)
  {
	map.removeOverlay(leftMarker)
  }

  leftMarker = new GMarker(new GLatLng(0, 0),options);

  map.addOverlay(leftMarker);
  MoveLeftMarker(lat,lng);
}

function MoveLeftMarker(lat,lng)
{
  leftMarker.setLatLng(new GLatLng(lat,lng));

}


//]]>
</script>
</head>
<body class="gui" onload="load()" onunload="GUnload()">
  <div id="map" style="width: 100%; height: 70%"></div>

<form name=form1>
  <textarea name=textarea1 cols=60 rows=2 wrap=virtual></textarea>
</form>

</body>
</html>
