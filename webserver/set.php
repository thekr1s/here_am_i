<html><body>
<?php
$lat = $_POST['lat'];
$lon = $_POST['lon'];
$time = $_POST['time'];

echo "SET: ". $lat . " " . $lon . ".<br />";

$ourFileName = "latlon.txt";
$fh = fopen($ourFileName, 'w') or die("can't open file");
fwrite($fh, $lat . " " . $lon . " " . $time . " ");
fclose($fh);
?>
</body></html>
