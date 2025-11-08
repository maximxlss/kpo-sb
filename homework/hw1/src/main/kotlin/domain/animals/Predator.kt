package ru.msk.xls.domain.animals


abstract class Predator(params: Params) : Animal(params) {
    interface Params : Animal.Params {
        val preyList: List<String>
    }

    val preyList = params.preyList
}