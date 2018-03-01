<?php

function distance($lat1, $lon1, $lat2, $lon2, $unit) {

  $theta = $lon1 - $lon2;
  $dist = sin(deg2rad($lat1)) * sin(deg2rad($lat2)) +  cos(deg2rad($lat1)) * cos(deg2rad($lat2)) * cos(deg2rad($theta));
  $dist = acos($dist);
  $dist = rad2deg($dist);
  $miles = $dist * 60 * 1.1515;
  $unit = strtoupper($unit);

  if ($unit == "K") {
    return ($miles * 1.609344);
  } else if ($unit == "N") {
    return ($miles * 0.8684);
  } else {
    return $miles;
  }
}

$home_lat = 52.04640114;
$home_lon = 5.28826868;

$ourFileName = "latlon.txt";
$fh = fopen($ourFileName, 'r') or die("can't open file");
$d = fgetcsv($fh, 1000, " ");

$lat = $d[0];
$lon = $d[1];
$time = time() - $d[2]/1000;
fclose($fh);
$dist= distance($home_lat, $home_lon, $lat, $lon, "K");
echo "#lat: " . " age:" . $time . " dist:" . $dist;



?>
