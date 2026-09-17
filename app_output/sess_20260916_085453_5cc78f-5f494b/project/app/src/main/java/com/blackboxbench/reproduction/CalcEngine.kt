package com.blackboxbench.reproduction

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object Num {
    private val DISPLAY_MC = MathContext(16, RoundingMode.HALF_UP)
    private val DIV_MC = MathContext(16, RoundingMode.HALF_UP)

    fun parse(s: String): BigDecimal? {
        val t = s.trim()
        if (t.isEmpty() || t == "." || t == "-" || t == "-.") return null
        return try {
            BigDecimal(t)
        } catch (e: Exception) {
            null
        }
    }

    fun fmt(v: BigDecimal): String {
        var b = v.round(DISPLAY_MC).stripTrailingZeros()
        if (b.scale() < 0) b = b.setScale(0)
        return b.toPlainString()
    }

    fun fmtDouble(d: Double): String {
        if (d.isNaN() || d.isInfinite()) return "错误"
        return fmt(BigDecimal(d))
    }

    fun pow(a: BigDecimal, b: BigDecimal): BigDecimal {
        val n = b.toDouble()
        if (n == Math.floor(n) && Math.abs(n) <= 9999.0) {
            return a.pow(n.toInt()).round(DISPLAY_MC)
        }
        return BigDecimal(Math.pow(a.toDouble(), n)).round(DISPLAY_MC)
    }

    fun sqrt(a: BigDecimal): BigDecimal =
        BigDecimal(Math.sqrt(a.toDouble())).round(DISPLAY_MC)

    fun divide(a: BigDecimal, b: BigDecimal): BigDecimal =
        if (b.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else a.divide(b, DIV_MC)

    fun group(text: String): String {
        val negative = text.startsWith("-")
        val body = if (negative) text.substring(1) else text
        val dot = body.indexOf('.')
        val intPart = if (dot >= 0) body.substring(0, dot) else body
        val fracPart = if (dot >= 0) body.substring(dot) else ""
        val sb = StringBuilder()
        val n = intPart.length
        for (i in 0 until n) {
            if (i > 0 && (n - i) % 3 == 0) sb.append(',')
            sb.append(intPart[i])
        }
        return (if (negative) "-" else "") + sb.toString() + fracPart
    }
}

class CalcEngine(
    private val onHistory: (String, String) -> Unit
) {
    var left: BigDecimal? = null
        private set
    var leftText: String = ""
        private set
    var pendingOp: String? = null
        private set
    var current: String = ""
        private set
    var percent: Boolean = false
        private set
    var sqrtMode: Boolean = false
        private set
    var exprLine: String = ""
        private set

    fun display(): String {
        val cur = when {
            sqrtMode -> "1√" + current
            percent && current.isNotEmpty() -> current + "%"
            else -> current
        }
        return when {
            sqrtMode -> cur
            leftText.isNotEmpty() && pendingOp != null -> leftText + pendingOp + cur
            leftText.isNotEmpty() -> leftText + cur
            cur.isNotEmpty() -> cur
            else -> "0"
        }
    }

    private fun operandValue(): BigDecimal =
        Num.parse(current) ?: BigDecimal.ZERO

    fun inputDigit(d: String) {
        if (sqrtMode) {
            current = if (current == "0") d else current + d
            return
        }
        if (percent) percent = false
        current = when {
            current == "0" -> d
            current == "." -> "." + d
            else -> current + d
        }
    }

    fun inputDot() {
        if (current.contains('.')) return
        current = if (current.isEmpty()) "." else current + "."
    }

    fun inputOp(op: String) {
        if (sqrtMode) {
            val v = operandValue()
            sqrtMode = false
            commitOperand(Num.sqrt(v), "1√" + current)
            reset()
        }
        val hasOperand = current.isNotEmpty()
        if (hasOperand) {
            val v = operandValue()
            if (left != null && pendingOp != null) {
                val res = apply(pendingOp!!, left!!, v, percent)
                left = res
                leftText = Num.fmt(res)
            } else {
                left = v
                leftText = Num.fmt(v)
            }
        }
        pendingOp = op
        current = ""
        percent = false
        exprLine = ""
    }

    fun inputPercent() {
        if (current.isNotEmpty()) percent = true
    }

    fun inputSqrt() {
        sqrtMode = true
        current = ""
        percent = false
    }

    fun backspace() {
        if (percent) {
            percent = false
            return
        }
        if (current.isNotEmpty()) {
            current = current.dropLast(1)
            if (current == "-") current = ""
        }
    }

    fun clearAll() {
        left = null
        leftText = ""
        pendingOp = null
        current = ""
        percent = false
        sqrtMode = false
        exprLine = ""
    }

    fun loadResult(s: String) {
        clearAll()
        current = s
    }

    fun equals() {
        if (sqrtMode) {
            if (current.isEmpty()) return
            val v = operandValue()
            val res = Num.sqrt(v)
            val expr = "1√" + current
            onHistory(expr, Num.fmt(res))
            exprLine = expr
            finish(res)
            return
        }
        if (current.isEmpty()) return
        if (left != null && pendingOp != null) {
            val v = operandValue()
            if (pendingOp == "\u00f7" && v.compareTo(BigDecimal.ZERO) == 0) return
            val res = apply(pendingOp!!, left!!, v, percent)
            val expr = leftText + pendingOp + current + (if (percent) "%" else "")
            onHistory(expr, Num.fmt(res))
            exprLine = expr
            finish(res)
        } else {
            if (percent) return
            finish(operandValue())
        }
    }

    private fun commitOperand(v: BigDecimal, text: String) {
        if (left != null && pendingOp != null) {
            val res = apply(pendingOp!!, left!!, v, false)
            left = res
            leftText = Num.fmt(res)
        } else {
            left = v
            leftText = text
        }
    }

    private fun finish(v: BigDecimal) {
        current = Num.fmt(v)
        left = null
        leftText = ""
        pendingOp = null
        percent = false
        sqrtMode = false
    }

    private fun reset() {
        left = null
        leftText = ""
        pendingOp = null
        current = ""
        percent = false
        sqrtMode = false
    }

    private fun apply(op: String, a: BigDecimal, b: BigDecimal, pct: Boolean): BigDecimal {
        val bb = if (pct) {
            when (op) {
                "+", "-" -> Num.divide(a.multiply(b), BigDecimal(100))
                "\u00d7", "\u00f7" -> Num.divide(b, BigDecimal(100))
                else -> b
            }
        } else b
        return when (op) {
            "+" -> a.add(bb)
            "-" -> a.subtract(bb)
            "\u00d7" -> a.multiply(bb)
            "\u00f7" -> Num.divide(a, bb)
            "^" -> Num.pow(a, bb)
            else -> bb
        }
    }
}
