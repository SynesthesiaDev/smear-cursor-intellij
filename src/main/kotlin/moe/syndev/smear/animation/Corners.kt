package moe.syndev.smear.animation

import moe.syndev.smear.util.MutableVector2
import java.security.InvalidParameterException

data class Corners(
    val topLeft: MutableVector2,
    val topRight: MutableVector2,
    val bottomRight: MutableVector2,
    val bottomLeft: MutableVector2
) {
    constructor() : this(MutableVector2(), MutableVector2(), MutableVector2(), MutableVector2())

    fun reset() {
        topLeft.reset()
        topRight.reset()
        bottomRight.reset()
        bottomLeft.reset()
    }

    fun forEachCorner(unit: (MutableVector2) -> Unit) {
        unit.invoke(topLeft)
        unit.invoke(topRight)
        unit.invoke(bottomRight)
        unit.invoke(bottomLeft)
    }
    
    operator fun get(index: Int): MutableVector2 {
        return when (index) {
            0 -> topLeft
            1 -> topRight
            2 -> bottomRight
            3 -> bottomLeft
            else -> throw InvalidParameterException("only 4 corners mate.")
        }
    }

    operator fun set(index: Int, vector: MutableVector2) {
        val vec = when (index) {
            0 -> topLeft
            1 -> topRight
            2 -> bottomRight
            3 -> bottomLeft
            else -> throw InvalidParameterException("only 4 corners mate.")
        }
        vec.set(vector)
    }


}