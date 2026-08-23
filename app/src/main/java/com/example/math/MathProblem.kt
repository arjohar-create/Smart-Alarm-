package com.example.math

import kotlin.random.Random

data class MathProblem(
    val expression: String,
    val solution: Int,
    val id: String = java.util.UUID.randomUUID().toString()
)

object MathProblemGenerator {

    fun generate(difficulty: String): MathProblem {
        return when (difficulty.uppercase()) {
            "HARD" -> generateHard()
            "MEDIUM" -> generateMedium()
            else -> generateEasy()
        }
    }

    private fun generateEasy(): MathProblem {
        // e.g. 15 + 7, 28 - 14, 7 * 6, 36 / 4
        val op = Random.nextInt(4)
        return when (op) {
            0 -> {
                val a = Random.nextInt(10, 50)
                val b = Random.nextInt(5, 30)
                MathProblem("$a + $b", a + b)
            }
            1 -> {
                val a = Random.nextInt(20, 60)
                val b = Random.nextInt(5, a)
                MathProblem("$a - $b", a - b)
            }
            2 -> {
                val a = Random.nextInt(3, 11)
                val b = Random.nextInt(3, 11)
                MathProblem("$a × $b", a * b)
            }
            else -> {
                val b = Random.nextInt(2, 10)
                val solution = Random.nextInt(2, 10)
                val a = b * solution
                MathProblem("$a ÷ $b", solution)
            }
        }
    }

    private fun generateMedium(): MathProblem {
        // e.g. 48 + 75, 120 - 45, 14 * 8, 144 / 12
        val op = Random.nextInt(4)
        return when (op) {
            0 -> {
                val a = Random.nextInt(30, 99)
                val b = Random.nextInt(25, 99)
                MathProblem("$a + $b", a + b)
            }
            1 -> {
                val a = Random.nextInt(70, 200)
                val b = Random.nextInt(20, a - 10)
                MathProblem("$a - $b", a - b)
            }
            2 -> {
                val a = Random.nextInt(7, 16)
                val b = Random.nextInt(6, 16)
                MathProblem("$a × $b", a * b)
            }
            else -> {
                val b = Random.nextInt(4, 15)
                val solution = Random.nextInt(6, 16)
                val a = b * solution
                MathProblem("$a ÷ $b", solution)
            }
        }
    }

    private fun generateHard(): MathProblem {
        val op = Random.nextInt(3)
        return when (op) {
            0 -> {
                val a = Random.nextInt(120, 550)
                val b = Random.nextInt(120, 550)
                MathProblem("$a + $b", a + b)
            }
            1 -> {
                val a = Random.nextInt(300, 900)
                val b = Random.nextInt(110, a - 50)
                MathProblem("$a - $b", a - b)
            }
            else -> {
                val a = Random.nextInt(13, 30)
                val b = Random.nextInt(12, 25)
                MathProblem("$a × $b", a * b)
            }
        }
    }
}
