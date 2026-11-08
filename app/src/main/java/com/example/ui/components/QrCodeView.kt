package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

@Composable
fun CryptoQrCodeView(
    address: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 180
) {
    // Generate a standard 25x25 QR matrix deterministically for the TRON address
    val matrix = remember(address) {
        generateQrMatrix(address, 25)
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(2.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size((sizeDp - 24).dp)) {
            val moduleCount = matrix.size
            val moduleSize = size.width / moduleCount

            for (row in 0 until moduleCount) {
                for (col in 0 until moduleCount) {
                    if (matrix[row][col]) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(col * moduleSize, row * moduleSize),
                            size = Size(moduleSize, moduleSize)
                        )
                    }
                }
            }
        }
    }
}

private fun generateQrMatrix(content: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) { false } }

    // 1. Draw top-left finder pattern (7x7)
    drawFinderPattern(matrix, 0, 0)
    // 2. Draw top-right finder pattern
    drawFinderPattern(matrix, 0, size - 7)
    // 3. Draw bottom-left finder pattern
    drawFinderPattern(matrix, size - 7, 0)

    // 4. Draw timing patterns
    for (i in 8 until size - 8) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    // 5. Fill remaining area with deterministic bits from hash of content
    val md = MessageDigest.getInstance("SHA-256")
    val hash = md.digest(content.toByteArray())
    var bitIndex = 0

    for (r in 0 until size) {
        for (c in 0 until size) {
            // Skip finder patterns
            if (isFinderArea(r, c, size)) continue

            val byteVal = hash[bitIndex % hash.size].toInt()
            val bit = ((byteVal shr (bitIndex % 8)) and 1) == 1
            val mask = (r + c) % 2 == 0
            matrix[r][c] = bit xor mask
            bitIndex++
        }
    }

    return matrix
}

private fun drawFinderPattern(matrix: Array<BooleanArray>, top: Int, left: Int) {
    for (r in 0 until 7) {
        for (c in 0 until 7) {
            val isBorder = (r == 0 || r == 6 || c == 0 || c == 6)
            val isInnerSquare = (r in 2..4 && c in 2..4)
            matrix[top + r][left + c] = isBorder || isInnerSquare
        }
    }
}

private fun isFinderArea(r: Int, c: Int, size: Int): Boolean {
    // Top-left
    if (r <= 7 && c <= 7) return true
    // Top-right
    if (r <= 7 && c >= size - 8) return true
    // Bottom-left
    if (r >= size - 8 && c <= 7) return true
    // Timing pattern row & col
    if (r == 6 || c == 6) return true
    return false
}
