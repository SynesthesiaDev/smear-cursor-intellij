package com.smearcursor.animation

import com.smearcursor.util.IPooledObject
import com.smearcursor.util.Vector2

data class Particle(
    var position: Vector2,
    var velocity: Vector2,
    var lifetime: Double
) : IPooledObject {
    
    override fun reset() {
        position.reset()
        velocity.reset()
        lifetime = 0.0
    }

    override var isPooled: Boolean = false
    override var returnAction: (IPooledObject) -> Unit = {}
}
