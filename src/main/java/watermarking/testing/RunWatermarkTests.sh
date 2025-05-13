#!/bin/bash
echo "Running Watermark Testing and Graph Generation..."

echo
echo "Step 1: Compiling Java classes..."
javac -cp ".:./lib/*" watermarking/testing/*.java

echo
echo "Step 2: Running Protocol Tests (this may take a while)..."
java -cp ".:./lib/*" watermarking.testing.ProtocolBatchRunner

echo
echo "Step 3: Generating Graph Data..."
java -cp ".:./lib/*" watermarking.testing.WatermarkComparisonGraphs

echo
echo "Testing and graph data generation complete!"
echo
echo "You can find the results in:"
echo "- protocol-results/ (raw test results)"
echo "- graph-data/ (CSV files for creating graphs)"
echo

read -p "Press [Enter] to exit..."