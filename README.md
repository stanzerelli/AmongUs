# Parallelism Project: Is there parallelism among us?

## Overview
This project presents the design, implementation, and performance evaluation of parallel algorithms developed to detect "Amongi" characters within large-scale images. The images analyzed are sourced from the Reddit r/place collaborative dataset. The core of this project lies in the application of the Java Fork/Join framework to effectively parallelize this computationally intensive image analysis task.

## Features & Implementation
The implementation strategy is divided into two primary phases using the Divide and Conquer pattern:
* **Phase 1 (X-Axis Parallelization):** Parallelization of the x-axis traversal, exploring both global `ConcurrentHashMap` and local `HashMap` strategies for result aggregation.
* **Phase 2 (Y-Axis Parallelization):** An extension of the local `HashMap` approach to incorporate y-axis parallelism, which is controlled by a configurable sequential threshold.

## Performance Evaluation
A comprehensive performance evaluation was conducted to analyze overhead, speed-up, and scalability. The benchmarks were executed on two distinct systems:
* A local AMD Ryzen 7 5800X system (8 cores, 16 threads).
* The Firefly high-performance computing server (AMD Threadripper 3990X with 64 cores, 128 threads).

The findings demonstrate substantial performance gains through parallel execution, with the Phase 2 implementation achieving a speed-up of ~28x on the 128-thread server under optimal threshold configurations.

## Detailed Documentation
A comprehensive project report (`TS000068-report-fj.pdf`) is available in this repository, which includes:
* Detailed theoretical examination of the work, span, and task creation characteristics.
* In-depth performance analysis with graphical representations of the benchmarking results.
