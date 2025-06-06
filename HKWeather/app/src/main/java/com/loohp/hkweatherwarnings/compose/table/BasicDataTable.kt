/*
 * This file is part of HKWeather.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkweatherwarnings.compose.table

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import com.google.common.collect.ImmutableList
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Layout model that arranges its children into rows and columns.
 */
@Composable
fun BasicDataTable(
    columns: ImmutableList<DataColumn>,
    modifier: Modifier = Modifier,
    separator: @Composable (rowIndex: Int) -> Unit = { },
    headerHeight: Dp = 56.dp,
    rowHeight: Dp = 52.dp,
    horizontalPadding: Dp = 16.dp,
    footer: @Composable () -> Unit = { },
    cellContentProvider: CellContentProvider = DefaultCellContentProvider,
    sortColumnIndex: Int? = null,
    sortAscending: Boolean = true,
    content: DataTableScope.() -> Unit
) {
    val tableScope = DataTableScopeImpl()
    val tableContent: @Composable () -> Unit = with(tableScope) {
        apply(content); @Composable {

        columns.forEachIndexed { columnIndex, columnDefinition ->
            with(TableCellScopeImpl(0, columnIndex)) {
                val cellScope = this
                Row(
                    Modifier.tableCell()
                        .padding(horizontal = horizontalPadding)
                        .height(headerHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sorted = columnIndex == sortColumnIndex
                    cellContentProvider.HeaderCellContent(
                        sorted = sorted,
                        sortAscending = sortAscending,
                        onClick = columnDefinition.onSort?.let {
                            { it(columnIndex, if (sorted) !sortAscending else sortAscending) }
                        }
                    ) {
                        cellScope.(columnDefinition.header)()
                    }
                }
            }
        }

        tableRows.forEachIndexed { rowIndex, rowData ->
            with(TableRowScopeImpl(rowIndex + 1)) {
                rowData.content(this)
                if (cells.size > columns.size) {
                    throw RuntimeException("Row ${this.rowIndex} has too many cells.")
                }
                if (cells.size < columns.size) {
                    throw RuntimeException("Row ${this.rowIndex} doesn't have enough cells.")
                }
                cells.forEachIndexed { columnIndex, cellData ->
                    with(TableCellScopeImpl(rowIndex + 1, columnIndex)) {
                        val cellScope = this
                        Row(
                            Modifier.tableCell()
                                .padding(horizontal = horizontalPadding)
                                .height(rowHeight),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            cellContentProvider.RowCellContent {
                                cellData.content(cellScope)
                            }
                        }
                    }
                }
            }
        }
    }
    }

    val separators = @Composable {
        val rows = tableScope.tableRows.size + 1 // table rows + header
        for (row in 0 until rows) {
            separator(row)
        }
    }

    val rowBackgrounds = @Composable {
        tableScope.tableRows.forEach {
            Box(Modifier
                .fillMaxSize()
                .then(if (it.onClick != null) Modifier.clickable { it.onClick.invoke() } else Modifier)
            )
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    val columnCount = columns.size
    Layout(
        listOf(tableContent, separators, rowBackgrounds, footer),
        modifier
    ) { (contentMeasurables, separatorMeasurables, rowBackgroundMeasurables, footerMeasurable), constraints ->
        val rowMeasurables = contentMeasurables.groupBy { it.rowIndex }
        val rowCount = rowMeasurables.size
        fun measurableAt(row: Int, column: Int) = rowMeasurables[row]?.getOrNull(column)
        val placeables = Array(rowCount) { arrayOfNulls<Placeable>(columnCount) }

        // Compute column widths and collect flex information.
        var totalFlex = 0f
        val columnWidths = IntArray(columnCount)
        var minTableWidth = 0
        var neededColumnWidth = 0
        for (column in 0 until columnCount) {
            val spec = columns[column].width
            val cells = List(rowCount) { row ->
                TableMeasurable(
                    preferredWidth = {
                        placeables[row][column]?.width
                            ?: measurableAt(row, column)?.measure(Constraints(maxWidth = constraints.maxWidth - columnWidths.sum()))
                                ?.also { placeables[row][column] = it }?.width ?: 0
                    },
                    minIntrinsicWidth = {
                        val height = if (row == 0) {
                            headerHeight.roundToPx()
                        } else {
                            rowHeight.roundToPx()
                        }
                        measurableAt(row, column)?.minIntrinsicWidth(height) ?: 0
                    },
                    maxIntrinsicWidth = {
                        val height = if (row == 0) {
                            headerHeight.roundToPx()
                        } else {
                            rowHeight.roundToPx()
                        }
                        measurableAt(row, column)?.maxIntrinsicWidth(height) ?: 0
                    }
                )
            }
            minTableWidth += spec.minIntrinsicWidth(
                cells,
                constraints.maxWidth,
                this,
                constraints.maxHeight
            )
            columnWidths[column] = spec.preferredWidth(cells, constraints.maxWidth, this)
            neededColumnWidth += columnWidths[column]
            totalFlex += spec.flexValue
        }
        val availableSpace =
            if (constraints.maxWidth == Constraints.Infinity) constraints.maxWidth else max(
                constraints.minWidth,
                minTableWidth
            )
        val remainingSpace = availableSpace - neededColumnWidth

        // Grow flexible columns to fill available horizontal space.
        if (totalFlex > 0 && remainingSpace > 0) {
            for (column in 0 until columnCount) {
                val spec = columns[column].width
                if (spec.flexValue > 0) {
                    columnWidths[column] += (remainingSpace * (spec.flexValue / totalFlex)).roundToInt()
                }
            }
        }

        // Measure the remaining children and calculate row heights.
        val rowHeights = Array(rowCount) { 0 }
        for (row in 0 until rowCount) {
            for (column in 0 until columnCount) {
                if (placeables[row][column] == null) {
                    placeables[row][column] = measurableAt(row, column)?.measure(
                        Constraints(minWidth = 0, maxWidth = columnWidths[column])
                    )
                }
                val cellHeight = placeables[row][column]?.height ?: 0
                rowHeights[row] = max(rowHeights[row], cellHeight)
            }
        }

        val columnOffsets = Array(columnCount + 1) { 0 }
        for (column in 0 until columnCount) {
            columnOffsets[column + 1] = columnOffsets[column] + columnWidths[column]
        }

        val footerPlaceable = footerMeasurable.map {
            it.measure(
                Constraints(
                    minWidth = max(
                        constraints.minWidth,
                        columnOffsets[columnCount]
                    )
                )
            )
        }.firstOrNull()

        val tableWidth = listOf(constraints.minWidth, columnOffsets[columnCount], footerPlaceable?.width ?: 0).max()

        val separatorPlaceables = separatorMeasurables.mapIndexed { index, measurable ->
            val separatorPlaceable = measurable.measure(Constraints(minWidth = 0, maxWidth = tableWidth))
            rowHeights[index] += separatorPlaceable.height
            separatorPlaceable
        }

        // Compute row/column offsets.
        val rowOffsets = Array(rowCount + 1) { 0 }
        for (row in 0 until rowCount) {
            rowOffsets[row + 1] = rowOffsets[row] + rowHeights[row]
        }

        val tableHeight = max(constraints.minHeight, rowOffsets[rowCount] + (footerPlaceable?.height ?: 0))

        // TODO(calintat): Do something when these do not satisfy constraints.
        val tableSize = constraints.constrain(IntSize(tableWidth, tableHeight))

        val rowBackgroundPlaceables = rowBackgroundMeasurables.mapIndexed { index, measurable ->
            measurable.measure(
                Constraints(
                    minWidth = tableSize.width,
                    maxWidth = tableSize.width,
                    minHeight = rowHeights[index + 1],
                    maxHeight = rowHeights[index + 1]
                )
            )
        }
        layout(tableSize.width, tableSize.height) {
            // Place backgrounds
            rowBackgroundPlaceables.forEachIndexed { index, placeable ->
                val rowIndex = index + 1 // header doesn't have a background
                placeable.place(x = 0, y = rowOffsets[rowIndex])
            }

            for (row in 0 until rowCount) {
                // Place cells
                for (column in 0 until columnCount) {
                    placeables[row][column]?.let {
                        val position = columns[column].alignment.align(
                            it.width, columnWidths[column], layoutDirection
                        )
                        it.place(
                            x = columnOffsets[column] + position,
                            y = rowOffsets[row]
                        )
                    }
                }

                // Place separators
                separatorPlaceables[row].let {
                    it.place(x = 0, y = rowOffsets[row] + rowHeights[row] - it.height)
                }
            }
            footerPlaceable?.place(x = 0, y = rowOffsets[rowCount])
        }
    }
}
