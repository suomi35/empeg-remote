package com.chasinglemons.empeg


class Empeg(private var empegName: String, private var empegIP: String) {
    fun setempegIP(empegIP: String) {
        this.empegIP = empegIP
    }

    fun getempegIP(): String {
        return empegIP
    }

    fun setempegName(empegName: String) {
        this.empegName = empegName
    }

    fun getempegName(): String {
        return empegName
    }
}