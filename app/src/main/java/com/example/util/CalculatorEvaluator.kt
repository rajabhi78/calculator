package com.example.util

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*

object CalculatorEvaluator {
    private val MC = MathContext(50, RoundingMode.HALF_UP)

    fun evaluate(expression: String, isDegreeMode: Boolean = true): BigDecimal {
        if (expression.isBlank()) return BigDecimal.ZERO
        val cleaned = preprocess(expression)
        return Parser(cleaned, isDegreeMode).parse()
    }

    private fun preprocess(expr: String): String {
        // Replace visual symbols with standard arithmetic operators
        var result = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "PI")
            .replace("e", "E")
            .replace("√", "sqrt")
        
        // Handle implicit multiplication between a number and some functions, constants, or brackets
        // e.g., 2PI -> 2*PI, 2(3+4) -> 2*(3+4), 2sqrt(9) -> 2*sqrt(9), (2)(3) -> (2)*(3)
        val regexPatterns = listOf(
            "(\\d)(\\()",          // Digit followed by '('
            "(\\))(\\d)",          // ')' followed by digit
            "(\\))(\\()",          // ')' followed by '('
            "(\\d)(PI|E|sqrt|sin|cos|tan|ln|log)", // Digit followed by keyword
            "(PI|E)(\\d)",         // Constant followed by digit
            "(PI|E)(\\()",         // Constant followed by '('
            "(\\))(PI|E|sqrt|sin|cos|tan|ln|log)"  // ')' followed by keyword
        )

        // Run sequential regular expression replacements to format implicit multiplication
        result = result.replace("(\\d)(\\()".toRegex(), "$1*$2")
        result = result.replace("(\\))(\\d)".toRegex(), "$1*$2")
        result = result.replace("(\\))(\\()".toRegex(), "$1*$2")
        result = result.replace("(\\d)(PI|E|sqrt|sin|cos|tan|ln|log)".toRegex(), "$1*$2")
        result = result.replace("(PI|E)(\\d)".toRegex(), "$1*$2")
        result = result.replace("(PI|E)(\\()".toRegex(), "$1*$2")
        result = result.replace("(\\))(PI|E|sqrt|sin|cos|tan|ln|log)".toRegex(), "$1*$2")

        return result
    }

    private class Parser(val str: String, val isDegreeMode: Boolean) {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): BigDecimal {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected symbol: '${ch.toChar()}'")
            return x
        }

        // Grammar levels:
        // expression = term + term | term - term
        // term = factor * factor | factor / factor
        // factor = +factor | -factor | (expression) | number | function factor | factor ^ factor | factor %

        fun parseExpression(): BigDecimal {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x = x.add(parseTerm(), MC) // addition
                else if (eat('-'.code)) x = x.subtract(parseTerm(), MC) // subtraction
                else return x
            }
        }

        fun parseTerm(): BigDecimal {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x = x.multiply(parseFactor(), MC) // multiplication
                else if (eat('/'.code)) {
                    val divisor = parseFactor()
                    if (divisor.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("Division by zero")
                    x = x.divide(divisor, MC) // division
                } else return x
            }
        }

        fun parseFactor(): BigDecimal {
            if (eat('+'.code)) return parseFactor() // unary plus
            if (eat('-'.code)) return parseFactor().negate() // unary minus

            var x: BigDecimal
            val startPos = this.pos
            if (eat('('.code)) { // parenthesis grouping
                x = parseExpression()
                eat(')'.code)
            } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numeric constants
                while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                val text = str.substring(startPos, this.pos)
                x = BigDecimal(text)
            } else if (ch >= 'a'.code && ch <= 'z'.code || ch >= 'A'.code && ch <= 'Z'.code || ch == '√'.code) { // variables & trig functions
                while ((ch >= 'a'.code && ch <= 'z'.code) || (ch >= 'A'.code && ch <= 'Z'.code)) nextChar()
                val func = str.substring(startPos, this.pos)
                if (func == "PI") {
                    x = BigDecimal(Math.PI.toString())
                } else if (func == "E") {
                    x = BigDecimal(Math.E.toString())
                } else {
                    x = parseFactor()
                    x = when (func) {
                        "sin" -> {
                            val rad = if (isDegreeMode) x.toDouble() * Math.PI / 180 else x.toDouble()
                            BigDecimal(sin(rad).toString())
                        }
                        "cos" -> {
                            val rad = if (isDegreeMode) x.toDouble() * Math.PI / 180 else x.toDouble()
                            BigDecimal(cos(rad).toString())
                        }
                        "tan" -> {
                            val rad = if (isDegreeMode) x.toDouble() * Math.PI / 180 else x.toDouble()
                            val check90 = abs(x.toDouble() % 180)
                            if (isDegreeMode && (check90 == 90.0)) {
                                throw ArithmeticException("Undefined (Tangent of 90°)")
                            }
                            BigDecimal(tan(rad).toString())
                        }
                        "ln" -> {
                            if (x.compareTo(BigDecimal.ZERO) <= 0) throw ArithmeticException("Natural log undefined for <= 0")
                            BigDecimal(ln(x.toDouble()).toString())
                        }
                        "log" -> {
                            if (x.compareTo(BigDecimal.ZERO) <= 0) throw ArithmeticException("Log undefined for <= 0")
                            BigDecimal(log10(x.toDouble()).toString())
                        }
                        "sqrt" -> {
                            if (x.compareTo(BigDecimal.ZERO) < 0) throw ArithmeticException("Square root of a negative value undefined")
                            x.sqrt(MC)
                        }
                        else -> throw RuntimeException("Unknown operator/function: '$func'")
                    }
                }
            } else {
                throw RuntimeException("Unexpected character: '${ch.toChar()}'")
            }

            // Exponent parsing (highest precedence)
            if (eat('^'.code)) {
                val exponent = parseFactor()
                x = x.pow(exponent.toInt(), MC) // NOTE: BigDecimal.pow takes int exponent
            }

            // Percent postfix operator
            if (eat('%'.code)) {
                x = x.divide(BigDecimal("100"), MC)
            }

            return x
        }
    }
}
