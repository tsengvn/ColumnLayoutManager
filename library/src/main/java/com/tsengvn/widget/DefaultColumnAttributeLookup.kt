package com.tsengvn.widget

internal class DefaultColumnAttributeLookup(val columns: Int) : ColumnAttributeLookup {
    override fun getColumnSpan(position: Int): Int = 1

    override fun getColumnStart(position: Int): Int = position.rem(columns)

    override fun isLastBlockInRow(position: Int): Boolean = false
}