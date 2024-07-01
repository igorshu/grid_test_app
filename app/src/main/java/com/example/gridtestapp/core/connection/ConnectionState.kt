package com.example.gridtestapp.core.connection

class ConnectionState(val previous: Boolean, val current: Boolean) {
    override fun toString(): String = "previous = $previous, current = $current"
}