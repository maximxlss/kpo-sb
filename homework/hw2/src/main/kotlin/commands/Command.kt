package ru.msk.xls.kpo.bank.commands

// PATTERN: Command - encapsulates operations as objects
interface Command<T> {
    fun execute(): T
}