package com.tsengvn.widget

interface ColumnAttributeLookup {
    fun getColumnSpan(position : Int) : Int

    fun getColumnStart(position: Int) : Int

    fun isLastBlockInRow(position: Int) : Boolean
}