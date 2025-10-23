package com.example.gpssatelliteviewer.data

// last N updates (N sec moving window)
const val CHART_UPDATE_WINDOW = 100

// 15 sec
const val LOCATION_TIMEOUT_PERIOD = 15000L

// how many frames should pass before onFrame() for each manager is applied for scene updates
const val frameCountUpdateInterval = 1

// general minimal value for stuff
const val EPS = 1e-3f