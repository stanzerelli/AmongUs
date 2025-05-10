#!/bin/bash

# Script to run benchmarks on AMD Threadripper 3990X
# This script configures and runs benchmarks with optimal settings for the Threadripper

# Set JAVA_HOME if needed
# export JAVA_HOME=/path/to/java

# Set memory settings for large images
export JVM_OPTS="-Xms4g -Xmx64g"

# Create a timestamp for the results
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
OUTPUT_DIR="benchmark_results_${TIMESTAMP}"
mkdir -p $OUTPUT_DIR

echo "Starting benchmarks on $(hostname) at $(date)"
echo "CPU Info: $(grep 'model name' /proc/cpuinfo | head -1)"
echo "Memory: $(free -h | grep Mem | awk '{print $2}')"
echo "Java version: $(java -version 2>&1 | head -1)"

# Run benchmarks for different image sizes
IMAGES=("images/place_2k_2k.png" "images/place_20k_2k.png" "images/place_20k_20k.png" "images/place_20k_20k_discoloured.png" "images/place_23k_23k.png")

# Run benchmarks for the optimal combinations
echo "Running optimized benchmarks..."

# Test with different thread counts
for IMAGE in "${IMAGES[@]}"; do
    IMG_NAME=$(basename $IMAGE .png)

    echo "Benchmarking $IMG_NAME with varied thread counts..."

    # Run benchmark with focus on thread scaling
    java $JVM_OPTS -jar target/benchmarks.jar EnhancedJMHBenchmark \
        -p imageFile=$IMAGE \
        -p cores=1,4,16,32,64,128 \
        -p threshold=100 \
        -rf json -rff "${OUTPUT_DIR}/${IMG_NAME}_thread_scaling.json" \
        -o "${OUTPUT_DIR}/${IMG_NAME}_thread_scaling.txt"

    echo "Complete: $IMG_NAME thread scaling"

    # Run benchmark with focus on threshold scaling for 64 cores
    if [ "$IMG_NAME" = "place_20k_20k" ]; then
        echo "Benchmarking $IMG_NAME with varied thresholds..."
        java $JVM_OPTS -jar target/benchmarks.jar EnhancedJMHBenchmark \
            -p imageFile=$IMAGE \
            -p cores=64 \
            -p threshold=1,10,50,100,500,1000,2147483647 \
            -rf json -rff "${OUTPUT_DIR}/${IMG_NAME}_threshold_scaling.json" \
            -o "${OUTPUT_DIR}/${IMG_NAME}_threshold_scaling.txt"

        echo "Complete: $IMG_NAME threshold scaling"
    fi
done

# Run comprehensive benchmark for the smallest image with all parameters
# This is quick and gives a good overview
echo "Running full parameter sweep on small image..."
java $JVM_OPTS -jar target/benchmarks.jar EnhancedJMHBenchmark \
    -p imageFile=images/place_2k_2k.png \
    -p cores=1,4,16,32,64,128 \
    -p threshold=1,10,50,100,500,1000,2147483647 \
    -rf json -rff "${OUTPUT_DIR}/small_image_full_sweep.json" \
    -o "${OUTPUT_DIR}/small_image_full_sweep.txt"

echo "All benchmarks completed at $(date)"
echo "Results saved to ${OUTPUT_DIR}"