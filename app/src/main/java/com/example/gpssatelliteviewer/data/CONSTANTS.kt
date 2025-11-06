package com.example.gpssatelliteviewer.data

// last N updates (N sec moving window)
const val CHART_UPDATE_WINDOW = 100

// 15 sec
const val LOCATION_TIMEOUT_PERIOD = 15000L

// how many frames should pass before onFrame() for each manager is applied for scene updates
const val frameCountUpdateInterval = 2

// general minimal value for stuff
const val EPS = 1e-3f

// C/N0 buckets
const val NO_CNO = 0.0f
const val POOR_CNO = 8.0f
const val FAIR_CNO = 15.0f
const val GOOD_CNO = 22.0f
const val EXCELLENT_CNO = 30.0f
