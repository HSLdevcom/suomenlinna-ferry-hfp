#!/bin/sh

echo "Downloading new HSL GTFS data..."
wget -O hsl-new.zip https://dev.hsl.fi/gtfs/hsl.zip

echo "Replacing old GTFS data with new data..."
ls -la hsl*
rm hsl.zip
mv hsl-new.zip hsl.zip

echo "Downloading new HSL GTFS data completed."
ls -la hsl*
