<?php
$ourFileName = "latlon.txt";
$fh = fopen($ourFileName, 'r') or die("can't open file");
$d = fgetcsv($fh, 1000, " ");

$lat = $d[0];
$lon = $d[1];
$time = $d[2];
fclose($fh);

echo $lat . " " . $lon . " " . $time;

?>
