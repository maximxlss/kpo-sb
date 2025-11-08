package ru.msk.xls.domain.animals

abstract class Herbo(params: Params) : Animal(params) {
    interface Params : Animal.Params {
        val kindness: Double
    }

    val kindness: Double = params.kindness

    fun isKind() = kindness > 5
}