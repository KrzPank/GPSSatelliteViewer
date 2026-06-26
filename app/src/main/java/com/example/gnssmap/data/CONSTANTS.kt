package com.example.gnssmap.data

// last N-1 updates (N-1 sec moving window)
// ALWAYS ADD 1 FOR CHART TO DISPLAY FIRST VALUE (N-1) % 10 = 0
const val CHART_UPDATE_WINDOW = 121

// 15 sec
const val LOCATION_TIMEOUT_PERIOD = 15000L

const val minRecreationInterval = 16L  // Minimum 16ms between recreations /~60Hz

// general minimal value for stuff
const val EPS = 1e-3f

// C/N0 buckets
const val NO_CNO = 0.0f
const val POOR_CNO = 10.0f
const val FAIR_CNO = 15.0f
const val GOOD_CNO = 25.0f
const val EXCELLENT_CNO = 35.0f
