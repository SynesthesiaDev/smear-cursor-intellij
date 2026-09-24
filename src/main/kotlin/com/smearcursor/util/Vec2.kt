package com.smearcursor.util

class Vector2 : IPooledObject {

    @JvmField
    var x: Double = 0.0

    @JvmField
    var y: Double = 0.0

    fun set(x: Double, y: Double): Vector2 {
        this.x = x; this.y = y; return this
    }

    fun set(other: Vector2): Vector2 {
        x = other.x; y = other.y; return this
    }

    fun setZero(): Vector2 {
        x = 0.0; y = 0.0; return this
    }

    fun add(other: Vector2): Vector2 { x += other.x; y += other.y; return this }
    fun add(dx: Double, dy: Double): Vector2 { x += dx; y += dy; return this }
    fun addScaled(other: Vector2, s: Double): Vector2 { x += other.x * s; y += other.y * s; return this }

    fun sub(other: Vector2): Vector2 { x -= other.x; y -= other.y; return this }
    fun sub(dx: Double, dy: Double): Vector2 { x -= dx; y -= dy; return this }
    fun subScaled(other: Vector2, s: Double): Vector2 { x -= other.x * s; y -= other.y * s; return this }

    fun mul(s: Double): Vector2 { x *= s; y *= s; return this }
    fun mul(sx: Double, sy: Double): Vector2 { x *= sx; y *= sy; return this }
    fun div(s: Double): Vector2 { val inv = 1.0 / s; x *= inv; y *= inv; return this }

    override fun reset() {
        x = 0.0
        y = 0.0
    }

    override var isPooled: Boolean = false
    override var returnAction: (IPooledObject) -> Unit = {}

    override fun toString(): String = "Vector2($x, $y)"   
}